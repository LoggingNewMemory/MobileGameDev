#!/usr/bin/env python3
"""
DojoPulse boxing classifier v2 — trains on skeletons produced by extract_skeletons.py.

What changed vs train.py (and why):
  1. Fixed-rate timeline: every track is resampled to 30 fps from real timestamps, and wrist velocities are
     expressed per 1/30 s. The app does the same, so a 15 fps phone and a 60 fps iPhone clip agree.
  2. Event-centred windows: positives are 16-frame windows that END 1–5 frames after a wrist-speed peak of the
     labelled arm — exactly when the on-device StrikeDetector asks the model (end of the stroke). The old
     "displacement percentile" rule labelled random motion (including retractions) as punches.
  3. Hard negatives: low-motion windows, retraction-only windows and every window from Idle/negative videos
     (record yourself sitting with hands off-screen, walking, adjusting the phone…) are "Guard / Idle".
  4. Augmentation on the RAW skeleton (before features): mirror (L↔R with label swap), in-plane rotation,
     landmark noise matched to ML Kit jitter, time-warp, and frame dropping (simulates 15 fps / blur).
  5. Video-level split: windows of one video never appear in both train and validation. With ~3 videos per
     class a random window split leaks near-duplicate windows and inflates accuracy (the 98.6% reported for
     the current model is not a real generalisation number).
  6. Temporal model: dilated causal TCN (TFLite built-in ops only), keeps the order of time steps.

Output is a drop-in replacement for the app: same 16×68 input, same label names, files:
  boxing_classifier.tflite, boxing_classifier_quantized.tflite, boxing_classifier_labels.txt,
  boxing_classifier_metadata.json

Usage:
  python extract_skeletons.py --videos /path/to/drive_dataset --out skeletons
  python train_v2.py --skeletons skeletons --out models_v2
  python train_v2.py --skeletons skeletons --out models_v2 --cv          # leave-one-video-out per class
  python train_v2.py ... --install-to ../AppSource/app/src/main/assets    # copy artefacts into the app
"""

import argparse
import glob
import json
import os
import shutil
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pose_features import (BOXING_LABELS, FEATURE_DIM, IDLE_LABEL, LABEL_ARM, LANDMARK_INDICES,  # noqa: E402
                           MIRROR_LABEL, REFERENCE_FPS, WINDOW_SIZE, L_SHO, R_SHO, L_WRI, R_WRI,
                           mirror_sequence, resample_uniform, sequence_features, to_isotropic,
                           wrist_speed_sw)

PEAK_SPEED_SW = 3.0        # same order as StrikeDetectorConfig.minPeakSpeed
IDLE_MAX_SPEED_SW = 1.2    # both wrists slower than this for the whole window → guard / idle
END_OFFSETS = (1, 2, 3, 4, 5)


# ----------------------------------------------------------------------------------------------------------
# Data loading
# ----------------------------------------------------------------------------------------------------------

def load_tracks(skeleton_root, min_arm_vis=0.35):
    tracks = []
    for path in sorted(glob.glob(os.path.join(skeleton_root, "**", "*.npz"), recursive=True)):
        if os.path.basename(os.path.dirname(path)) == "_models":
            continue
        d = np.load(path)  # written by extract_skeletons.py; only numeric/unicode arrays, no pickle
        label = str(d["label"])
        if label not in BOXING_LABELS:
            print(f"[WARN] {path}: unknown label '{label}', skipped")
            continue
        lm = d["landmarks"]
        if label in LABEL_ARM:
            arm_vis = float(np.nanmean(lm[:, LABEL_ARM[label], 3]))
            if arm_vis < min_arm_vis:
                print(f"[SKIP] {d['source']} ({label}): punching wrist visibility {arm_vis:.2f} < {min_arm_vis}")
                continue
        w = int(d["width"]) if "width" in d else 0
        h = int(d["height"]) if "height" in d else 0
        segs = resample_uniform(d["t_ms"], lm, REFERENCE_FPS)
        segs = [to_isotropic(s, w, h) for s in segs if len(s) >= WINDOW_SIZE + 6]
        if segs:
            tracks.append(dict(path=path, video=str(d["source"]), label=label, segments=segs))
    return tracks


# ----------------------------------------------------------------------------------------------------------
# Augmentation on raw (T, 33, 4) sequences
# ----------------------------------------------------------------------------------------------------------

