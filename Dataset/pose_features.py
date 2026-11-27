#!/usr/bin/env python3
"""
Shared skeleton utilities for DojoPulse training (no TensorFlow import, safe to reuse).

Everything here mirrors the on-device code so that training and inference see the same numbers:
  * BlazePose 33-landmark topology (MediaPipe Pose Landmarker == ML Kit Pose Detection family)
  * 68-dim per-frame feature vector == AIPoseMovementClassifier.extractFeatures()
  * wrist velocities expressed per 1/30 s (REFERENCE_FPS), as the app now does
"""

import numpy as np

REFERENCE_FPS = 30.0
WINDOW_SIZE = 16
FEATURE_DIM = 68

# 13 landmark indices (BlazePose / ML Kit)
LANDMARK_INDICES = [0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28]

NOSE, L_SHO, R_SHO, L_ELB, R_ELB, L_WRI, R_WRI = 0, 11, 12, 13, 14, 15, 16
L_HIP, R_HIP, L_KNE, R_KNE, L_ANK, R_ANK = 23, 24, 25, 26, 27, 28

# Left/right landmark pairs of the 33-point BlazePose topology (used for mirror augmentation).
MIRROR_PAIRS = [(1, 4), (2, 5), (3, 6), (7, 8), (9, 10), (11, 12), (13, 14), (15, 16), (17, 18),
                (19, 20), (21, 22), (23, 24), (25, 26), (27, 28), (29, 30), (31, 32)]

BOXING_LABELS = ["Left Hook", "Left Jab", "Left Uppercut", "Right Hook", "Right UpperCut", "Strike", "Guard / Idle"]
IDLE_LABEL = "Guard / Idle"

# Mirroring swaps the punching arm. Labels are arm-based in the app (Strike == right straight).
MIRROR_LABEL = {
    "Left Hook": "Right Hook", "Right Hook": "Left Hook",
    "Left Uppercut": "Right UpperCut", "Right UpperCut": "Left Uppercut",
    "Left Jab": "Strike", "Strike": "Left Jab",
    IDLE_LABEL: IDLE_LABEL,
}

# Which wrist performs the labelled strike.
LABEL_ARM = {
    "Left Hook": L_WRI, "Left Jab": L_WRI, "Left Uppercut": L_WRI,
    "Right Hook": R_WRI, "Right UpperCut": R_WRI, "Strike": R_WRI,
}


def resolve_video_label(category, filename):
    """Folder names match the actual movement recorded in each video."""
    return category


def _angle(a, b, c):
    ba, bc = a - b, c - b
    n1, n2 = np.linalg.norm(ba), np.linalg.norm(bc)
    if n1 < 1e-6 or n2 < 1e-6:
        return 0.0
    return float(np.degrees(np.arccos(np.clip(np.dot(ba, bc) / (n1 * n2), -1.0, 1.0))))


def frame_features(pts, prev_pts=None, dt_s=None):
    """
    pts: (33, >=3) normalised [x, y, z] landmarks of one frame (MediaPipe convention).
    Returns the 68-dim vector, identical in layout to the Kotlin implementation.
    """
    pts = np.asarray(pts, dtype=np.float32)[:, :3]
    hip_mid = (pts[L_HIP] + pts[R_HIP]) / 2.0
    sho_mid = (pts[L_SHO] + pts[R_SHO]) / 2.0
    center = (hip_mid + sho_mid) / 2.0
    torso = np.linalg.norm(sho_mid - hip_mid)
    sw = np.linalg.norm(pts[L_SHO] - pts[R_SHO])
    scale = torso if torso > 0.05 else (sw if sw > 0.05 else 1.0)

    f = [((pts[i] - center) / scale) for i in LANDMARK_INDICES]
    f = list(np.concatenate(f))
    f += [
        _angle(pts[L_SHO], pts[L_ELB], pts[L_WRI]) / 180.0,
        _angle(pts[R_SHO], pts[R_ELB], pts[R_WRI]) / 180.0,
        _angle(pts[L_HIP], pts[L_SHO], pts[L_ELB]) / 180.0,
        _angle(pts[R_HIP], pts[R_SHO], pts[R_ELB]) / 180.0,
        _angle(pts[L_SHO], pts[L_HIP], pts[L_KNE]) / 180.0,
        _angle(pts[R_SHO], pts[R_HIP], pts[R_KNE]) / 180.0,
        _angle(pts[L_HIP], pts[L_KNE], pts[L_ANK]) / 180.0,
        _angle(pts[R_HIP], pts[R_KNE], pts[R_ANK]) / 180.0,
    ]
    for a, b in [(L_WRI, L_SHO), (R_WRI, R_SHO), (L_WRI, NOSE), (R_WRI, NOSE), (L_WRI, R_WRI)]:
        f += list((pts[a] - pts[b]) / scale)
    if prev_pts is not None and dt_s is not None and 0 < dt_s <= 0.25:
        k = (1.0 / REFERENCE_FPS) / dt_s
        prev_pts = np.asarray(prev_pts, dtype=np.float32)[:, :3]
        f += list((pts[L_WRI] - prev_pts[L_WRI]) / scale * k)
        f += list((pts[R_WRI] - prev_pts[R_WRI]) / scale * k)
    else:
        f += [0.0] * 6
    out = np.asarray(f, dtype=np.float32)
    assert out.shape[0] == FEATURE_DIM, out.shape
    return out


