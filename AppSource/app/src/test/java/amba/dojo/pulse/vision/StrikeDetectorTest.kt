package amba.dojo.pulse.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StrikeDetectorTest {

    private fun buildFrame(
        tMs: Long,
        leftWristX: Float,
        leftWristY: Float,
        leftWristZ: Float,
        leftElbowX: Float = 260f,
        leftElbowY: Float = 220f,
        leftElbowZ: Float = -20f
    ): PoseFrame {
        val joints = mapOf(
            BlazePoseIndex.LEFT_SHOULDER to Joint(260f, 160f, 0f, 0.99f),
            BlazePoseIndex.RIGHT_SHOULDER to Joint(140f, 160f, 0f, 0.99f),
            BlazePoseIndex.LEFT_ELBOW to Joint(leftElbowX, leftElbowY, leftElbowZ, 0.99f),
            BlazePoseIndex.RIGHT_ELBOW to Joint(140f, 220f, -20f, 0.99f),
            BlazePoseIndex.LEFT_WRIST to Joint(leftWristX, leftWristY, leftWristZ, 0.99f),
            BlazePoseIndex.RIGHT_WRIST to Joint(150f, 170f, -30f, 0.99f),
            BlazePoseIndex.LEFT_HIP to Joint(250f, 360f, 0f, 0.99f),
            BlazePoseIndex.RIGHT_HIP to Joint(150f, 360f, 0f, 0.99f),
            BlazePoseIndex.LEFT_KNEE to Joint(250f, 520f, 0f, 0.99f),
            BlazePoseIndex.RIGHT_KNEE to Joint(150f, 520f, 0f, 0.99f)
        )
        return PoseFrame(timestampMs = tMs, width = 400, height = 700, joints = joints)
    }

    private fun runLeftStroke(
        detector: StrikeDetector,
        ai: AiStrikeProbabilities?,
        startX: Float = 250f,
        startY: Float = 170f,
        startZ: Float = -20f,
        dxPerStep: Float,
        dyPerStep: Float,
        dzPerStep: Float,
        steps: Int = 6,
        dtMs: Long = 33L,
        elbowX: Float = 260f,
        elbowY: Float = 200f,
        elbowZ: Float = -40f
    ): StrikeEvent? {
        var t = 0L
        // Settle track in guard for ~330ms (acquireFrames=3, minTrackAgeMs=150)
        repeat(10) {
            detector.process(buildFrame(t, startX, startY, startZ, elbowX, elbowY, elbowZ), ai)
            t += dtMs
        }
        var event: StrikeEvent? = null
        for (step in 1..steps) {
            val res = detector.process(
                buildFrame(
                    t,
                    startX + dxPerStep * step,
                    startY + dyPerStep * step,
                    startZ + dzPerStep * step,
                    elbowX,
                    elbowY,
                    elbowZ
                ),
                ai
            )
            if (res.event != null) event = res.event
            t += dtMs
        }
        repeat(4) {
            val res = detector.process(
                buildFrame(
                    t,
                    startX + dxPerStep * steps,
                    startY + dyPerStep * steps,
                    startZ + dzPerStep * steps,
                    elbowX,
                    elbowY,
                    elbowZ
                ),
                ai
            )
            if (res.event != null) event = res.event
            t += dtMs
        }
        return event
    }

    @Test
    fun `detects straight jab without AI`() {
        val detector = StrikeDetector()
        val ev = runLeftStroke(
            detector,
            ai = null,
            dxPerStep = -4f,
            dyPerStep = -3f,
            dzPerStep = -22f,
            elbowX = 255f,
            elbowY = 165f,
            elbowZ = -85f
        )
        assertNotNull(ev)
        assertEquals(Arm.LEFT, ev!!.arm)
        assertEquals(StrikeKind.STRAIGHT, ev.kind)
    }

    @Test
    fun `detects true uppercut from chest height to chin`() {
        val detector = StrikeDetector()
        // Starts below shoulder (y=215 => startHeight=+0.46 SW), rises steeply to y=135 (apexHeight=-0.21 SW), elbow ~100 deg
        val ev = runLeftStroke(
            detector,
            ai = null,
            startX = 250f,
            startY = 215f,
            startZ = -20f,
            dxPerStep = -2f,
            dyPerStep = -13f,
            dzPerStep = -12f,
            elbowX = 250f,
            elbowY = 205f,
            elbowZ = -65f
        )
        assertNotNull(ev)
        assertEquals(StrikeKind.UPPERCUT, ev!!.kind)
    }

    @Test
    fun `chin-level jab with slight upward drift is not misclassified as uppercut`() {
        val detector = StrikeDetector()
        // Starts at chin guard (y=120 => startHeight=-0.33 SW), drives forward with slight upward drift
        val ev = runLeftStroke(
            detector,
            ai = null,
            startX = 250f,
            startY = 120f,
            startZ = -20f,
            dxPerStep = -3f,
            dyPerStep = -4f,
            dzPerStep = -22f,
            elbowX = 255f,
            elbowY = 135f,
            elbowZ = -80f
        )
        assertNotNull(ev)
        assertEquals(StrikeKind.STRAIGHT, ev!!.kind)
    }

    @Test
    fun `rejects slow raise and small twitch false positives`() {
        val detector = StrikeDetector()
        // Small twitch (< 0.26 SW net displacement)
        val twitch = runLeftStroke(
            detector,
            ai = null,
            dxPerStep = 0f,
            dyPerStep = -3f,
            dzPerStep = -2f
        )
        assertNull(twitch)

        detector.reset()
        // Slow hand raise (dtMs = 120ms -> speed < minPeakSpeed 2.5 SW/s)
        val slowRaise = runLeftStroke(
            detector,
            ai = null,
            startY = 210f,
            dxPerStep = 0f,
            dyPerStep = -12f,
            dzPerStep = 0f,
            dtMs = 120L
        )
        assertNull(slowRaise)
    }

    @Test
    fun `AI re-ranks plausible kinds but never creates a punch when geometry scores zero`() {
        val detector = StrikeDetector()
        // Shovel-hook trajectory: starts below shoulder, moves across AND up -> both HOOK and UPPERCUT plausible
        val aiHook = AiStrikeProbabilities { arm, kind ->
            if (arm == Arm.LEFT && kind == StrikeKind.HOOK) 0.95f else 0.02f
        }
        val ev = runLeftStroke(
            detector,
            ai = aiHook,
            startY = 200f,
            dxPerStep = -11f,
            dyPerStep = -10f,
            dzPerStep = -4f,
            elbowX = 270f,
            elbowY = 190f,
            elbowZ = -10f
        )
        assertNotNull(ev)
        assertEquals(StrikeKind.HOOK, ev!!.kind)

        // Downward drop (not a punch): even with high AI uppercut probability, AI cannot create a punch
        detector.reset()
        val aiUpper = AiStrikeProbabilities { arm, kind ->
            if (arm == Arm.LEFT && kind == StrikeKind.UPPERCUT) 0.99f else 0.0f
        }
        val drop = runLeftStroke(detector, ai = aiUpper, dxPerStep = -4f, dyPerStep = 14f, dzPerStep = -4f)
        assertNull(drop)
    }
}
