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
}