def augment(seq, rng):
    s = seq.copy()
    # 3D yaw around torso vertical Y-axis (±45°) to cover oblique stances (MediaPipe z ~2.2x x scale)
    yaw = np.deg2rad(rng.uniform(-45, 45))
    cy, sy = np.cos(yaw), np.sin(yaw)
    cxz = np.array([((s[:, 11, 0] + s[:, 12, 0]) / 2).mean(), ((s[:, 11, 2] + s[:, 12, 2]) / 2).mean()], dtype=np.float32)
    dx = s[..., 0] - cxz[0]
    dz = (s[..., 2] - cxz[1]) * 0.45
    s[..., 0] = cxz[0] + cy * dx - sy * dz
    s[..., 2] = cxz[1] + (sy * dx + cy * dz) * (1.0 / 0.45)
    # in-plane rotation (camera roll) about the shoulder midpoint, ±10°
    ang = np.deg2rad(rng.uniform(-10, 10))
    c, si = np.cos(ang), np.sin(ang)
    centre = ((s[:, 11, :2] + s[:, 12, :2]) / 2).mean(axis=0)
    xy = s[..., :2] - centre
    s[..., 0] = centre[0] + c * xy[..., 0] - si * xy[..., 1]
    s[..., 1] = centre[1] + si * xy[..., 0] + c * xy[..., 1]
    # global scale (distance to camera)
    k = rng.uniform(0.8, 1.2)
    s[..., :3] = (s[..., :3] - np.r_[centre, 0]) * k + np.r_[centre, 0]
    # landmark jitter: ML Kit is noisier than MediaPipe-heavy; z noisier than x/y
    s[..., 0:2] += rng.normal(0, 0.008, s[..., 0:2].shape)
    s[..., 2] += rng.normal(0, 0.018, s[..., 2].shape)
    # time warp (punch speed variation) 0.8×–1.25×
    warp = rng.uniform(0.8, 1.25)
    t_src = np.arange(len(s))
    t_new = np.arange(0, len(s) - 1, warp)
    flat = s.reshape(len(s), -1)
    s = np.stack([np.interp(t_new, t_src, flat[:, j]) for j in range(flat.shape[1])], 1).reshape(len(t_new), 33, -1)
    # frame dropping + hold (15 fps camera / blur dropouts)
    if rng.random() < 0.5:
        drop = rng.random(len(s)) < 0.25
        drop[0] = False
        for i in np.where(drop)[0]:
            s[i] = s[i - 1]
    return s.astype(np.float32)


def is_outbound_punch(s, pk, lab):
    """True if the peak corresponds to an outbound strike rather than a retraction or wind-up dip."""
    w_idx = LABEL_ARM[lab]
    s_idx = L_SHO if w_idx == L_WRI else R_SHO
    o_idx = R_SHO if w_idx == L_WRI else L_SHO
    v = wrist_speed_sw(s, w_idx)
    a = pk
    while a > 0 and v[a - 1] < v[a] and pk - a < 10:
        a -= 1
    b = pk
    while b < len(v) - 1 and v[b + 1] < v[b] and b - pk < 8:
        b += 1
    if b - a < 2:
        return False
    sw = float(np.linalg.norm(s[a, L_SHO, :2] - s[a, R_SHO, :2])) + 1e-6
    disp = float(np.linalg.norm(s[b, w_idx, :3] - s[a, w_idx, :3])) / sw
    if disp < 0.20:
        return False
    rise = float(s[a, w_idx, 1] - s[b, w_idx, 1]) / sw
    reach_a = float(np.linalg.norm(s[a, w_idx, :3] - s[a, s_idx, :3])) / sw
    reach_b = float(np.linalg.norm(s[b, w_idx, :3] - s[b, s_idx, :3])) / sw
    dz = float(s[a, w_idx, 2] - s[b, w_idx, 2]) / sw
    dx_sign = 1.0 if s[a, o_idx, 0] >= s[a, s_idx, 0] else -1.0
    across = float(s[b, w_idx, 0] - s[a, w_idx, 0]) * dx_sign / sw
    if "Upper" in lab:
        return rise > 0.20
    if "Hook" in lab:
        return across > 0.15 or (rise > -0.25 and reach_b >= reach_a - 0.1)
    return (reach_b - reach_a > 0.10) or (dz > 0.25) or (abs(s[b, w_idx, 0] - s[a, w_idx, 0]) / sw > 0.35 and rise > -0.35)


# ----------------------------------------------------------------------------------------------------------
# Windowing
# ----------------------------------------------------------------------------------------------------------

