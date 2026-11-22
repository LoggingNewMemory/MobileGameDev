#!/usr/bin/env python3
"""
DojoPulse AI Movement Training Pipeline (Boxing and Swimming)
Extracts 3D pose landmarks from training videos using MediaPipe,
normalizes biomechanical coordinates, builds temporal action sequences,
trains neural network classifiers, and exports TensorFlow Lite (.tflite)
models and metadata for Android Native integration.
"""

import os
import sys
import json
import shutil
import argparse
import numpy as np
import cv2
import mediapipe as mp
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix

DATASET_ROOT = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(DATASET_ROOT, "models")
CACHE_DIR = os.path.join(DATASET_ROOT, "cache")
APP_ASSETS_DIR = "/home/yamada/CODE/BINUS/MobileGameDev/AppSource/app/src/main/assets"

# 13 Landmark indices corresponding to ML Kit and MediaPipe Pose
LANDMARK_INDICES = [
    0,   # NOSE
    11,  # LEFT_SHOULDER
    12,  # RIGHT_SHOULDER
    13,  # LEFT_ELBOW
    14,  # RIGHT_ELBOW
    15,  # LEFT_WRIST
    16,  # RIGHT_WRIST
    23,  # LEFT_HIP
    24,  # RIGHT_HIP
    25,  # LEFT_KNEE
    26,  # RIGHT_KNEE
    27,  # LEFT_ANKLE
    28   # RIGHT_ANKLE
]

WINDOW_SIZE = 16  # 16 frames window (~0.5s at 30 fps)
FEATURE_DIM = 62  # 39 coords + 8 angles + 15 relative vectors

def calculate_angle_3d(a, b, c):
    """Calculate angle in degrees between vector BA and BC."""
    ba = a - b
    bc = c - b
    norm_ba = np.linalg.norm(ba)
    norm_bc = np.linalg.norm(bc)
    if norm_ba < 1e-6 or norm_bc < 1e-6:
        return 0.0
    cosine = np.dot(ba, bc) / (norm_ba * norm_bc)
    cosine = np.clip(cosine, -1.0, 1.0)
    return float(np.degrees(np.arccos(cosine)))

def extract_frame_features(landmarks_33):
    """
    Computes invariant normalized features from 33 raw (x,y,z) landmarks.
    Output: 62-element 1D numpy array.
    """
    pts = np.zeros((33, 3), dtype=np.float32)
    for idx in range(33):
        pts[idx] = landmarks_33[idx][:3]

    # Midpoint of hips (23, 24) and shoulders (11, 12)
    hip_mid = (pts[23] + pts[24]) / 2.0
    shoulder_mid = (pts[11] + pts[12]) / 2.0
    center = (hip_mid + shoulder_mid) / 2.0

    torso_scale = np.linalg.norm(shoulder_mid - hip_mid)
    shoulder_width = np.linalg.norm(pts[11] - pts[12])
    scale = torso_scale if torso_scale > 0.05 else shoulder_width
    if scale < 0.01:
        scale = 1.0

    # Normalized keypoint positions (13 keypoints * 3 = 39 features)
    norm_keypoints = []
    for idx in LANDMARK_INDICES:
        norm_pt = (pts[idx] - center) / scale
        norm_keypoints.extend(norm_pt.tolist())

    # Biomechanical angles (8 angles, normalized to [0, 1])
    angles = [
        calculate_angle_3d(pts[11], pts[13], pts[15]) / 180.0, # Left elbow
        calculate_angle_3d(pts[12], pts[14], pts[16]) / 180.0, # Right elbow
        calculate_angle_3d(pts[23], pts[11], pts[13]) / 180.0, # Left shoulder
        calculate_angle_3d(pts[24], pts[12], pts[14]) / 180.0, # Right shoulder
        calculate_angle_3d(pts[11], pts[23], pts[25]) / 180.0, # Left hip
        calculate_angle_3d(pts[12], pts[24], pts[26]) / 180.0, # Right hip
        calculate_angle_3d(pts[23], pts[25], pts[27]) / 180.0, # Left knee
        calculate_angle_3d(pts[24], pts[26], pts[28]) / 180.0  # Right knee
    ]

    # Relative vectors (normalized by scale)
    # Wrists relative to shoulders, nose, and each other
    rel_vectors = []
    rel_vectors.extend(((pts[15] - pts[11]) / scale).tolist()) # L wrist - L shoulder
    rel_vectors.extend(((pts[16] - pts[12]) / scale).tolist()) # R wrist - R shoulder
    rel_vectors.extend(((pts[15] - pts[0]) / scale).tolist())  # L wrist - Nose
    rel_vectors.extend(((pts[16] - pts[0]) / scale).tolist())  # R wrist - Nose
    rel_vectors.extend(((pts[15] - pts[16]) / scale).tolist()) # L wrist - R wrist

    features = np.array(norm_keypoints + angles + rel_vectors, dtype=np.float32)
    return features

