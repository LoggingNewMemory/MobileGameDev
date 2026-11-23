# Punch Definitions & Detection Review ("professor take")

This document has two jobs:

1. Give **operational definitions** of the four punches, written so that a camera-based system can check them.
   A definition a computer can't measure is useless for scoring.
2. Review the detection problem described in `problem.md` against the code as it actually was, and explain
   what was changed.

Everything below uses the BlazePose 33-landmark skeleton that both ML Kit (on the phone) and MediaPipe
(offline) produce.

---

## 1. Conventions

| Symbol | Meaning |
|---|---|
| **SW** | Shoulder width: the distance between the two shoulder landmarks. Every distance is measured in SW, so the rules hold whether you stand 1 m or 4 m from the camera. |
| **Body frame** | Origin at the shoulder midpoint, y points down. Positions are relative to the body, so stepping or swaying is not hand motion. |
| **Own shoulder** | The shoulder on the same side as the punching hand. "Height 0" means level with the shoulder line; negative is above it (the chin is about −0.4 SW), positive is below it (the solar plexus is about +0.5 SW). |
| **Lead / rear** | Orthodox: left = lead, right = rear. Southpaw: the reverse. |
| **Launch** | The moment the wrist starts its ballistic acceleration. |
| **Extremum** | The point of the stroke furthest from the launch point. Everything after it is retraction. |
| **Reach** | 3-D distance from the wrist to its own shoulder. A guard is about 0.5 SW; full extension is 1.3–1.6 SW. |
| **Across** | Horizontal travel toward the *opposite* shoulder. This holds for front and back cameras, mirrored or not. |

---

## 2. What every punch has in common: is it a punch at all?

A movement is a punch only if all of these are true. Only then do we ask which punch it is.

1. **Observable.** The punching wrist is visible the whole time, from launch to extremum. In code, the
   wrist must already have been tracked for at least 200 ms before launch, with in-frame likelihood of at
   least 0.65 when tracking starts. Short dips during the motion (blur) are allowed down to 0.30, for at
   most 2 frames.
2. **Ballistic.** Peak wrist speed is at least 3 SW/s. A real punch reaches 8–25 SW/s; sitting jitter
   stays at or below 1–2 SW/s.
3. **Substantial.** The wrist travels at least 0.30 SW from launch to extremum.
4. **Coherent.** Path efficiency (straight-line distance ÷ path length) is at least 0.6. A punch moves in
   one direction. Landmark jitter zig-zags, so it fails this even when it is fast.
5. **Outbound.** The stroke moves *away* from the guard. Retractions (reach shrinking, the hand dropping,
   or moving away from the midline) are never punches.
6. **One count per stroke.** After a counted punch, that hand must slow down (below 1.2 SW/s for 2 frames
   and at least 150 ms) before it can count again. The other hand is independent, so a 1-2 combo counts
   as 2.

---

## 3. The four punches

### 3.1 Jab (lead straight) and Cross (rear straight, called "Strike" in the app)

**Boxing definition.** A straight punch from the guard to the target along the shortest line. The elbow
extends almost fully, the fist rotates palm-down, and it returns to the guard along the same line.
The jab uses the lead hand and needs little hip rotation. The cross uses the rear hand and is driven
by hip and shoulder rotation plus a pivot on the rear foot.

**Operational definition.**
- Reach grows by at least 0.30 SW and gets to at least 0.80 SW, **and**
- the elbow angle at the extremum is at least 135° (when the elbow is visible), **and**
- the hand does not fall by more than 0.3 SW.
- Lead hand means jab; rear hand means cross. The stance setting decides which is which.

**Camera note.** From a front camera, a straight punch travels mostly *toward* the lens. Its 2-D motion
is small and the depth estimate is noisy. So the detector also accepts a weaker straight: a fast,
coherent, outbound stroke at head height that fits no other punch. It gets a low score, and the model
can re-rank it.

### 3.2 Hook

**Boxing definition.** A horizontal, curved punch. The elbow stays bent at roughly 90°, the forearm is
about parallel to the floor, and the fist travels in an arc across the body to the side of the
opponent's head or body. Power comes from rotating the torso about the lead (or rear) leg, not from
extending the arm.

**Operational definition.**
- Across travel is at least 0.30 SW (toward the opposite shoulder), **and**
- horizontal travel is at least as large as the vertical change, **and**
- the wrist stays between head and lower-chest height (−1.2 to +0.45 SW), **and**
- the elbow angle is between 55° and 155° (when visible), so the arm stays bent.