def windows_from_segment(seg, label, rng, n_aug, use_mirror):
    """Returns list of (window_features (16,68), label)."""
    out = []
    variants = [(seg, label)]
    if use_mirror:
        variants.append((mirror_sequence(seg), MIRROR_LABEL[label]))
    for base, lab in variants:
        for a in range(n_aug + 1):
            s = base if a == 0 else augment(base, rng)
            if len(s) < WINDOW_SIZE + 6:
                continue
            feats = sequence_features(s, REFERENCE_FPS)
            vl, vr = wrist_speed_sw(s, L_WRI), wrist_speed_sw(s, R_WRI)
            vmax = np.maximum(vl, vr)

            if lab == IDLE_LABEL:
                for end in range(WINDOW_SIZE, len(s) + 1, 4):
                    out.append((feats[end - WINDOW_SIZE:end], lab))
                continue

            v_arm = vl if LABEL_ARM[lab] == L_WRI else vr
            peaks = find_peaks(v_arm, PEAK_SPEED_SW, min_distance=8)
            for p in peaks:
                if p < 6:
                    continue
                if is_outbound_punch(s, p, lab):
                    for off in END_OFFSETS:
                        end = p + off
                        if WINDOW_SIZE <= end <= len(s):
                            out.append((feats[end - WINDOW_SIZE:end], lab))
                else:
                    # Retractions and wind-up dips become hard negatives
                    end = p + 3
                    if WINDOW_SIZE <= end <= len(s):
                        out.append((feats[end - WINDOW_SIZE:end], IDLE_LABEL))
            # negatives from the same clip: calm stretches (guard between reps)
            for end in range(WINDOW_SIZE, len(s) + 1, 6):
                if vmax[end - WINDOW_SIZE:end].max() < IDLE_MAX_SPEED_SW:
                    out.append((feats[end - WINDOW_SIZE:end], IDLE_LABEL))
    return out


def find_peaks(v, height, min_distance):
    try:
        from scipy.signal import find_peaks as sp_find
        idx, _ = sp_find(v, height=height, distance=min_distance, prominence=height * 0.5)
        return list(idx)
    except ImportError:
        idx = [i for i in range(1, len(v) - 1) if v[i] >= height and v[i] >= v[i - 1] and v[i] > v[i + 1]]
        kept = []
        for i in sorted(idx, key=lambda j: -v[j]):
            if all(abs(i - k) >= min_distance for k in kept):
                kept.append(i)
        return sorted(kept)


def build_split(tracks, val_videos, rng, n_aug):
    Xtr, ytr, Xva, yva = [], [], [], []
    lab2id = {l: i for i, l in enumerate(BOXING_LABELS)}
    for tr in tracks:
        is_val = tr["video"] in val_videos
        for seg in tr["segments"]:
            ws = windows_from_segment(seg, tr["label"], rng, 0 if is_val else n_aug, use_mirror=not is_val)
            for w, lab in ws:
                (Xva if is_val else Xtr).append(w)
                (yva if is_val else ytr).append(lab2id[lab])
    as_np = lambda X, y: (np.asarray(X, np.float32).reshape(-1, WINDOW_SIZE, FEATURE_DIM), np.asarray(y, np.int32))
    return (*as_np(Xtr, ytr), *as_np(Xva, yva))


def pick_val_videos(tracks, rng, fold=None):
    """One held-out video per class (rotating with `fold` for cross-validation)."""
    by_label = {}
    for t in tracks:
        by_label.setdefault(t["label"], []).append(t["video"])
    val = set()
    for lab, vids in by_label.items():
        vids = sorted(set(vids))
        if len(vids) < 2:
            print(f"[WARN] '{lab}' has only {len(vids)} video(s); it cannot be validated on unseen footage")
            continue
        val.add(vids[fold % len(vids)] if fold is not None else vids[rng.integers(len(vids))])
    return val


# ----------------------------------------------------------------------------------------------------------
# Model
# ----------------------------------------------------------------------------------------------------------