def extract_video_landmarks(video_path):
    """Extract raw 33 landmarks for every frame of a video, with disk caching."""
    raw_cache_dir = os.path.join(CACHE_DIR, "raw_landmarks")
    os.makedirs(raw_cache_dir, exist_ok=True)
    basename = os.path.splitext(os.path.basename(video_path))[0]
    parent = os.path.basename(os.path.dirname(video_path)).replace(" ", "_")
    cache_path = os.path.join(raw_cache_dir, f"{parent}_{basename}.npy")

    if os.path.exists(cache_path):
        return np.load(cache_path)

    mp_pose = mp.solutions.pose
    pose = mp_pose.Pose(
        static_image_mode=False,
        model_complexity=1,
        smooth_landmarks=True,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
    )

    cap = cv2.VideoCapture(video_path)
    video_frames = []

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        # Downscale 4K / high-res frames for fast and reliable pose tracking
        h, w = frame.shape[:2]
        if max(h, w) > 1280:
            scale = 1280.0 / max(h, w)
            frame = cv2.resize(frame, (int(w * scale), int(h * scale)))

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        res = pose.process(rgb)
        if res.pose_landmarks:
            frame_lm = []
            for lm in res.pose_landmarks.landmark:
                frame_lm.append([lm.x, lm.y, lm.z, lm.visibility])
            video_frames.append(frame_lm)

    cap.release()
    pose.close()
    arr = np.array(video_frames, dtype=np.float32)
    np.save(cache_path, arr)
    return arr

def resolve_video_label(sport_name, category, filename):
    """
    Resolves the true label for a video file.
    Legacy iPhone recordings IMG_3668, IMG_3669, IMG_3673 in 'Left Jab' are physically uppercuts.
    Legacy iPhone recordings IMG_3675, IMG_3677, IMG_3678 in 'Left Uppercut' are physically jabs.
    New recordings (and any other files) follow their folder name.
    """
    if sport_name == "Boxing":
        if filename.startswith(("IMG_3668", "IMG_3669", "IMG_3673")):
            return "Left Uppercut"
        if filename.startswith(("IMG_3675", "IMG_3677", "IMG_3678")):
            return "Left Jab"
    return category

