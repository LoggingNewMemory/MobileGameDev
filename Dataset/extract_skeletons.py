#!/usr/bin/env python3
"""
Extract BlazePose (33-landmark) skeletons from the DojoPulse boxing videos with MediaPipe Pose Landmarker.

Why MediaPipe and not YOLOv8-Pose: the app runs ML Kit Pose Detection, which is BlazePose. MediaPipe Pose
Landmarker is the same model family and topology (33 points, x/y/z, visibility), so features and thresholds
learned offline transfer to the phone. YOLOv8-Pose outputs the 17-point COCO skeleton (no z, no nose-to-ankle
BlazePose semantics, different confidence calibration), which would create a train/inference mismatch.

Input layout (same as the Google Drive dataset):
    <videos_root>/Left Hook/*.MOV
    <videos_root>/Left Jab/*.MOV
    <videos_root>/Left Uppercut/*.MOV
    <videos_root>/Right Hook/*.MOV
    <videos_root>/Right UpperCut/*.MOV
    <videos_root>/Strike/*.MOV
    <videos_root>/Idle/*.MOV            (optional: guard, bouncing, walking, SITTING with hands off-screen …)

Output (one file per video, timeline preserved; frames without a person are NaN):
    <out_root>/<Label>/<video>.npz  with
        t_ms        (N,)        presentation timestamp of every decoded frame
        landmarks   (N, 33, 4)  normalised x, y, z, visibility
        presence    (N, 33)     landmark presence
        world       (N, 33, 3)  metric world landmarks (hip-centred, metres)
        width, height, fps, label, source
    <out_root>/index.csv

Usage:
    python extract_skeletons.py --videos /path/to/dataset --out skeletons
    python extract_skeletons.py --videos /path/to/dataset --out skeletons --model heavy --preview
"""

import argparse
import csv
import os
import sys
import urllib.request

import cv2
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pose_features import resolve_video_label  # noqa: E402

MODEL_URLS = {
    "lite": "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task",
    "full": "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task",
    "heavy": "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task",
}
VIDEO_EXT = (".mov", ".mp4", ".avi", ".mkv", ".m4v")
CONNECTIONS = [(11, 12), (11, 13), (13, 15), (12, 14), (14, 16), (11, 23), (12, 24), (23, 24),
               (23, 25), (25, 27), (24, 26), (26, 28), (0, 11), (0, 12)]


def ensure_model(kind, cache_dir):
    os.makedirs(cache_dir, exist_ok=True)
    path = os.path.join(cache_dir, f"pose_landmarker_{kind}.task")
    if not os.path.exists(path):
        print(f"[*] Downloading MediaPipe {kind} model …")
        urllib.request.urlretrieve(MODEL_URLS[kind], path)
    return path


def make_landmarker(model_path, min_conf):
    import mediapipe as mp
    from mediapipe.tasks import python as mp_python
    from mediapipe.tasks.python import vision

    options = vision.PoseLandmarkerOptions(
        base_options=mp_python.BaseOptions(model_asset_path=model_path),
        running_mode=vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=min_conf,
        min_pose_presence_confidence=min_conf,
        min_tracking_confidence=min_conf,
        output_segmentation_masks=False,
    )
    return mp, vision.PoseLandmarker.create_from_options(options)


def extract_video(path, model_path, max_side, min_conf, preview_path=None):
    mp, landmarker = make_landmarker(model_path, min_conf)  # new instance per video: VIDEO mode needs monotonic ts
    cap = cv2.VideoCapture(path)
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    writer = None
    t_list, lm_list, pr_list, wl_list = [], [], [], []
    frame_idx, last_ts = 0, -1
    w = h = 0
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        pos_ms = cap.get(cv2.CAP_PROP_POS_MSEC)
        ts = int(round(pos_ms)) if pos_ms and pos_ms > 0 else int(round(frame_idx * 1000.0 / fps))
        if ts <= last_ts:
            ts = last_ts + 1
        last_ts = ts
        frame_idx += 1

        hh, ww = frame.shape[:2]
        if max(hh, ww) > max_side:
            s = max_side / max(hh, ww)
            frame = cv2.resize(frame, (int(ww * s), int(hh * s)), interpolation=cv2.INTER_AREA)
        h, w = frame.shape[:2]
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        res = landmarker.detect_for_video(mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb), ts)

        lm = np.full((33, 4), np.nan, np.float32)
        pr = np.zeros(33, np.float32)
        wl = np.full((33, 3), np.nan, np.float32)
        if res.pose_landmarks:
            for i, p in enumerate(res.pose_landmarks[0]):
                lm[i] = (p.x, p.y, p.z, p.visibility if p.visibility is not None else 1.0)
                pr[i] = p.presence if p.presence is not None else 1.0
            if res.pose_world_landmarks:
                for i, p in enumerate(res.pose_world_landmarks[0]):
                    wl[i] = (p.x, p.y, p.z)
        t_list.append(ts); lm_list.append(lm); pr_list.append(pr); wl_list.append(wl)

        if preview_path is not None:
            if writer is None:
                writer = cv2.VideoWriter(preview_path, cv2.VideoWriter_fourcc(*"mp4v"), fps, (w, h))
            vis = frame.copy()
            if not np.isnan(lm[0, 0]):
                for a, b in CONNECTIONS:
                    if lm[a, 3] > 0.5 and lm[b, 3] > 0.5:
                        cv2.line(vis, (int(lm[a, 0] * w), int(lm[a, 1] * h)), (int(lm[b, 0] * w), int(lm[b, 1] * h)), (0, 255, 0), 2)
                for i in (15, 16):
                    col = (0, 255, 255) if lm[i, 3] >= 0.65 else (0, 0, 255)  # red = would NOT be trusted on device
                    cv2.circle(vis, (int(lm[i, 0] * w), int(lm[i, 1] * h)), 6, col, -1)
            writer.write(vis)

    cap.release()
    landmarker.close()
    if writer is not None:
        writer.release()
    return dict(
        t_ms=np.asarray(t_list, np.int64),
        landmarks=np.asarray(lm_list, np.float32).reshape(-1, 33, 4),
        presence=np.asarray(pr_list, np.float32).reshape(-1, 33),
        world=np.asarray(wl_list, np.float32).reshape(-1, 33, 3),
        width=w, height=h, fps=float(fps),
    )