def to_isotropic(seq, width, height):
    """Scale y by height/width so x, y, z share uniform square-pixel proportions (matching Kotlin)."""
    if width <= 0 or height <= 0 or width == height:
        return seq
    out = seq.copy()
    out[..., 1] *= float(height) / float(width)
    return out


def sequence_features(seq, fps=REFERENCE_FPS):
    """seq: (T, 33, >=3) uniformly sampled at fps → (T, 68)."""
    dt = 1.0 / fps
    return np.stack([frame_features(seq[i], seq[i - 1] if i > 0 else None, dt) for i in range(len(seq))])


def resample_uniform(t_ms, landmarks, fps=REFERENCE_FPS, max_gap_ms=120.0):
    """
    Linearly resample an irregular / gappy landmark track to a uniform fps grid.
    Frames with no detection are NaN in `landmarks`. Gaps longer than max_gap_ms split the track.
    Returns a list of contiguous segments, each (T, 33, C).
    """
    t_ms = np.asarray(t_ms, dtype=np.float64)
    lm = np.asarray(landmarks, dtype=np.float32)
    valid = ~np.isnan(lm[:, 0, 0])
    t_v, lm_v = t_ms[valid], lm[valid]
    if len(t_v) < 2:
        return []
    segments, start = [], 0
    for i in range(1, len(t_v) + 1):
        if i == len(t_v) or t_v[i] - t_v[i - 1] > max_gap_ms:
            ts, ls = t_v[start:i], lm_v[start:i]
            if len(ts) >= 2:
                grid = np.arange(ts[0], ts[-1] + 1e-6, 1000.0 / fps)
                flat = ls.reshape(len(ts), -1)
                res = np.stack([np.interp(grid, ts, flat[:, k]) for k in range(flat.shape[1])], axis=1)
                segments.append(res.reshape(len(grid), *ls.shape[1:]).astype(np.float32))
            start = i
    return segments


def wrist_speed_sw(seq, wrist_idx, fps=REFERENCE_FPS):
    """Wrist speed relative to the shoulder midpoint, in shoulder-widths per second (2D)."""
    sho_mid = (seq[:, L_SHO, :2] + seq[:, R_SHO, :2]) / 2.0
    sw = np.linalg.norm(seq[:, L_SHO, :2] - seq[:, R_SHO, :2], axis=1)
    sw = np.maximum(np.convolve(sw, np.ones(9) / 9, mode="same"), 1e-3)
    rel = (seq[:, wrist_idx, :2] - sho_mid) / sw[:, None]
    v = np.zeros(len(seq), dtype=np.float32)
    v[1:] = np.linalg.norm(np.diff(rel, axis=0), axis=1) * fps
    return np.convolve(v, np.ones(3) / 3, mode="same")


def mirror_sequence(seq):
    """Horizontal flip + left/right landmark swap (x is normalised to [0,1])."""
    out = seq.copy()
    out[..., 0] = 1.0 - out[..., 0]
    for a, b in MIRROR_PAIRS:
        out[:, [a, b]] = out[:, [b, a]]
    return out