def process_dataset(sport_name, categories, force_refresh=False):
    """
    Loads or extracts cached landmarks for a given sport.
    Returns X (samples, WINDOW_SIZE, FEATURE_DIM) and y (samples,).
    """
    os.makedirs(CACHE_DIR, exist_ok=True)
    cache_file = os.path.join(CACHE_DIR, f"{sport_name.lower()}_landmarks.npz")
    sport_dir = os.path.join(DATASET_ROOT, sport_name)

    if not force_refresh and os.path.exists(cache_file):
        print(f"[OK] Loading cached landmark data from {cache_file}...")
        data = np.load(cache_file, allow_pickle=True)
        return data["X"], data["y"], list(data["labels"])

    print(f"[*] Extracting landmarks from {sport_name} videos (force_refresh={force_refresh})...")

    if sport_name == "Boxing":
        all_labels_list = list(categories) + ["Guard / Idle"]
    else:
        all_labels_list = list(categories)

    label_to_id = {cat: idx for idx, cat in enumerate(all_labels_list)}
    class_windows = {lbl: [] for lbl in all_labels_list}

    for cat in categories:
        cat_dir = os.path.join(sport_dir, cat)
        if not os.path.isdir(cat_dir):
            print(f"    [WARN] Skipping missing category folder: {cat_dir}")
            continue

        video_files = [f for f in sorted(os.listdir(cat_dir)) if f.lower().endswith(('.mov', '.mp4', '.avi'))]
        print(f"  Category: '{cat}' ({len(video_files)} videos)")

        for vf in video_files:
            vpath = os.path.join(cat_dir, vf)
            target_label = resolve_video_label(sport_name, cat, vf)
            print(f"    -> Extracting: {vf} (Mapped to: '{target_label}') ...", end="", flush=True)
            raw_frames = extract_video_landmarks(vpath)
            print(f" {len(raw_frames)} frames captured.")

            if len(raw_frames) < WINDOW_SIZE:
                continue

            frame_features = np.array([extract_frame_features(f) for f in raw_frames])

            # Calculate wrist motion per frame
            lw = raw_frames[:, 15, :3]
            rw = raw_frames[:, 16, :3]
            lw_disp = np.linalg.norm(np.diff(lw, axis=0, prepend=lw[0:1]), axis=1)
            rw_disp = np.linalg.norm(np.diff(rw, axis=0, prepend=rw[0:1]), axis=1)
            max_disp = np.maximum(lw_disp, rw_disp)

            stride = 2 if sport_name == "Boxing" else 3
            num_windows = (len(frame_features) - WINDOW_SIZE) // stride + 1

            if sport_name == "Boxing":
                w_disps = [np.mean(max_disp[i*stride : i*stride + WINDOW_SIZE]) for i in range(num_windows)]
                p25 = np.percentile(w_disps, 25)
                p40 = np.percentile(w_disps, 40)

                for w_idx in range(num_windows):
                    start = w_idx * stride
                    window = frame_features[start : start + WINDOW_SIZE]
                    d = w_disps[w_idx]

                    if d >= p40:
                        class_windows[target_label].append(window)
                    elif d <= p25:
                        class_windows["Guard / Idle"].append(window)
            else:
                for w_idx in range(num_windows):
                    start = w_idx * stride
                    window = frame_features[start : start + WINDOW_SIZE]
                    class_windows[target_label].append(window)

    # For Boxing, also process dedicated recordings from Dataset/Idle
    idle_dir = os.path.join(DATASET_ROOT, "Idle")
    if sport_name == "Boxing" and os.path.isdir(idle_dir):
        idle_files = [f for f in sorted(os.listdir(idle_dir)) if f.lower().endswith(('.mov', '.mp4', '.avi'))]
        print(f"  Category: 'Guard / Idle' (incorporating {len(idle_files)} dedicated Idle videos from Dataset/Idle)...")
        for vf in idle_files:
            vpath = os.path.join(idle_dir, vf)
            raw_frames = extract_video_landmarks(vpath)
            if len(raw_frames) < WINDOW_SIZE:
                continue
            frame_features = np.array([extract_frame_features(f) for f in raw_frames])
            stride = 4  # Stride of 4 for diverse idle samples
            num_windows = (len(frame_features) - WINDOW_SIZE) // stride + 1
            for w_idx in range(num_windows):
                start = w_idx * stride
                window = frame_features[start : start + WINDOW_SIZE]
                class_windows["Guard / Idle"].append(window)

    print("\n  Class distribution before augmentation:")
    for lbl in all_labels_list:
        print(f"    {lbl}: {len(class_windows[lbl])} windows")

    all_windows = []
    all_labels = []

    target_samples_per_strike = 950
    target_samples_guard = max(1500, len(class_windows.get("Guard / Idle", [])))

    for lbl in all_labels_list:
        windows = class_windows[lbl]
        cid = label_to_id[lbl]
        target_count = target_samples_guard if lbl == "Guard / Idle" else target_samples_per_strike

        if len(windows) == 0:
            continue

        for w in windows:
            all_windows.append(w)
            all_labels.append(cid)

        # Augmentation to balance class representation
        while len(windows) < target_count:
            idx = np.random.randint(0, len(windows))
            jitter = np.random.normal(0, 0.015, windows[idx].shape).astype(np.float32)
            aug_w = windows[idx] + jitter
            windows.append(aug_w)
            all_windows.append(aug_w)
            all_labels.append(cid)

    X = np.array(all_windows, dtype=np.float32)
    y = np.array(all_labels, dtype=np.int32)

    print(f"\n[OK] Final balanced dataset: {len(X)} windows across {len(all_labels_list)} classes.")
    for lbl in all_labels_list:
        cid = label_to_id[lbl]
        print(f"    {lbl}: {(y == cid).sum()} samples")

    np.savez_compressed(cache_file, X=X, y=y, labels=all_labels_list)
    print(f"[OK] Saved landmark cache to {cache_file}")

    return X, y, all_labels_list

def build_model(input_shape, num_classes):
    """Constructs a lightweight 1D-CNN temporal action classifier."""
    model = keras.Sequential([
        layers.Input(shape=input_shape, name="pose_sequence_input"),
        layers.Conv1D(32, kernel_size=3, padding="same", activation="relu"),
        layers.BatchNormalization(),
        layers.MaxPooling1D(pool_size=2),

        layers.Conv1D(64, kernel_size=3, padding="same", activation="relu"),
        layers.BatchNormalization(),
        layers.GlobalAveragePooling1D(),

        layers.Dense(64, activation="relu"),
        layers.Dropout(0.3),
        layers.Dense(num_classes, activation="softmax", name="action_probabilities")
    ])

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )
    return model