def main():
    ap = argparse.ArgumentParser(description="Extract BlazePose skeletons with MediaPipe Pose Landmarker")
    ap.add_argument("--videos", required=True, help="dataset root with one folder per label")
    ap.add_argument("--out", default="skeletons")
    ap.add_argument("--model", choices=list(MODEL_URLS), default="full",
                    help="'full' is closest to ML Kit's on-device model; 'heavy' is cleaner but slower")
    ap.add_argument("--max-side", type=int, default=1280, help="downscale long side (4K iPhone clips)")
    ap.add_argument("--min-conf", type=float, default=0.5)
    ap.add_argument("--preview", action="store_true", help="also write overlay MP4s for visual QA")
    ap.add_argument("--force", action="store_true", help="re-extract even if the .npz exists")
    args = ap.parse_args()

    model_path = ensure_model(args.model, os.path.join(args.out, "_models"))
    rows = []
    for folder in sorted(os.listdir(args.videos)):
        cat_dir = os.path.join(args.videos, folder)
        if not os.path.isdir(cat_dir) or folder.startswith(("_", ".")):
            continue
        for vf in sorted(f for f in os.listdir(cat_dir) if f.lower().endswith(VIDEO_EXT)):
            label = "Guard / Idle" if folder.lower() in ("idle", "guard", "negatives") else resolve_video_label(folder, vf)
            out_dir = os.path.join(args.out, label.replace("/", "-").replace(" ", "_"))
            os.makedirs(out_dir, exist_ok=True)
            out_path = os.path.join(out_dir, os.path.splitext(vf)[0] + ".npz")
            if os.path.exists(out_path) and not args.force:
                d = np.load(out_path)  # label/source are stored as plain unicode arrays; no pickle needed
            else:
                prev = os.path.splitext(out_path)[0] + "_preview.mp4" if args.preview else None
                print(f"[*] {folder}/{vf} → '{label}' …", end="", flush=True)
                d = extract_video(os.path.join(cat_dir, vf), model_path, args.max_side, args.min_conf, prev)
                np.savez_compressed(out_path, label=np.str_(label), source=np.str_(os.path.join(folder, vf)), **d)
                print(f" {len(d['t_ms'])} frames")
            lm = d["landmarks"]
            detected = float(np.mean(~np.isnan(lm[:, 0, 0]))) if len(lm) else 0.0
            wrist_ok = float(np.nanmean((lm[:, 15, 3] >= 0.65) | (lm[:, 16, 3] >= 0.65))) if len(lm) else 0.0
            rows.append([out_path, label, os.path.join(folder, vf), len(lm), f"{float(d['fps']):.2f}",
                         f"{detected:.3f}", f"{wrist_ok:.3f}"])

    with open(os.path.join(args.out, "index.csv"), "w", newline="") as f:
        wr = csv.writer(f)
        wr.writerow(["npz", "label", "source", "frames", "fps", "person_detected_ratio", "wrist_visible_ratio"])
        wr.writerows(rows)
    print(f"[OK] {len(rows)} videos → {args.out}/index.csv")
    low = [r for r in rows if float(r[5]) < 0.8]
    if low:
        print("[WARN] Low detection rate (check framing / lighting):")
        for r in low:
            print("   ", r[2], r[5])


if __name__ == "__main__":
    main()
