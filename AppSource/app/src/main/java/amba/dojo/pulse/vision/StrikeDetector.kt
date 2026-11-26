package amba.dojo.pulse.vision

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure-Kotlin punch event detector (no Android / ML Kit types, so it is unit-testable on the JVM).
 *
 * Implements the "operational definitions" in docs/PUNCH_DEFINITIONS.md.
 *
 * Pipeline per arm:
 *  0. Framing gate     – both shoulders must be confidently visible; body scale = shoulder width (SW).
 *  1. Visibility gate  – a wrist must be *acquired* (N consecutive confident, in-frame samples) before any
 *                        of its motion counts. Once acquired, short confidence dips (motion blur) are
 *                        tolerated for up to [StrikeDetectorConfig.maxGapFrames] frames. Ghost wrists that
 *                        BlazePose extrapolates at the bottom edge never get acquired.
 *  2. Kinematics       – positions are expressed relative to the shoulder midpoint in SW units
 *                        (distance-invariant, body-sway-invariant), smoothed with a One-Euro filter, and
 *                        differentiated with real frame timestamps (frame-rate independent).
 *  3. Stroke segmenter – READY → STROKE (speed ≥ start threshold) → end at deceleration or direction
 *                        reversal → RECOVERY (until the hand slows down again) → READY.
 *  4. Validation       – a stroke is only a punch if it is ballistic (peak speed), travels far enough
 *                        (net displacement) and is coherent (path efficiency). Random landmark jitter
 *                        fails efficiency even when it is fast.
 *  5. Classification   – geometric scores for STRAIGHT / HOOK / UPPERCUT with hard gates, optionally
 *                        fused with the TFLite model's probabilities. The model can only re-rank types
 *                        that are already kinematically plausible; it can never create a punch alone.
 */

/** BlazePose / ML Kit landmark indices used by the detector. */
object BlazePoseIndex {
    const val NOSE = 0
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
}

/** One landmark in image pixels. [z] uses ML Kit's convention (roughly pixel scale, negative = toward camera). */
data class Joint(val x: Float, val y: Float, val z: Float = 0f, val likelihood: Float = 1f)

/** One analysed camera frame. */
data class PoseFrame(
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val joints: Map<Int, Joint>
)

enum class Arm { LEFT, RIGHT }

enum class StrikeKind { STRAIGHT, HOOK, UPPERCUT }

/** Tunable thresholds. Speeds are in shoulder-widths per second (SW/s), distances in SW. */
data class StrikeDetectorConfig(
    // --- framing ---
    val minShoulderLikelihood: Float = 0.5f,
    val tooCloseShoulderY: Float = 0.68f,       // shoulder line in the bottom third → bust/selfie framing
    val tooCloseShoulderWidth: Float = 0.60f,   // shoulders span > 60% of the frame width

    // --- wrist visibility (hysteresis) ---
    val acquireLikelihood: Float = 0.65f,       // needed for N frames in a row to start tracking a wrist
    val acquireFrames: Int = 3,
    val minTrackAgeMs: Long = 200L,             // a stroke's launch must be observed on an already-settled track
    val keepLikelihood: Float = 0.30f,          // once tracked, motion-blurred frames down to this still count
    val maxGapFrames: Int = 2,                  // consecutive unusable frames tolerated before track loss
    val bottomEdgeMargin: Float = 0.08f,        // wrists in the bottom 8% of the image are treated as off-screen
    val sideEdgeMargin: Float = 0.01f,

    // --- smoothing (One-Euro) ---
    val filterMinCutoffHz: Float = 2.0f,
    val filterBeta: Float = 0.4f,

    // --- stroke segmentation ---
    val startSpeed: Float = 1.8f,
    val endSpeedFraction: Float = 0.35f,        // stroke ends when speed falls below 35% of its peak ...
    val endSpeedFloor: Float = 1.0f,            // ... or below this absolute speed
    val reversalFraction: Float = 0.80f,        // or when distance from launch drops below 80% of its max
    val maxStrokeMs: Long = 700L,
    val rearmSpeed: Float = 1.2f,
    val rearmFrames: Int = 2,
    val minRecoveryMs: Long = 150L,
    val crossArmSuppressMs: Long = 150L,        // the other arm cannot count within this window

    // --- punch validation ---
    val minPeakSpeed: Float = 3.0f,
    val minNetDisplacement: Float = 0.30f,
    val minPathEfficiency: Float = 0.60f,
    val zWeight: Float = 0.6f,                  // ML Kit depth is noisier than x/y; down-weight it

    // --- classification ---
    val minAcceptScore: Float = 0.30f,
    val aiWeight: Float = 0.25f
)

