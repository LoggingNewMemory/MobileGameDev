package amba.dojo.pulse.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Synthetic-trajectory tests for [StrikeDetector].
 *
 * Trajectories are described in body units (shoulder widths, SW) relative to the *own* shoulder,
 * y down, and rendered to pixels like ML Kit would report them. The subject faces the camera,
 * so the subject's LEFT shoulder appears on the image RIGHT (larger x).
 */
class StrikeDetectorTest {

    private val w = 720
    private val h = 1280

    /** Wrist/elbow state for one arm in body units relative to the own shoulder. */
    private data class ArmPose(
        val x: Float, val y: Float, val z: Float = 0f,
        val elbowDeg: Float = Float.NaN,
        val likelihood: Float = 0.95f
    )

    private class Scene(
        val shoulderY: Float = 448f,     // px
        val centerX: Float = 360f,       // px
        val swPx: Float = 140f           // shoulder width in px
    )

    private fun frame(
        t: Long,
        scene: Scene,
        left: ArmPose?,
        right: ArmPose?,
        shoulderLikelihood: Float = 0.99f,
        bodyShiftX: Float = 0f
    ): PoseFrame {
        val j = HashMap<Int, Joint>()
        val lsx = scene.centerX + bodyShiftX + scene.swPx / 2f
        val rsx = scene.centerX + bodyShiftX - scene.swPx / 2f
        j[BlazePoseIndex.LEFT_SHOULDER] = Joint(lsx, scene.shoulderY, 0f, shoulderLikelihood)
        j[BlazePoseIndex.RIGHT_SHOULDER] = Joint(rsx, scene.shoulderY, 0f, shoulderLikelihood)
        fun put(arm: ArmPose?, sx: Float, wristIdx: Int, elbowIdx: Int) {
            if (arm == null) return
            val wx = sx + arm.x * scene.swPx
            val wy = scene.shoulderY + arm.y * scene.swPx
            val wz = arm.z * scene.swPx
            j[wristIdx] = Joint(wx, wy, wz, arm.likelihood)
            if (!arm.elbowDeg.isNaN()) {
                // Isosceles elbow placement that yields exactly elbowDeg at the elbow.
                val dx = wx - sx; val dy = wy - scene.shoulderY; val dz = wz
                val d = sqrt(dx * dx + dy * dy + dz * dz)
                val half = (arm.elbowDeg / 2f) * (PI.toFloat() / 180f)
                val l = d / (2f * sin(half))
                val off = sqrt((l * l - (d / 2f) * (d / 2f)).coerceAtLeast(0f))
                // perpendicular to SW in the x-y plane (pointing down-ish)
                val nx = -dy / d; val ny = dx / d
                val sign = if (ny >= 0) 1f else -1f
                j[elbowIdx] = Joint(
                    sx + dx / 2f + sign * nx * off,
                    scene.shoulderY + dy / 2f + sign * ny * off,
                    dz / 2f,
                    0.9f
                )
            }
        }
        put(left, lsx, BlazePoseIndex.LEFT_WRIST, BlazePoseIndex.LEFT_ELBOW)
        put(right, rsx, BlazePoseIndex.RIGHT_WRIST, BlazePoseIndex.RIGHT_ELBOW)
        return PoseFrame(t, w, h, j)
    }

    // Guard positions: fists near the chin, slightly inside the shoulders.
    private val leftGuard = ArmPose(-0.25f, -0.35f, -0.2f, 50f)
    private val rightGuard = ArmPose(0.25f, -0.35f, -0.2f, 50f)

    private fun lerp(a: ArmPose, b: ArmPose, u: Float): ArmPose {
        val s = (1 - cos(u.coerceIn(0f, 1f) * PI.toFloat())) / 2f // ease in/out → ballistic bell-shaped speed
        fun l(x: Float, y: Float) = x + (y - x) * s
        return ArmPose(
            l(a.x, b.x), l(a.y, b.y), l(a.z, b.z),
            if (a.elbowDeg.isNaN() || b.elbowDeg.isNaN()) Float.NaN else l(a.elbowDeg, b.elbowDeg),
            minOf(a.likelihood, b.likelihood)
        )
    }