A hand moving *away* from the midline is a wind-up or a retraction, not a hook.

### 3.3 Uppercut

**Boxing definition.** A vertical punch that rises from below toward the opponent's chin or solar plexus.
The punching-side shoulder dips and the knees bend slightly (the "load"). Then the legs and hips drive
up, and the fist rises with the palm facing you. The elbow stays bent (roughly 70–120°) and close to
the body. The arm never fully extends.

**Operational definition.**
- The wrist rises at least 0.25 SW, **and**
- the rise is at least as large as the horizontal travel (vertical-dominant), **and**
- the wrist **launches from chest level or below** (start height of −0.2 SW or lower on screen, so it
  comes from below the chin), **and**
- the wrist **reaches about shoulder/chin level** (apex height at most +0.15 SW), **and**
- the elbow angle at the apex is at most 140° (when visible). A tucked elbow is often hidden against the
  torso, so a missing elbow does not veto the punch.

### 3.4 Edge cases

| Situation | Verdict | Why |
|---|---|---|
| **Hand is down / in the lap / off-screen.** Is it an uppercut? | **No.** It isn't a punch of any kind. | An uppercut has to be *observed*: the wrist must be visible as it launches and as it rises to chin level. A hand you can't see gives no evidence. A landmark BlazePose invents at the bottom edge of the image is a guess, not a measurement. |
| Hand rises **into** the frame from below and ends at the chin | **No.** | The launch was not observed. The track must exist for at least 200 ms before launch. |
| Hand resting low (by the hip) in view, then a fast rise to the chin | **Yes, uppercut.** | Every element is visible, ballistic, vertical-dominant, and from below to chin level. |
| Slow raise of the hand to the face (scratching your nose) | **No.** | Not ballistic: peak speed is below 3 SW/s. |
| Rise that stops at chest level | **No uppercut.** | It never reaches about shoulder level. |
| Fast rise with the arm fully extended upward | **No uppercut.** | The elbow is above 140°, so it is a raise or a "salute", not a punch. |
| Dip before the uppercut (the load) | Not counted. | It moves downward and outbound from the guard, so it fails every class. The detector re-arms right away so the real rise is still caught. |
| Dropping the hand after any punch | Not counted. | It is a retraction. |
| Wind-up (drawing the hand out wide) before a hook | Not counted. | It moves away from the midline. |
| Jab then cross within 0.2 s | 2 punches. | Each hand is tracked separately. The other arm is suppressed only for 150 ms, which covers the slight sympathetic motion of the non-punching hand. |
| Bust/selfie framing (shoulders in the bottom third, or shoulders wider than 60% of the frame) | Nothing counted; "Step back". | Hands can't be observed in that framing. |
| Elbow hidden against the ribs during an uppercut | Still an uppercut. | The definition is wrist-first. The elbow only refines the score. |
| Shovel hook (between a hook and an uppercut) | Whichever score is higher. | Both classes score it, and the higher one wins. This is a genuine ambiguity in boxing too. |

---

## 4. Review of the problem as it stood in the code

`problem.md` describes a two-stage pipeline (a velocity event detector, then a classifier). That shape is
right. The implementation had defects that brought the false positives back, or swapped them for misses.
Here is what I found in `PunchClassifier.kt` and `AIPoseMovementClassifier.kt` at commit `0d3d08f`:

| # | Defect | Effect |
|---|---|---|
| 1 | The wrist visibility gate was 0.38, but the doc measures ghost wrists at 0.35–0.55. | Ghost wrists passed the gate whenever they sat above 92% of the image height. |
| 2 | Velocity was in screen units per second, with a threshold of 0.65 s⁻¹. Ghost jitter of 0.015–0.035 per frame at 30 fps is 0.45–1.05 s⁻¹. | Jitter regularly crossed the "ballistic" threshold. Screen units also make thresholds depend on distance: far away, punches look slow (misses); close up, jitter looks fast (false positives). |
| 3 | No coherence test. | Jitter that zig-zags could pass the displacement test across a 7-frame window. |
| 4 | The TFLite model was fed **only on strike frames**. | Its 16-frame window was a collage of 16 unrelated strike moments, with "velocity" computed between strikes seconds apart. Its output was noise, and the fusion rule "AI says Upper and Δy ≥ 0.018" then turned noise into uppercuts. This was the main source of ghost uppercuts once the chin-level check failed. |
| 5 | The AI was trusted even when the hips were off-screen. | Hip-centred normalisation is undefined there (the doc's §3.4), and the model drifts to "Left Uppercut". |
| 6 | Wall-clock time at callback (`System.currentTimeMillis()`) was used instead of the frame timestamp. | ML Kit's variable latency went into dt, so velocities were wrong by tens of percent. |
| 7 | One global armed/cooldown state for both hands, re-armed only when *both* wrists were slow. | Real combos (1-2, jab-cross-hook) were swallowed, which shows up as missed strikes. |
| 8 | The uppercut test did not require the wrist to *start low*. | Any upward motion that ended near the shoulder could qualify. |
| 9 | Training: windows were labelled by a per-video displacement percentile, and train/validation were split randomly over overlapping windows from about 3 videos per class. | Labels included retractions and idle motion. The 98.6% validation accuracy was a leak, not generalisation. Velocity was also per video frame, while the app ran at a different FPS. |

### What changed

- **`StrikeDetector.kt`** (new, pure Kotlin, unit-tested) implements §2–§3 exactly:
  - shoulder-width units in a body frame
  - visibility hysteresis (acquire at 0.65, keep at 0.30 for up to 2 frames)
  - the 200 ms track-age rule
  - One-Euro smoothing and real timestamps
  - a per-arm READY → STROKE → RECOVERY state machine
  - ballistic, substantial and coherent validation
  - geometric scores with hard gates
  - the AI used as a *re-ranker only*: it can never create a punch on its own
- **`PunchClassifier.kt`** is now a thin ML Kit adapter. It feeds the TFLite model on **every** frame and
  only uses it when the body is in-distribution.
- **`AIPoseMovementClassifier.kt`**:
  - clears its buffer when gated
  - expresses velocities per 1/30 s
  - reports `inDistribution`
- **`TrainingScreen.kt`** passes the camera frame timestamp.
- **Tests** (`StrikeDetectorTest.kt`, 16 cases):
  - sitting with ghost wrists (including confident adversarial jitter, over 20 random seeds)
  - a hand rising from the lap
  - body sway, and bust framing
  - uppercut at 30 and 15 fps, with motion blur, and with a hidden elbow
  - a jab toward the camera, a hook, a 1-2 combo, a double jab
  - a slow arm raise
  - a sweep over noise × FPS × speed: 180/180 correct, 0 double counts

### Honest limits

- Thresholds come from biomechanics and synthetic trajectories, **not yet from real footage**. Before
  shipping, record about 10 minutes of real use (sitting, guard, every punch) and log `StrokeMetrics` to
  tune them. All thresholds live in `StrikeDetectorConfig`.
- With a front camera, telling jab/cross from hook relies partly on ML Kit's depth estimate, which is
  weak. A 30–45° camera angle (the classic "bladed" view) makes straights much easier to see.
- 3 videos per class from (likely) one person is not enough for a model that generalises. Aim for at
  least 10 people × 2 sessions × 20 reps per class, plus dedicated **negative** videos: sitting with hands
  off-screen, walking, adjusting the phone, drinking water.

---

## 5. Skeleton extraction: MediaPipe vs YOLOv8-Pose

**Use MediaPipe Pose Landmarker (the `full` model).** ML Kit Pose Detection on the phone *is* BlazePose.
MediaPipe gives the same 33-point topology, the same normalised x/y, a comparable z, and comparable
visibility semantics. Training on what you infer with removes a whole class of train/inference mismatch.

YOLOv8-Pose is a good detector, especially for several people or heavy occlusion, but:

- it outputs the 17-point COCO skeleton: no z, and different landmark definitions
- its confidences are calibrated differently, so the 0.65 / 0.30 gates would mean something else
- the app would need a second pose model (more battery, and a different noise profile) to benefit

Pick it only if you also move the phone runtime to YOLO.

Pipeline:

```bash
cd Dataset
python setup.py                                   # venv
python extract_skeletons.py --videos <drive_dataset_dir> --out skeletons --preview
python train_v2.py --skeletons skeletons --out models_v2 --cv
python train_v2.py --skeletons skeletons --out models_v2 --install-to ../AppSource/app/src/main/assets
```

`--preview` writes overlay videos. Wrists drawn **red** have confidence below 0.65, so the phone would
not trust them. Watch these to find clips with ghost limbs or bad framing before training.