/** Diagnostics for one candidate stroke, useful for logging and threshold tuning. */
data class StrokeMetrics(
    val durationMs: Long,
    val peakSpeed: Float,
    val netDisplacement: Float,
    val pathEfficiency: Float,
    val rise: Float,            // max upward travel from launch (+ = up)
    val netVertical: Float,     // launch → extremum vertical change (+ = up, − = down)
    val lateral: Float,         // |horizontal| travel
    val across: Float,          // + = toward the opposite shoulder (mirror-invariant)
    val reachStart: Float,
    val reachGain: Float,
    val reachMax: Float,
    val startHeight: Float,     // wrist y relative to own shoulder at launch (+ = below shoulder)
    val apexHeight: Float,      // highest wrist y relative to own shoulder (+ = below shoulder)
    val elbowAngleAtEnd: Float  // degrees, NaN if the elbow was not reliably visible
)

data class StrikeEvent(
    val arm: Arm,
    val kind: StrikeKind,
    val score: Float,
    val timestampMs: Long,
    val metrics: StrokeMetrics
)

/** AI model output re-expressed as P(arm, kind). Null entries are treated as 0. */
fun interface AiStrikeProbabilities {
    fun probability(arm: Arm, kind: StrikeKind): Float
}

data class DetectorFrameResult(
    val event: StrikeEvent?,
    val isFramedTooClose: Boolean,
    val shouldersVisible: Boolean,
    val leftTracked: Boolean,
    val rightTracked: Boolean,
    val leftInGuard: Boolean,
    val rightInGuard: Boolean,
    val leftElbowAngle: Float,
    val rightElbowAngle: Float
)

class StrikeDetector(private val cfg: StrikeDetectorConfig = StrikeDetectorConfig()) {

    private data class Vec3(val x: Float, val y: Float, val z: Float) {
        operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    }

    private data class Sample(
        val t: Long,
        val p: Vec3,            // smoothed wrist, body frame (SW units, origin = shoulder midpoint)
        val shoulder: Vec3,     // own shoulder, body frame
        val elbowAngle: Float,
        val speed: Float,
        val acrossSign: Float   // sign of (opposite shoulder x − own shoulder x)
    )

    private enum class Phase { READY, STROKE, RECOVERY }

    private inner class ArmTracker(val arm: Arm) {
        var tracked = false
        var trackStartT = 0L
        var goodStreak = 0
        var gapFrames = 0
        val samples = ArrayDeque<Sample>()
        var phase = Phase.READY
        val stroke = ArrayList<Sample>()
        var strokeEndT = 0L
        var lowSpeedFrames = 0
        val fx = OneEuro(); val fy = OneEuro(); val fz = OneEuro()

        fun lose() {
            tracked = false
            goodStreak = 0
            gapFrames = 0
            samples.clear()
            stroke.clear()
            phase = Phase.READY
            fx.reset(); fy.reset(); fz.reset()
        }
    }

    private val left = ArmTracker(Arm.LEFT)
    private val right = ArmTracker(Arm.RIGHT)
    private var scale = 0f
    private var lastCountT = Long.MIN_VALUE / 2
    private var lastCountArm: Arm? = null

    fun reset() {
        left.lose(); right.lose()
        scale = 0f
        lastCountT = Long.MIN_VALUE / 2
        lastCountArm = null
    }