    /** Piecewise trajectory: list of (durationMs, target). Starts at [start]. */
    private fun path(start: ArmPose, vararg legs: Pair<Long, ArmPose>): (Long) -> ArmPose = { t ->
        var cur = start
        var t0 = 0L
        var out = start
        var done = false
        for ((dur, target) in legs) {
            if (!done && t < t0 + dur) {
                out = lerp(cur, target, (t - t0).toFloat() / dur)
                done = true
            }
            cur = target
            t0 += dur
        }
        if (done) out else cur
    }

    private fun run(
        fps: Int,
        durationMs: Long,
        scene: Scene = Scene(),
        leftFn: (Long) -> ArmPose? = { leftGuard },
        rightFn: (Long) -> ArmPose? = { rightGuard },
        shift: (Long) -> Float = { 0f },
        detector: StrikeDetector = StrikeDetector()
    ): List<StrikeEvent> {
        val events = ArrayList<StrikeEvent>()
        val step = 1000L / fps
        var t = 0L
        while (t <= durationMs) {
            val r = detector.process(frame(1_000_000L + t, scene, leftFn(t), rightFn(t), bodyShiftX = shift(t)))
            r.event?.let { events.add(it) }
            t += step
        }
        return events
    }

    // ------------------------------------------------------------------------------------------
    // False-positive scenarios (the "ghost uppercut while sitting" bug)
    // ------------------------------------------------------------------------------------------

    @Test
    fun sittingClose_ghostWristsAtBottomEdge_noStrikes() {
        val rnd = Random(1)
        val scene = Scene(shoulderY = 0.60f * h, swPx = 300f)
        // BlazePose hallucinates wrists near y≈0.88–0.95, likelihood 0.35–0.55, jitter ±40 px.
        val ghost: (Long) -> ArmPose = {
            ArmPose(
                x = rnd.nextFloat() * 0.27f - 0.13f,
                y = (0.91f * h + rnd.nextFloat() * 80f - 40f - scene.shoulderY) / scene.swPx,
                likelihood = 0.35f + rnd.nextFloat() * 0.20f
            )
        }
        val events = run(30, 15_000, scene, ghost, ghost)
        assertEquals("ghost wrists must never count", 0, events.size)
    }

    @Test
    fun sittingClose_confidentButJitteryWrists_noStrikes() {
        // Adversarial: ghosts that pass the visibility gate (likelihood 0.7, inside the frame).
        val rnd = Random(7)
        val scene = Scene(shoulderY = 0.60f * h, swPx = 300f)
        val ghost: (Long) -> ArmPose = {
            ArmPose(
                x = rnd.nextFloat() * 0.27f - 0.13f,
                y = 0.9f + rnd.nextFloat() * 0.27f - 0.13f,
                likelihood = 0.70f
            )
        }
        val events = run(30, 15_000, scene, ghost, ghost)
        assertEquals("incoherent jitter must fail the path-efficiency gate", 0, events.size)
    }

    @Test
    fun handRisingIntoFrameFromLap_isNotAnUppercut() {
        val scene = Scene(shoulderY = 0.60f * h, swPx = 300f)
        // Hand starts off-screen below the frame (no landmark confidence), then rises to the chin.
        val offScreen = ArmPose(-0.2f, 2.0f, 0f, likelihood = 0.1f)
        val chin = ArmPose(-0.2f, -0.4f, 0f, 80f)
        val left = path(offScreen, 1000L to offScreen, 220L to chin.copy(likelihood = 0.9f), 1500L to chin)
        val events = run(30, 3000, scene, left, { null })
        assertEquals(0, events.count { it.kind == StrikeKind.UPPERCUT })
    }

    @Test
    fun guardHeld_bodySwayAndStepping_noStrikes() {
        val events = run(30, 6000, shift = { t -> 120f * sin(t / 1000f * 2f * PI.toFloat() * 0.8f) })
        assertEquals(0, events.size)
    }

    @Test
    fun noShoulders_orTooClose_noStrikes() {
        val det = StrikeDetector()
        val scene = Scene(shoulderY = 0.75f * h, swPx = 300f)
        val left = path(leftGuard, 500L to leftGuard, 120L to ArmPose(-0.2f, -0.5f, -1.2f, 165f), 400L to leftGuard)
        var count = 0
        var t = 0L
        while (t < 2000) {
            if (det.process(frame(t, scene, left(t), rightGuard)).event != null) count++
            t += 33
        }
        assertEquals(0, count)
    }

    // ------------------------------------------------------------------------------------------
    // True-positive scenarios
    // ------------------------------------------------------------------------------------------