def build_model(num_classes, arch="tcn"):
    import tensorflow as tf
    from tensorflow.keras import layers, regularizers

    inp = layers.Input(shape=(WINDOW_SIZE, FEATURE_DIM), name="pose_sequence_input")
    if arch == "lstm":
        x = layers.GaussianNoise(0.03)(inp)
        x = layers.LSTM(64, return_sequences=True, unroll=True)(x)
        x = layers.LSTM(48, unroll=True)(x)
    else:
        x = layers.GaussianNoise(0.03)(inp)
        x = layers.Conv1D(48, 3, padding="causal", activation="relu",
                          kernel_regularizer=regularizers.l2(1e-3))(x)
        x = layers.SpatialDropout1D(0.2)(x)
        for d in (2, 4):  # receptive field covers the 16-frame window without BatchNorm overfitting
            y = layers.Conv1D(48, 3, padding="causal", dilation_rate=d, activation="relu",
                              kernel_regularizer=regularizers.l2(1e-3))(x)
            y = layers.SpatialDropout1D(0.2)(y)
            x = layers.Add()([x, y])
        last = layers.Lambda(lambda t: t[:, -1, :], name="last_step")(x)
        avg_p = layers.GlobalAveragePooling1D()(x)
        max_p = layers.GlobalMaxPooling1D()(x)
        x = layers.Concatenate()([last, avg_p, max_p])
    x = layers.Dense(48, activation="relu", kernel_regularizer=regularizers.l2(1e-3))(x)
    x = layers.Dropout(0.4)(x)
    out = layers.Dense(num_classes, activation="softmax", name="action_probabilities")(x)
    model = tf.keras.Model(inp, out)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(8e-4),
        loss=tf.keras.losses.CategoricalCrossentropy(label_smoothing=0.05),
        metrics=["accuracy"],
    )
    return model


def train_once(Xtr, ytr, Xva, yva, arch, epochs, seed):
    import tensorflow as tf
    tf.keras.utils.set_random_seed(seed)
    n = len(BOXING_LABELS)
    Ytr = tf.keras.utils.to_categorical(ytr, n)
    counts = np.bincount(ytr, minlength=n).astype(np.float32)
    class_weight = {i: float(len(ytr) / (n * c)) if c > 0 else 0.0 for i, c in enumerate(counts)}
    model = build_model(n, arch)
    if len(Xva):
        Yva = tf.keras.utils.to_categorical(yva, n)
        cb = [tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=12, restore_best_weights=True),
              tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=4, min_lr=1e-5)]
        hist = model.fit(Xtr, Ytr, validation_data=(Xva, Yva), epochs=epochs, batch_size=64,
                         class_weight=class_weight, callbacks=cb, verbose=2)
        best_ep = int(np.argmin(hist.history["val_loss"])) + 1
    else:
        lr_cb = tf.keras.callbacks.LearningRateScheduler(
            lambda ep, lr: float(8e-4 * 0.5 * (1.0 + np.cos(np.pi * ep / max(epochs, 1))))
        )
        model.fit(Xtr, Ytr, epochs=epochs, batch_size=64, class_weight=class_weight,
                  callbacks=[lr_cb], verbose=2)
        best_ep = epochs
    return model, best_ep


def evaluate(model, Xva, yva):
    from sklearn.metrics import classification_report, confusion_matrix
    if len(Xva) == 0:
        return {}
    pred = np.argmax(model.predict(Xva, verbose=0), axis=-1)
    present = sorted(set(yva.tolist()) | set(pred.tolist()))
    names = [BOXING_LABELS[i] for i in present]
    print(classification_report(yva, pred, labels=present, target_names=names, zero_division=0))
    cm = confusion_matrix(yva, pred, labels=list(range(len(BOXING_LABELS))))
    return dict(accuracy=float((pred == yva).mean()),
                report=classification_report(yva, pred, labels=present, target_names=names, zero_division=0, output_dict=True),
                confusion_matrix=cm.tolist())