    fun process(frame: PoseFrame, ai: AiStrikeProbabilities? = null): DetectorFrameResult {
        val w = frame.width.toFloat().coerceAtLeast(1f)
        val h = frame.height.toFloat().coerceAtLeast(1f)
        val ls = frame.joints[BlazePoseIndex.LEFT_SHOULDER]
        val rs = frame.joints[BlazePoseIndex.RIGHT_SHOULDER]

        val shouldersVisible = ls != null && rs != null &&
                ls.likelihood >= cfg.minShoulderLikelihood && rs.likelihood >= cfg.minShoulderLikelihood
        if (!shouldersVisible) {
            left.lose(); right.lose()
            return emptyResult(tooClose = false, shoulders = false)
        }
        ls!!; rs!!

        val shoulderY = (ls.y + rs.y) / (2f * h)
        val sw2d = dist2(ls.x - rs.x, ls.y - rs.y)
        val tooClose = shoulderY > cfg.tooCloseShoulderY || sw2d / w > cfg.tooCloseShoulderWidth
        if (tooClose) {
            left.lose(); right.lose()
            return emptyResult(tooClose = true, shoulders = true)
        }

        // Body scale: 3D shoulder width (robust to the bladed boxing stance), EMA-smoothed.
        val sw3d = sqrt((ls.x - rs.x).sq() + (ls.y - rs.y).sq() + (ls.z - rs.z).sq())
        val rawScale = max(sw2d, min(sw3d, sw2d * 2.5f)).coerceAtLeast(12f)
        scale = if (scale <= 0f) rawScale else scale + 0.15f * (rawScale - scale)

        val origin = Vec3((ls.x + rs.x) / 2f, (ls.y + rs.y) / 2f, (ls.z + rs.z) / 2f)
        fun toBody(j: Joint) = Vec3((j.x - origin.x) / scale, (j.y - origin.y) / scale, (j.z - origin.z) / scale)

        val lsB = toBody(ls); val rsB = toBody(rs)
        val leftResult = updateArm(left, frame, BlazePoseIndex.LEFT_ELBOW, BlazePoseIndex.LEFT_WRIST, ls, lsB, rsB, w, h, ::toBody, ai)
        val rightResult = updateArm(right, frame, BlazePoseIndex.RIGHT_ELBOW, BlazePoseIndex.RIGHT_WRIST, rs, rsB, lsB, w, h, ::toBody, ai)

        val candidates = listOfNotNull(leftResult.event, rightResult.event)
        var accepted: StrikeEvent? = null
        for (ev in candidates.sortedByDescending { it.score }) {
            val suppressed = lastCountArm != null && lastCountArm != ev.arm &&
                    ev.timestampMs - lastCountT < cfg.crossArmSuppressMs
            if (suppressed) continue
            accepted = ev
            lastCountT = ev.timestampMs
            lastCountArm = ev.arm
            break
        }

        return DetectorFrameResult(
            event = accepted,
            isFramedTooClose = false,
            shouldersVisible = true,
            leftTracked = left.tracked,
            rightTracked = right.tracked,
            leftInGuard = leftResult.inGuard,
            rightInGuard = rightResult.inGuard,
            leftElbowAngle = leftResult.elbow,
            rightElbowAngle = rightResult.elbow
        )
    }

    private data class ArmUpdate(val event: StrikeEvent?, val inGuard: Boolean, val elbow: Float)