    private val chestLoad = ArmPose(-0.15f, 0.45f, -0.1f, 85f)
    private val chinApex = ArmPose(-0.25f, -0.45f, -0.4f, 80f)

    private fun leftUppercut(blur: Boolean = false): (Long) -> ArmPose {
        val apex = if (blur) chinApex.copy(likelihood = 0.40f) else chinApex
        return path(
            leftGuard,
            600L to leftGuard,
            180L to chestLoad,         // slow load / dip
            150L to apex,              // explosive rise
            350L to leftGuard,         // return to guard
            800L to leftGuard
        )
    }

    @Test
    fun leftUppercut_30fps_countedOnce() {
        val events = run(30, 2500, leftFn = leftUppercut())
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    @Test
    fun leftUppercut_15fps_withMotionBlur_stillCounted() {
        val events = run(15, 2500, leftFn = leftUppercut(blur = true))
        assertEquals(events.toString(), 1, events.size)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    @Test
    fun uppercut_withTuckedElbowNotVisible_stillCounted() {
        // Elbow hidden against the torso → elbow landmark unusable; wrist-only definition must still work.
        val noElbow = { p: ArmPose -> p.copy(elbowDeg = Float.NaN) }
        val f = leftUppercut()
        val events = run(30, 2500, leftFn = { t -> noElbow(f(t)) })
        assertEquals(1, events.size)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    private val leftJabExtended = ArmPose(-0.45f, -0.45f, -1.4f, 168f)

    @Test
    fun leftJab_towardCamera_countedOnce_retractionIgnored() {
        val left = path(leftGuard, 500L to leftGuard, 130L to leftJabExtended, 180L to leftGuard, 1000L to leftGuard)
        val events = run(30, 2000, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun rightHook_countedOnce_retractionIgnored() {
        // Rear hand swings from outside the right shoulder (image-left, −x) across the face toward the
        // left side (+x) at head height.
        val wide = ArmPose(-0.30f, -0.30f, -0.3f, 95f)
        val across = ArmPose(0.80f, -0.40f, -0.6f, 95f)
        val right = path(rightGuard, 500L to rightGuard, 120L to wide, 160L to across, 300L to rightGuard, 800L to rightGuard)
        val events = run(30, 2200, rightFn = right)
        assertEquals(events.toString(), 1, events.count { it.kind == StrikeKind.HOOK })
        assertTrue(events.all { it.arm == Arm.RIGHT })
        assertEquals(1, events.size)
    }

    @Test
    fun jabCrossCombo_bothCounted() {
        val rightCrossExt = ArmPose(0.55f, -0.45f, -1.5f, 170f)
        val left = path(leftGuard, 500L to leftGuard, 130L to leftJabExtended, 170L to leftGuard, 1500L to leftGuard)
        val right = path(rightGuard, 800L to rightGuard, 140L to rightCrossExt, 180L to rightGuard, 1000L to rightGuard)
        val events = run(30, 2400, leftFn = left, rightFn = right)
        assertEquals(events.toString(), 2, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(Arm.RIGHT, events[1].arm)
        assertTrue(events.all { it.kind == StrikeKind.STRAIGHT })
    }

    @Test
    fun doubleJab_countedTwice() {
        val left = path(
            leftGuard, 500L to leftGuard,
            130L to leftJabExtended, 170L to leftGuard, 250L to leftGuard,
            130L to leftJabExtended, 170L to leftGuard, 800L to leftGuard
        )
        val events = run(30, 2400, leftFn = left)
        assertEquals(events.toString(), 2, events.size)
    }

    @Test
    fun slowArmRaise_isNotAPunch() {
        // Lifting the hand to scratch the face: same geometry as an uppercut but not ballistic.
        val left = path(leftGuard, 300L to chestLoad, 300L to chestLoad, 900L to chinApex, 600L to leftGuard)
        val events = run(30, 2500, leftFn = left)
        assertEquals(0, events.size)
    }

    @Test
    fun scoreKinds_handDropping_isNeverUppercut() {
        val det = StrikeDetector()
        val m = StrokeMetrics(
            durationMs = 150, peakSpeed = 6f, netDisplacement = 0.8f, pathEfficiency = 0.95f,
            rise = 0f, netVertical = -0.8f, lateral = 0.05f, across = 0f, reachStart = 0.6f, reachGain = 0.1f, reachMax = 0.7f,
            startHeight = -0.4f, apexHeight = -0.4f, elbowAngleAtEnd = 90f
        )
        assertEquals(0f, det.scoreKinds(m)[StrikeKind.UPPERCUT])
        assertEquals(0f, det.scoreKinds(m)[StrikeKind.STRAIGHT])
    }

    // ------------------------------------------------------------------------------------------
    // Robustness sweep: landmark noise, frame rates, punch speeds
    // ------------------------------------------------------------------------------------------

    private fun noisy(rnd: Random, sd: Float, f: (Long) -> ArmPose?): (Long) -> ArmPose? = { t ->
        f(t)?.let {
            fun n() = (rnd.nextFloat() + rnd.nextFloat() + rnd.nextFloat() - 1.5f) * sd * 2f
            it.copy(x = it.x + n(), y = it.y + n(), z = it.z + 2f * n(),
                likelihood = (it.likelihood - rnd.nextFloat() * 0.25f).coerceAtLeast(0.32f))
        }
    }

    @Test
    fun sweep_noiseFpsAndSpeed_highRecallNoDoubleCounts() {
        var trials = 0
        var correct = 0
        var extra = 0
        for (fps in listOf(15, 24, 30, 60)) for (riseMs in listOf(110L, 160L, 240L)) for (seed in 0 until 5) {
            val rnd = Random(seed * 31 + fps)
            val up = path(leftGuard, 600L to leftGuard, 200L to chestLoad, riseMs to chinApex, 350L to leftGuard, 800L to leftGuard)
            val jab = path(leftGuard, 600L to leftGuard, riseMs to leftJabExtended, 200L to leftGuard, 800L to leftGuard)
            val hookR = path(rightGuard, 600L to rightGuard, 150L to ArmPose(-0.30f, -0.30f, -0.3f, 95f),
                riseMs to ArmPose(0.80f, -0.40f, -0.6f, 95f), 300L to rightGuard, 800L to rightGuard)
            for ((name, scenario) in listOf(
                "upper" to Triple(up, { _: Long -> rightGuard } as (Long) -> ArmPose?, StrikeKind.UPPERCUT),
                "jab" to Triple(jab, { _: Long -> rightGuard } as (Long) -> ArmPose?, StrikeKind.STRAIGHT),
                "hook" to Triple({ _: Long -> leftGuard } as (Long) -> ArmPose?, hookR, StrikeKind.HOOK)
            )) {
                trials++
                val ev = run(fps, 2600, leftFn = noisy(rnd, 0.02f, scenario.first), rightFn = noisy(rnd, 0.02f, scenario.second))
                if (ev.size == 1 && ev[0].kind == scenario.third) correct++
                if (ev.size > 1) extra++
                if (ev.size != 1 || ev[0].kind != scenario.third) println("miss $name fps=$fps rise=$riseMs seed=$seed -> ${ev.map { it.arm to it.kind }}")
            }
        }
        println("sweep: $correct / $trials correct, $extra multi-counts")
        assertTrue("recall ${correct}/${trials}", correct >= trials * 0.9)
        assertEquals(0, extra)
    }

    @Test
    fun sweep_ghostSeeds_zeroFalsePositives() {
        var total = 0
        for (seed in 0 until 20) {
            val rnd = Random(seed)
            val scene = Scene(shoulderY = (0.50f + 0.17f * rnd.nextFloat()) * h, swPx = 220f + 120f * rnd.nextFloat())
            val ghost: (Long) -> ArmPose = {
                ArmPose(
                    x = rnd.nextFloat() * 0.3f - 0.15f,
                    y = (0.90f * h + rnd.nextFloat() * 100f - 50f - scene.shoulderY) / scene.swPx,
                    likelihood = 0.30f + rnd.nextFloat() * 0.45f
                )
            }
            total += run(30, 10_000, scene, ghost, ghost).size
        }
        assertEquals(0, total)
    }

    // ------------------------------------------------------------------------------------------
    // Half-body vs Full-body framing, AI non-veto, and real-world edge cases
    // ------------------------------------------------------------------------------------------

    @Test
    fun halfBodyFraming_portraitCloseShoulders_detectsStrikesAndReportsHalfBody() {
        // Portrait half-body: shoulders span 68% of frame width, no hips/knees in frame.
        val scene = Scene(shoulderY = 0.42f * h, swPx = 0.68f * w)
        val det = StrikeDetector()
        val left = path(
            leftGuard,
            500L to leftGuard,
            130L to leftJabExtended,
            180L to leftGuard,
            700L to leftGuard
        )
        val events = ArrayList<StrikeEvent>()
        var lastMode = FramingMode.NONE
        var t = 0L
        while (t <= 1600L) {
            val r = det.process(frame(1_000_000L + t, scene, left(t), rightGuard))
            lastMode = r.framingMode
            r.event?.let { events.add(it) }
            t += 33L
        }
        assertEquals(FramingMode.HALF_BODY, lastMode)
        assertEquals(1, events.size)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun fullBodyFraming_withAdversarialZeroAi_doesNotVetoFrontJab() {
        val scene = Scene(shoulderY = 0.30f * h, swPx = 140f)
        val det = StrikeDetector()
        // Front-facing jab with weak depth report (relies on straight fallback).
        val weakDepthJab = ArmPose(-0.25f, -0.35f, -0.75f, 125f)
        val left = path(leftGuard, 500L to leftGuard, 130L to weakDepthJab, 180L to leftGuard, 600L to leftGuard)
        val zeroAi = AiStrikeProbabilities { _, _ -> 0f }
        val events = ArrayList<StrikeEvent>()
        var lastMode = FramingMode.NONE
        var t = 0L
        while (t <= 1500L) {
            val base = frame(1_000_000L + t, scene, left(t), rightGuard)
            val withLegs = base.joints.toMutableMap().apply {
                put(BlazePoseIndex.LEFT_HIP, Joint(400f, 700f, 0f, 0.9f))
                put(BlazePoseIndex.RIGHT_HIP, Joint(320f, 700f, 0f, 0.9f))
                put(BlazePoseIndex.LEFT_KNEE, Joint(400f, 950f, 0f, 0.9f))
                put(BlazePoseIndex.RIGHT_KNEE, Joint(320f, 950f, 0f, 0.9f))
            }
            val r = det.process(base.copy(joints = withLegs), zeroAi)
            lastMode = r.framingMode
            r.event?.let { events.add(it) }
            t += 33L
        }
        assertEquals(FramingMode.FULL_BODY, lastMode)
        assertEquals(1, events.size)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun crossAimedAtCenterline_extendedElbow_classifiedAsStraightNotHook() {
        val scene = Scene()
        // Right cross starts at right guard and drives forward + across toward centerline with extended elbow.
        val rightCrossCenter = ArmPose(0.55f, -0.40f, -1.4f, 165f)
        val right = path(rightGuard, 500L to rightGuard, 130L to rightCrossCenter, 180L to rightGuard, 700L to rightGuard)
        val events = run(30, 1600, scene, rightFn = right)
        assertEquals(1, events.size)
        assertEquals(Arm.RIGHT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun briefShoulderOcclusion_doesNotDropStroke() {
        val scene = Scene()
        val det = StrikeDetector()
        val left = path(leftGuard, 500L to leftGuard, 130L to leftJabExtended, 180L to leftGuard, 700L to leftGuard)
        val events = ArrayList<StrikeEvent>()
        var t = 0L
        while (t <= 1600L) {
            // Drop shoulder likelihood for 2 frames right before launch (t = 462..495)
            val sLik = if (t in 460L..500L) 0.2f else 0.99f
            val r = det.process(frame(1_000_000L + t, scene, left(t), rightGuard, shoulderLikelihood = sLik))
            r.event?.let { events.add(it) }
            t += 33L
        }
        assertEquals(1, events.size)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun scoreKinds_moderateStraightExtension_aboveMinAcceptScore() {
        val det = StrikeDetector()
        // Just inside strongStraight gates: previously scored ~0.12 (< 0.30 minAcceptScore) and was dropped.
        val m = StrokeMetrics(
            durationMs = 130, peakSpeed = 4.5f, netDisplacement = 0.45f, pathEfficiency = 0.90f,
            rise = 0.05f, netVertical = 0.0f, lateral = 0.15f, across = 0.15f, forwardZ = 0.35f,
            reachStart = 0.50f, reachGain = 0.30f, reachMax = 0.80f,
            startHeight = -0.35f, apexHeight = -0.40f, elbowAngleAtEnd = 140f
        )
        val scores = det.scoreKinds(m)
        assertTrue("straight=${scores[StrikeKind.STRAIGHT]} must be >= 0.50", scores[StrikeKind.STRAIGHT]!! >= 0.50f)
    }

    @Test
    fun centerlineCross_135degElbow_classifiedAsStraightNotHook() {
        val det = StrikeDetector()
        val m = StrokeMetrics(
            durationMs = 130, peakSpeed = 5.0f, netDisplacement = 0.55f, pathEfficiency = 0.90f,
            rise = 0.05f, netVertical = 0.0f, lateral = 0.30f, across = 0.30f, forwardZ = 0.40f,
            reachStart = 0.50f, reachGain = 0.35f, reachMax = 0.85f,
            startHeight = -0.35f, apexHeight = -0.40f, elbowAngleAtEnd = 135f
        )
        val scores = det.scoreKinds(m)
        assertTrue(
            "straight (${scores[StrikeKind.STRAIGHT]}) must beat hook (${scores[StrikeKind.HOOK]})",
            scores[StrikeKind.STRAIGHT]!! > scores[StrikeKind.HOOK]!!
        )
    }

    @Test
    fun fastOneTwoCombo_110msApart_bothCounted() {
        val rightCrossExt = ArmPose(0.55f, -0.45f, -1.5f, 170f)
        val left = path(leftGuard, 500L to leftGuard, 130L to leftJabExtended, 170L to leftGuard, 1500L to leftGuard)
        val right = path(rightGuard, 610L to rightGuard, 130L to rightCrossExt, 170L to rightGuard, 1500L to rightGuard)
        val events = run(30, 2400, leftFn = left, rightFn = right)
        assertEquals(events.toString(), 2, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(Arm.RIGHT, events[1].arm)
    }

    @Test
    fun extrapolatedOffscreenKnees_classifiedAsHalfBodyNotFullBody() {
        val scene = Scene(shoulderY = 0.42f * h, swPx = 280f)
        val det = StrikeDetector()
        val base = frame(1_000_000L, scene, leftGuard, rightGuard)
        val withOffscreenKnees = base.joints.toMutableMap().apply {
            put(BlazePoseIndex.LEFT_HIP, Joint(420f, 1100f, 0f, 0.7f))
            put(BlazePoseIndex.RIGHT_HIP, Joint(300f, 1100f, 0f, 0.7f))
            put(BlazePoseIndex.LEFT_KNEE, Joint(420f, h + 150f, 0f, 0.6f))
            put(BlazePoseIndex.RIGHT_KNEE, Joint(300f, h + 150f, 0f, 0.6f))
        }
        val r = det.process(base.copy(joints = withOffscreenKnees))
        assertEquals(FramingMode.HALF_BODY, r.framingMode)
    }

    @Test
    fun frontFacingUppercut_towardCenterlineFromCompactGuard_classifiedAsUppercutNotJab() {
        // Front-facing uppercut: compact dip (-0.25f), rises toward centerline (+across) and camera (-z),
        // with foreshortened elbow (130 deg) — previously mistaken for Left Jab.
        val compactLoad = ArmPose(-0.20f, -0.22f, -0.15f, 85f)
        val frontApex = ArmPose(0.05f, -0.50f, -0.55f, 130f)
        val left = path(leftGuard, 600L to leftGuard, 150L to compactLoad, 150L to frontApex, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2200, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    @Test
    fun frontFacingUppercut_hiddenElbowAndForwardZ_classifiedAsUppercutNotJab() {
        // Front-facing uppercut where elbow is occluded (NaN) and wrist moves forward toward camera.
        val load = ArmPose(-0.18f, 0.05f, -0.15f, Float.NaN)
        val apex = ArmPose(0.02f, -0.40f, -0.60f, Float.NaN)
        val left = path(leftGuard, 600L to leftGuard, 150L to load, 150L to apex, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2200, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    @Test
    fun leftHook_withForwardAndUpwardComponent_classifiedAsHookNotJab() {
        val wide = ArmPose(0.25f, -0.30f, -0.25f, 95f)
        val across = ArmPose(-0.65f, -0.45f, -0.65f, 100f)
        val left = path(leftGuard, 500L to leftGuard, 120L to wide, 160L to across, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2200, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.HOOK, events[0].kind)
    }

    @Test
    fun lateralGuardShift_withoutForwardExtension_notMistakenForLeftJab() {
        // Quick sideways adjustment of the left hand (not moving forward or across) must not trigger Left Jab.
        val shiftedOut = ArmPose(0.10f, -0.35f, -0.20f, 55f)
        val left = path(leftGuard, 500L to leftGuard, 120L to shiftedOut, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2000, leftFn = left)
        assertEquals(events.toString(), 0, events.size)
    }

    @Test
    fun frontFacingUppercut_noDipFromHighGuard_classifiedAsUppercutNotJab() {
        // Front-facing uppercut thrown directly from chin guard (-0.34f) without a deep dip.
        val highApex = ArmPose(-0.05f, -0.68f, -0.55f, 130f)
        val left = path(leftGuard, 600L to leftGuard, 140L to highApex, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2200, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    @Test
    fun frontFacingUppercut_foreshortened142DegElbowAndForwardZ_classifiedAsUppercutNotJab() {
        // Front-facing uppercut where foreshortening makes elbow read ~142-156 deg and z moves toward camera.
        val load = ArmPose(-0.20f, -0.15f, -0.15f, 90f)
        val apex = ArmPose(-0.35f, -0.52f, -0.95f, 142f)
        val left = path(leftGuard, 600L to leftGuard, 150L to load, 150L to apex, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2200, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.UPPERCUT, events[0].kind)
    }

    @Test
    fun leftHook_occludedElbowWithForwardReach_classifiedAsHookNotJab() {
        // Left hook where elbow is occluded (NaN) and forward Z creates reachGain >= 0.25.
        val wide = ArmPose(0.25f, -0.30f, -0.20f, Float.NaN)
        val across = ArmPose(-0.65f, -0.35f, -0.85f, Float.NaN)
        val left = path(leftGuard, 500L to leftGuard, 120L to wide, 150L to across, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2200, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.HOOK, events[0].kind)
    }

    @Test
    fun guardFidget_withMinorDepthJitter_notMistakenForLeftJab() {
        // Small forward depth jitter (0.12 SW) during a guard adjustment must not fire Left Jab.
        val fidget = ArmPose(-0.15f, -0.30f, -0.35f, 125f)
        val left = path(leftGuard, 500L to leftGuard, 100L to fidget, 300L to leftGuard, 800L to leftGuard)
        val events = run(30, 2000, leftFn = left)
        assertEquals(events.toString(), 0, events.size)
    }

    @Test
    fun continuousUppercuts_withoutPause_allCounted() {
        // 4 continuous uppercuts in a row with no pause at the bottom turnaround.
        val bottom = ArmPose(-0.20f, -0.05f, -0.15f, 85f)
        val apex = ArmPose(-0.20f, -0.58f, -0.45f, 115f)
        val left = path(
            leftGuard, 500L to leftGuard, 120L to bottom,
            150L to apex, 150L to bottom,
            150L to apex, 150L to bottom,
            150L to apex, 150L to bottom,
            150L to apex, 200L to leftGuard, 500L to leftGuard
        )
        val events = run(30, 2600, leftFn = left)
        assertEquals(events.toString(), 4, events.size)
        assertTrue(events.all { it.arm == Arm.LEFT && it.kind == StrikeKind.UPPERCUT })
    }

    @Test
    fun continuousRollingUppercuts_speedNeverZero_allCounted() {
        // Circular/rolling continuous uppercuts where the fist traces an oval and never pauses.
        val events = run(30, 2800, leftFn = { t ->
            when {
                t < 500L -> leftGuard
                t < 2300L -> {
                    val phase = ((t - 500L) / 360.0) * (2.0 * Math.PI)
                    val y = (-0.32f + 0.28f * kotlin.math.cos(phase)).toFloat()
                    val z = (-0.30f - 0.20f * kotlin.math.sin(phase)).toFloat()
                    val x = (-0.20f - 0.08f * kotlin.math.sin(phase)).toFloat()
                    ArmPose(x, y, z, 105f)
                }
                else -> leftGuard
            }
        })
        assertTrue("Expected >= 4 rolling uppercuts, got ${events.size}: $events", events.size >= 4)
        assertTrue(events.all { it.arm == Arm.LEFT && it.kind == StrikeKind.UPPERCUT })
    }

    @Test
    fun frontFacingJab_upwardCameraAngleWithExtendedElbow_classifiedAsStraightNotUppercut() {
        // Jab aimed toward a front camera held slightly below eye level: wrist rises ~0.28 SW
        // while extending forward with elbow >= 150 deg.
        val jabApex = ArmPose(-0.20f, -0.63f, -0.95f, 155f)
        val left = path(leftGuard, 500L to leftGuard, 130L to jabApex, 170L to leftGuard, 800L to leftGuard)
        val events = run(30, 1800, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun frontFacingJab_aimedTowardCenterlineWithForeshortenedReach_classifiedAsStraightNotHook() {
        // Jab toward camera center (across ~0.30 SW) with foreshortened reachMax < 0.75 and 140 deg elbow.
        val centerJab = ArmPose(0.05f, -0.35f, -0.80f, 140f)
        val left = path(leftGuard, 500L to leftGuard, 130L to centerJab, 170L to leftGuard, 800L to leftGuard)
        val events = run(30, 1800, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun frontFacingJab_highApexNearTopEdge_staysTrackedAndCounted() {
        val scene = Scene(shoulderY = 0.25f * h, swPx = 280f)
        // High jab briefly clips slightly above y=0 (-0.03 * h).
        val highJab = ArmPose(-0.25f, -1.25f, -0.95f, 158f)
        val left = path(leftGuard, 500L to leftGuard, 140L to highJab, 180L to leftGuard, 700L to leftGuard)
        val events = run(30, 1800, scene = scene, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun frontFacingJab_rapidTenJabBurst_allCounted() {
        val legs = ArrayList<Pair<Long, ArmPose>>()
        legs.add(500L to leftGuard)
        repeat(10) {
            legs.add(130L to leftJabExtended)
            legs.add(160L to leftGuard)
            legs.add(80L to leftGuard)
        }
        legs.add(500L to leftGuard)
        val left = path(leftGuard, *legs.toTypedArray())
        val events = run(30, 4800, leftFn = left)
        assertEquals(events.toString(), 10, events.size)
        assertTrue(events.all { it.arm == Arm.LEFT && it.kind == StrikeKind.STRAIGHT })
    }

    @Test
    fun frontFacingJab_occludedElbowWithCenterlineDrift_classifiedAsStraightNotHook() {
        // Punching straight at the lens often occludes the elbow behind the wrist (NaN)
        // while drifting slightly toward centerline (across ~0.30 SW < forwardZ).
        val occludedCenterJab = ArmPose(0.05f, -0.35f, -0.75f, Float.NaN)
        val left = path(leftGuard, 500L to leftGuard, 130L to occludedCenterJab, 170L to leftGuard, 800L to leftGuard)
        val events = run(30, 1800, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun frontFacingJab_occludedElbowWithSlightUpwardAngle_classifiedAsStraightNotUppercut() {
        // Punching from chin guard toward camera lens with occluded elbow (NaN) and forwardZ > 1.4 * rise.
        val occludedUpJab = ArmPose(-0.20f, -0.60f, -0.85f, Float.NaN)
        val left = path(leftGuard, 500L to leftGuard, 130L to occludedUpJab, 170L to leftGuard, 800L to leftGuard)
        val events = run(30, 1800, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun frontFacingJab_compactForwardDepth_stillCounted() {
        // Compact/fast jab where ML Kit reports only ~0.35 SW forward depth.
        val compactJab = ArmPose(-0.25f, -0.35f, -0.55f, 135f)
        val left = path(leftGuard, 500L to leftGuard, 120L to compactJab, 160L to leftGuard, 800L to leftGuard)
        val events = run(30, 1800, leftFn = left)
        assertEquals(events.toString(), 1, events.size)
        assertEquals(Arm.LEFT, events[0].arm)
        assertEquals(StrikeKind.STRAIGHT, events[0].kind)
    }

    @Test
    fun loweringArmsFromGuard_notMistakenForHook() {
        // Dropping the right guard hand downward and slightly across must not fire a hook.
        val dropped = ArmPose(-0.05f, 0.05f, -0.20f, 110f)
        val right = path(rightGuard, 500L to rightGuard, 150L to dropped, 600L to dropped)
        val events = run(30, 1500, rightFn = right)
        assertEquals(events.toString(), 0, events.size)
    }

    @Test
    fun guardHand_pureZDepthJitter_notMistakenForPunch() {
        // Stationary guard hand with a brief noisy ML Kit depth spike (-0.25 SW) and negligible 2D travel.
        val zSpike = ArmPose(0.22f, -0.32f, -0.45f, 55f)
        val right = path(rightGuard, 500L to rightGuard, 100L to zSpike, 250L to rightGuard, 700L to rightGuard)
        val events = run(30, 1700, rightFn = right)
        assertEquals(events.toString(), 0, events.size)
    }
}