def export(model, out_dir, metrics, extra_meta):
    import tensorflow as tf
    os.makedirs(out_dir, exist_ok=True)
    base = os.path.join(out_dir, "boxing_classifier")
    conv = tf.lite.TFLiteConverter.from_keras_model(model)
    conv.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]   # no Flex ops on Android
    open(base + ".tflite", "wb").write(conv.convert())
    conv = tf.lite.TFLiteConverter.from_keras_model(model)
    conv.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    conv.optimizations = [tf.lite.Optimize.DEFAULT]
    open(base + "_quantized.tflite", "wb").write(conv.convert())
    with open(base + "_labels.txt", "w") as f:
        f.write("\n".join(BOXING_LABELS) + "\n")
    meta = dict(sport="Boxing", window_size=WINDOW_SIZE, feature_dim=FEATURE_DIM, landmark_indices=LANDMARK_INDICES,
                labels=BOXING_LABELS, feature_version=2, velocity_reference_fps=REFERENCE_FPS,
                validation_accuracy=metrics.get("accuracy"), validation_split="held-out videos (one per class)",
                **extra_meta)
    with open(base + "_metadata.json", "w") as f:
        json.dump(meta, f, indent=2)

    # sanity: the exported model runs with the interpreter and matches Keras
    interp = tf.lite.Interpreter(model_path=base + ".tflite")
    interp.allocate_tensors()
    x = np.random.default_rng(0).normal(size=(1, WINDOW_SIZE, FEATURE_DIM)).astype(np.float32)
    interp.set_tensor(interp.get_input_details()[0]["index"], x)
    interp.invoke()
    diff = np.abs(interp.get_tensor(interp.get_output_details()[0]["index"]) - model.predict(x, verbose=0)).max()
    print(f"[OK] Exported to {out_dir} (TFLite vs Keras max |Δ| = {diff:.2e})")
    return [base + s for s in (".tflite", "_quantized.tflite", "_labels.txt", "_metadata.json")]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--skeletons", required=True)
    ap.add_argument("--out", default="models_v2")
    ap.add_argument("--arch", choices=["tcn", "lstm"], default="tcn")
    ap.add_argument("--aug", type=int, default=6, help="augmented copies per training clip (plus mirror)")
    ap.add_argument("--epochs", type=int, default=60)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--cv", action="store_true", help="leave-one-video-out per class cross-validation report")
    ap.add_argument("--final-all", action="store_true", help="train final exported model on all visible tracks using CV-calibrated epochs")
    ap.add_argument("--install-to", default=None, help="copy artefacts into this Android assets folder")
    args = ap.parse_args()

    rng = np.random.default_rng(args.seed)
    tracks = load_tracks(args.skeletons)
    if not tracks:
        sys.exit(f"No skeleton files under {args.skeletons}. Run extract_skeletons.py first.")
    print(f"[*] {len(tracks)} videos: " + ", ".join(f"{l}={sum(t['label'] == l for t in tracks)}" for l in BOXING_LABELS))

    cv_accs, cv_epochs = [], []
    if args.cv:
        n_folds = max(sum(t["label"] == l for t in tracks) for l in BOXING_LABELS if l != IDLE_LABEL)
        for fold in range(n_folds):
            val = pick_val_videos(tracks, rng, fold)
            Xtr, ytr, Xva, yva = build_split(tracks, val, rng, args.aug)
            print(f"\n=== fold {fold}: train {len(Xtr)} / val {len(Xva)} windows ===")
            m, best_ep = train_once(Xtr, ytr, Xva, yva, args.arch, args.epochs, args.seed + fold)
            cv_accs.append(evaluate(m, Xva, yva).get("accuracy", float("nan")))
            cv_epochs.append(best_ep)
        print(f"\n[CV] held-out-video accuracy per fold: {np.round(cv_accs, 3).tolist()}  mean={np.nanmean(cv_accs):.3f}  best_epochs={cv_epochs}")

    if args.final_all:
        val = set()
        final_epochs = int(np.clip(np.median(cv_epochs) if cv_epochs else 16, 8, 22))
        Xtr, ytr, Xva, yva = build_split(tracks, val, rng, args.aug)
        print(f"[*] Final full-dataset training on all {len(tracks)} videos ({len(Xtr)} windows, {final_epochs} epochs)")
        print("    train per class: " + ", ".join(f"{l}={int((ytr == i).sum())}" for i, l in enumerate(BOXING_LABELS)))
        model, _ = train_once(Xtr, ytr, Xva, yva, args.arch, final_epochs, args.seed)
        metrics = {"accuracy": float(np.nanmean(cv_accs)) if cv_accs else None}
        extra = dict(
            arch=args.arch,
            feature_space="isotropic_width_normalized",
            yaw_aug_deg=45,
            final_train_videos=len(tracks),
            final_epochs=final_epochs,
            cv_fold_accuracies=cv_accs,
            cv_mean_accuracy=float(np.nanmean(cv_accs)) if cv_accs else None,
        )
    else:
        val = pick_val_videos(tracks, rng, fold=0)
        Xtr, ytr, Xva, yva = build_split(tracks, val, rng, args.aug)
        print(f"[*] Train windows {len(Xtr)}, validation windows {len(Xva)} (held-out videos: {sorted(val)})")
        print("    train per class: " + ", ".join(f"{l}={int((ytr == i).sum())}" for i, l in enumerate(BOXING_LABELS)))
        model, _ = train_once(Xtr, ytr, Xva, yva, args.arch, args.epochs, args.seed)
        metrics = evaluate(model, Xva, yva)
        extra = dict(arch=args.arch, held_out_videos=sorted(val), confusion_matrix=metrics.get("confusion_matrix"),
                     cv_fold_accuracies=cv_accs or None)
    files = export(model, args.out, metrics, extra)
    if args.install_to:
        os.makedirs(args.install_to, exist_ok=True)
        for f in files:
            shutil.copy2(f, os.path.join(args.install_to, os.path.basename(f)))
        print(f"[OK] Installed into {args.install_to}")


if __name__ == "__main__":
    main()