    private fun updateArm(
        a: ArmTracker,
        frame: PoseFrame,
        elbowIdx: Int,
        wristIdx: Int,
        shoulderJ: Joint,
        ownShoulder: Vec3,
        oppShoulder: Vec3,
        w: Float,
        h: Float,
        toBody: (Joint) -> Vec3,
        ai: AiStrikeProbabilities?
    ): ArmUpdate {
        val wrist = frame.joints[wristIdx]
        val elbow = frame.joints[elbowIdx]
        val inFrame = wrist != null &&
                wrist.y >= 0f && wrist.y <= h * (1f - cfg.bottomEdgeMargin) &&
                wrist.x >= w * cfg.sideEdgeMargin && wrist.x <= w * (1f - cfg.sideEdgeMargin)
        val needed = if (a.tracked) cfg.keepLikelihood else cfg.acquireLikelihood
        val good = inFrame && wrist!!.likelihood >= needed

        if (!good) {
            if (a.tracked) {
                a.gapFrames++
                if (a.gapFrames > cfg.maxGapFrames) a.lose()
            } else {
                a.goodStreak = 0
            }
            return ArmUpdate(null, false, Float.NaN)
        }
        wrist!!
        a.gapFrames = 0
        if (!a.tracked) {
            a.goodStreak++
            if (a.goodStreak < cfg.acquireFrames) return ArmUpdate(null, false, Float.NaN)
            a.tracked = true
            a.trackStartT = frame.timestampMs
        }

        val elbowAngle = if (elbow != null && elbow.likelihood >= 0.5f) {
            angle3d(shoulderJ, elbow, wrist)
        } else Float.NaN

        val raw = toBody(wrist)
        val tSec = frame.timestampMs / 1000.0
        val p = Vec3(a.fx.filter(raw.x, tSec), a.fy.filter(raw.y, tSec), a.fz.filter(raw.z, tSec))
        val acrossSign = if (oppShoulder.x >= ownShoulder.x) 1f else -1f

        val prev = a.samples.lastOrNull()
        var speed = 0f
        if (prev != null) {
            val dtMs = frame.timestampMs - prev.t
            if (dtMs <= 0L) return ArmUpdate(null, inGuard(p, ownShoulder), elbowAngle)
            if (dtMs > 250L) {
                // Discontinuity (dropped frames / app paused): restart kinematics, keep the track.
                a.samples.clear(); a.stroke.clear(); a.phase = Phase.READY
            } else {
                speed = wdist(p - prev.p) / (dtMs / 1000f)
            }
        }
        val s = Sample(frame.timestampMs, p, ownShoulder, elbowAngle, speed, acrossSign)
        a.samples.addLast(s)
        while (a.samples.size > 32) a.samples.removeFirst()

        val event = stepStateMachine(a, s, ai)
        return ArmUpdate(event, inGuard(p, ownShoulder), elbowAngle)
    }

    private fun stepStateMachine(a: ArmTracker, s: Sample, ai: AiStrikeProbabilities?): StrikeEvent? {
        when (a.phase) {
            Phase.READY -> {
                if (s.speed >= cfg.startSpeed && a.samples.size >= 2) {
                    a.phase = Phase.STROKE
                    a.stroke.clear()
                    // Launch anchor: walk back while speed keeps decreasing (the moment the hand started moving).
                    val list = a.samples.toList()
                    var i = list.size - 2
                    while (i > 0 && list[i - 1].speed < list[i].speed && s.t - list[i - 1].t <= 200L) i--
                    // The launch must be observed on a settled track: a hand that only became visible
                    // mid-motion (e.g. rising into frame from the lap) has no observed start → not a punch.
                    if (list[i].t - a.trackStartT < cfg.minTrackAgeMs) {
                        a.phase = Phase.RECOVERY
                        a.strokeEndT = s.t
                        a.lowSpeedFrames = 0
                        return null
                    }
                    for (k in i until list.size) a.stroke.add(list[k])
                }
                return null
            }
            Phase.STROKE -> {
                a.stroke.add(s)
                val anchor = a.stroke.first()
                val peak = a.stroke.maxOf { it.speed }
                val maxDist = a.stroke.maxOf { wdist(it.p - anchor.p) }
                val curDist = wdist(s.p - anchor.p)
                val decelerated = s.speed < max(cfg.endSpeedFloor, peak * cfg.endSpeedFraction)
                val reversed = maxDist > 0.1f && curDist < maxDist * cfg.reversalFraction
                val timedOut = s.t - anchor.t > cfg.maxStrokeMs
                if (decelerated || reversed || timedOut) {
                    val metrics = measure(a.stroke)
                    a.stroke.clear()
                    val event = if (metrics != null && isBallistic(metrics)) classify(a.arm, metrics, s.t, ai) else null
                    if (event == null) {
                        // Not a punch (e.g. the slow "load" dip before an uppercut, or a wind-up before a
                        // hook). Re-arm immediately so the real strike that follows is not swallowed.
                        a.phase = Phase.READY
                        return null
                    }
                    a.phase = Phase.RECOVERY
                    a.strokeEndT = s.t
                    a.lowSpeedFrames = 0
                    return event
                }
                return null
            }
            Phase.RECOVERY -> {
                a.lowSpeedFrames = if (s.speed < cfg.rearmSpeed) a.lowSpeedFrames + 1 else 0
                if (a.lowSpeedFrames >= cfg.rearmFrames && s.t - a.strokeEndT >= cfg.minRecoveryMs) {
                    a.phase = Phase.READY
                }
                return null
            }
        }
    }

