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
        dxPerStep: Float,
        dyPerStep: Float,
        dzPerStep: Float,
        elbowX: Float = 260f,
        elbowY: Float = 200f,
        elbowZ: Float = -40f
    ): StrikeEvent? {
        var t = 0L
        // Settle track in guard for 300ms (acquireFrames=3, minTrackAgeMs=150)
        repeat(10) {
            detector.process(buildFrame(t, 250f, 170f, -20f, elbowX, elbowY, elbowZ), ai)
            t += 33L
        }
        // Ballistic extension
        var event: StrikeEvent? = null
        for (step in 1..6) {
            val res = detector.process(
                buildFrame(
                    t,
                    250f + dxPerStep * step,
                    170f + dyPerStep * step,
                    -20f + dzPerStep * step,
                    elbowX,
                    elbowY,
                    elbowZ
                ),
                ai
            )
            if (res.event != null) event = res.event
            t += 33L
        }
        // Decelerate at apex so stroke ends
        repeat(4) {
            val res = detector.process(
                buildFrame(
                    t,
                    250f + dxPerStep * 6,
                    170f + dyPerStep * 6,
                    -20f + dzPerStep * 6,
                    elbowX,
                    elbowY,
                    elbowZ
                ),
                ai
            )
            if (res.event != null) event = res.event
            t += 33L
        }
        return event
    }

    @Test
    fun `detects straight jab without AI`() {
        val detector = StrikeDetector()
        val ev = runLeftStroke(
            detector,
            ai = null,
            dxPerStep = 0f,
            dyPerStep = -2f,
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
    fun `AI re-ranks kinematically plausible kinds`() {
        val detector = StrikeDetector()
        // Diagonal upward + across motion where both HOOK and UPPERCUT can be geometrically plausible
        val aiHook = AiStrikeProbabilities { arm, kind ->
            if (arm == Arm.LEFT && kind == StrikeKind.HOOK) 0.95f else 0.02f
        }
        val ev = runLeftStroke(
            detector,
            ai = aiHook,
            dxPerStep = -14f,
            dyPerStep = -10f,
            dzPerStep = -6f,
            elbowX = 270f,
            elbowY = 170f,
            elbowZ = -10f
        )
        assertNotNull(ev)
        assertEquals(StrikeKind.HOOK, ev!!.kind)
    }

    @Test
    fun `AI fallback rescues ballistic stroke when geometric rules score zero`() {
        val detector = StrikeDetector()
        // Downward-diagonal ballistic motion (netV < -0.30 so geo scores 0 for straight/hook/uppercut)
        val noEvent = runLeftStroke(detector, ai = null, dxPerStep = -6f, dyPerStep = 14f, dzPerStep = -8f)
        assertNull(noEvent)

        detector.reset()
        val highAi = AiStrikeProbabilities { arm, kind ->
            if (arm == Arm.LEFT && kind == StrikeKind.UPPERCUT) 0.85f else 0.05f
        }
        val rescued = runLeftStroke(detector, ai = highAi, dxPerStep = -6f, dyPerStep = 14f, dzPerStep = -8f)
        assertNotNull(rescued)
        assertEquals(StrikeKind.UPPERCUT, rescued!!.kind)
    }
}