def train_and_export(sport_name, categories, force_refresh=False):
    print("\n" + "=" * 65)
    print(f"Training Model for: {sport_name.upper()}")
    print("=" * 65)

    X, y, labels = process_dataset(sport_name, categories, force_refresh=force_refresh)
    num_classes = len(labels)

    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print(f"Dataset split: Train={len(X_train)} samples, Validation={len(X_val)} samples.")
    print(f"Classes: {labels}")

    model = build_model((WINDOW_SIZE, FEATURE_DIM), num_classes)
    model.summary()

    callbacks = [
        keras.callbacks.EarlyStopping(monitor="val_loss", patience=12, restore_best_weights=True),
        keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=5, min_lr=1e-5)
    ]

    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=50,
        batch_size=16,
        callbacks=callbacks,
        verbose=1
    )

    val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
    print(f"\n[RESULT] Validation Accuracy: {val_acc * 100:.2f}% (Loss: {val_loss:.4f})")

    y_pred = np.argmax(model.predict(X_val), axis=-1)
    print("\nClassification Report:")
    print(classification_report(y_val, y_pred, labels=list(range(len(labels))), target_names=labels, zero_division=0))

    os.makedirs(MODELS_DIR, exist_ok=True)
    base_name = f"{sport_name.lower()}_classifier"

    # 1. Export Standard TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    tflite_path = os.path.join(MODELS_DIR, f"{base_name}.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"[OK] Exported TFLite Model: {tflite_path} ({len(tflite_model) / 1024:.1f} KB)")

    # 2. Export Quantized TFLite (Ultra-fast mobile inference)
    converter_q = tf.lite.TFLiteConverter.from_keras_model(model)
    converter_q.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_q_model = converter_q.convert()
    tflite_q_path = os.path.join(MODELS_DIR, f"{base_name}_quantized.tflite")
    with open(tflite_q_path, "wb") as f:
        f.write(tflite_q_model)
    print(f"[OK] Exported Quantized TFLite: {tflite_q_path} ({len(tflite_q_model) / 1024:.1f} KB)")

    # 3. Export Labels
    labels_path = os.path.join(MODELS_DIR, f"{base_name}_labels.txt")
    with open(labels_path, "w") as f:
        for lbl in labels:
            f.write(f"{lbl}\n")
    print(f"[OK] Exported Labels: {labels_path}")

    # 4. Export Metadata
    meta = {
        "sport": sport_name,
        "window_size": WINDOW_SIZE,
        "feature_dim": FEATURE_DIM,
        "landmark_indices": LANDMARK_INDICES,
        "labels": labels,
        "validation_accuracy": float(val_acc),
        "validation_loss": float(val_loss)
    }
    meta_path = os.path.join(MODELS_DIR, f"{base_name}_metadata.json")
    with open(meta_path, "w") as f:
        json.dump(meta, f, indent=2)
    print(f"[OK] Exported Metadata: {meta_path}")

    # 5. Copy to Android Assets if exists
    if os.path.exists(os.path.dirname(APP_ASSETS_DIR)):
        os.makedirs(APP_ASSETS_DIR, exist_ok=True)
        for fname in [f"{base_name}.tflite", f"{base_name}_quantized.tflite", f"{base_name}_labels.txt", f"{base_name}_metadata.json"]:
            src = os.path.join(MODELS_DIR, fname)
            dst = os.path.join(APP_ASSETS_DIR, fname)
            shutil.copy2(src, dst)
        print(f"[OK] Copied models directly to Android assets: {APP_ASSETS_DIR}")

    return val_acc

def main():
    parser = argparse.ArgumentParser(description="DojoPulse AI Movement Training")
    parser.add_argument("--sport", choices=["boxing", "swimming", "all"], default="all",
                        help="Sport movement to train (default: all)")
    parser.add_argument("--force", "--refresh-cache", action="store_true", dest="force",
                        help="Force re-extraction and processing of dataset videos")
    args = parser.parse_args()

    boxing_cats = [
        "Left Hook",
        "Left Jab",
        "Left Uppercut",
        "Right Hook",
        "Right UpperCut",
        "Strike"
    ]

    swimming_cats = [
        "Gaya Batang",
        "Gaya Crawl",
        "Gaya Katak",
        "Gaya Kupu-Kupu"
    ]

    if args.sport in ["boxing", "all"]:
        train_and_export("Boxing", boxing_cats, force_refresh=args.force)

    if args.sport in ["swimming", "all"]:
        train_and_export("Renang", swimming_cats, force_refresh=args.force)

    print("\n" + "=" * 65)
    print("All Models Successfully Trained and Ready for Deployment!")
    print(f"Saved artifacts in: {MODELS_DIR}")
    if os.path.exists(APP_ASSETS_DIR):
        print(f"Android assets updated at: {APP_ASSETS_DIR}")
    print("=" * 65)

if __name__ == "__main__":
    main()