    private fun measure(stroke: List<Sample>): StrokeMetrics? {
        if (stroke.size < 2) return null
        val anchor = stroke.first()
        // Extremum = furthest point from launch; everything after it is retraction.
        var endIdx = 0
        var best = -1f
        for (i in stroke.indices) {
            val d = wdist(stroke[i].p - anchor.p)
            if (d > best) { best = d; endIdx = i }
        }
        if (endIdx == 0) return null
        val seg = stroke.subList(0, endIdx + 1)
        val end = seg.last()
        var path = 0f
        for (i in 1 until seg.size) path += wdist(seg[i].p - seg[i - 1].p)
        val net = wdist(end.p - anchor.p)
        val reachStart = dist3(anchor.p - anchor.shoulder)
        val reachMax = seg.maxOf { dist3(it.p - it.shoulder) }
        val minY = seg.minOf { it.p.y - it.shoulder.y }
        val dx = end.p.x - anchor.p.x
        return StrokeMetrics(
            durationMs = end.t - anchor.t,
            peakSpeed = seg.drop(1).maxOf { it.speed },
            netDisplacement = net,
            pathEfficiency = if (path > 1e-6f) net / path else 0f,
            rise = (anchor.p.y - anchor.shoulder.y) - minY,
            netVertical = anchor.p.y - end.p.y,
            lateral = abs(dx),
            across = dx * anchor.acrossSign,
            reachStart = reachStart,
            reachGain = reachMax - reachStart,
            reachMax = reachMax,
            startHeight = anchor.p.y - anchor.shoulder.y,
            apexHeight = minY,
            elbowAngleAtEnd = end.elbowAngle
        )
    }

    private fun isBallistic(m: StrokeMetrics): Boolean =
        m.peakSpeed >= cfg.minPeakSpeed &&
                m.netDisplacement >= cfg.minNetDisplacement &&
                m.pathEfficiency >= cfg.minPathEfficiency &&
                m.durationMs in 20L..cfg.maxStrokeMs

    /** Geometric scores per kind (0 = impossible). Public for tests and tuning. */
    fun scoreKinds(m: StrokeMetrics): Map<StrikeKind, Float> {
        val elbowKnown = !m.elbowAngleAtEnd.isNaN()
        val vertical = m.rise
        val netV = m.netVertical

        // UPPERCUT: wrist launched from chest level or below, rises vertically to ~shoulder/chin height,
        // elbow stays bent. Hand must have been visible the whole time (guaranteed by tracking).
        val upper = if (
            vertical >= 0.25f &&
            vertical >= 1.0f * m.lateral &&
            m.startHeight >= -0.20f &&      // started no higher than just above the shoulder line
            m.apexHeight <= 0.15f &&        // reached at least (about) shoulder level
            (!elbowKnown || m.elbowAngleAtEnd <= 140f)
        ) {
            0.5f * ramp(vertical, 0.25f, 0.6f) +
                    0.3f * ramp(vertical / (m.lateral + 0.05f), 1.0f, 2.5f) +
                    0.2f * (if (elbowKnown) 1f else 0.5f)
        } else 0f

        // HOOK: horizontal arc toward the opposite side at head/shoulder height with a bent elbow.
        val hook = if (
            m.across >= 0.30f &&
            m.lateral >= 1.0f * abs(netV) &&
            m.apexHeight in -1.2f..0.45f &&
            (!elbowKnown || m.elbowAngleAtEnd in 55f..155f)
        ) {
            0.5f * ramp(m.across, 0.30f, 0.8f) +
                    0.3f * ramp(m.lateral / (abs(netV) + 0.05f), 1.0f, 2.5f) +
                    0.2f * (if (elbowKnown) 1f else 0.5f)
        } else 0f

        // STRAIGHT (jab / cross): the arm extends — reach grows toward full arm length, elbow opens.
        val strongStraight = m.reachGain >= 0.30f && m.reachMax >= 0.80f && netV >= -0.30f &&
                (!elbowKnown || m.elbowAngleAtEnd >= 135f)
        val straight = when {
            strongStraight ->
                0.5f * ramp(m.reachGain, 0.30f, 0.8f) +
                        0.3f * ramp(m.reachMax, 0.80f, 1.3f) +
                        0.2f * (if (elbowKnown) ramp(m.elbowAngleAtEnd, 135f, 170f) else 0.5f)
            // Weak fallback for straight punches thrown directly at the camera, where 2D travel and the
            // noisy depth channel under-report extension. Excludes retractions (reach shrinking, hand
            // dropping, or moving away from the midline).
            m.reachGain >= -0.10f && netV >= -0.15f && m.across >= -0.20f &&
                    m.apexHeight in -1.2f..0.45f -> 0.32f
            else -> 0f
        }
        return mapOf(StrikeKind.STRAIGHT to straight, StrikeKind.HOOK to hook, StrikeKind.UPPERCUT to upper)
    }

    private fun classify(arm: Arm, m: StrokeMetrics, t: Long, ai: AiStrikeProbabilities?): StrikeEvent? {
        val geo = scoreKinds(m)
        var bestKind: StrikeKind? = null
        var bestScore = 0f
        for ((kind, g) in geo) {
            if (g <= 0f) continue // AI may only re-rank kinematically plausible kinds
            val fused = if (ai != null) {
                (1f - cfg.aiWeight) * g + cfg.aiWeight * ai.probability(arm, kind).coerceIn(0f, 1f)
            } else g
            if (fused > bestScore) { bestScore = fused; bestKind = kind }
        }
        if (bestKind == null || bestScore < cfg.minAcceptScore) return null
        return StrikeEvent(arm, bestKind, bestScore, t, m)
    }

    private fun inGuard(p: Vec3, ownShoulder: Vec3): Boolean =
        (p.y - ownShoulder.y) in -1.1f..0.6f && abs(p.x - ownShoulder.x) < 0.9f

    private fun emptyResult(tooClose: Boolean, shoulders: Boolean) = DetectorFrameResult(
        event = null, isFramedTooClose = tooClose, shouldersVisible = shoulders,
        leftTracked = false, rightTracked = false, leftInGuard = false, rightInGuard = false,
        leftElbowAngle = Float.NaN, rightElbowAngle = Float.NaN
    )

    private fun wdist(v: Vec3) = sqrt(v.x * v.x + v.y * v.y + (cfg.zWeight * v.z).sq())
    private fun dist3(v: Vec3) = sqrt(v.x * v.x + v.y * v.y + (cfg.zWeight * v.z).sq())

    private inner class OneEuro {
        private var xPrev = 0f
        private var dxPrev = 0f
        private var tPrev = -1.0
        fun reset() { tPrev = -1.0; dxPrev = 0f }
        fun filter(x: Float, t: Double): Float {
            if (tPrev < 0 || t <= tPrev) { xPrev = x; tPrev = t; dxPrev = 0f; return x }
            val dt = (t - tPrev).toFloat()
            val dx = (x - xPrev) / dt
            val aD = alpha(1.0f, dt)
            val dxHat = aD * dx + (1 - aD) * dxPrev
            val cutoff = cfg.filterMinCutoffHz + cfg.filterBeta * abs(dxHat)
            val a = alpha(cutoff, dt)
            val xHat = a * x + (1 - a) * xPrev
            xPrev = xHat; dxPrev = dxHat; tPrev = t
            return xHat
        }
        private fun alpha(cutoff: Float, dt: Float): Float {
            val tau = 1f / (2f * Math.PI.toFloat() * cutoff)
            return 1f / (1f + tau / dt)
        }
    }

    companion object {
        private fun Float.sq() = this * this
        private fun dist2(dx: Float, dy: Float) = sqrt(dx * dx + dy * dy)
        fun ramp(v: Float, lo: Float, hi: Float): Float = ((v - lo) / (hi - lo)).coerceIn(0f, 1f)

        fun angle3d(a: Joint, b: Joint, c: Joint): Float {
            val bax = a.x - b.x; val bay = a.y - b.y; val baz = a.z - b.z
            val bcx = c.x - b.x; val bcy = c.y - b.y; val bcz = c.z - b.z
            val n1 = sqrt(bax * bax + bay * bay + baz * baz)
            val n2 = sqrt(bcx * bcx + bcy * bcy + bcz * bcz)
            if (n1 < 1e-6f || n2 < 1e-6f) return Float.NaN
            val cos = ((bax * bcx + bay * bcy + baz * bcz) / (n1 * n2)).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(cos).toDouble()).toFloat()
        }
    }
}
