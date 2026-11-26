package amba.dojo.pulse.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingExerciseTest {

    @Test
    fun precisionCombo_leftSteps_rejectRightHandPunches() {
        val steps = PresetExercises.DRILL_PRECISION_COMBO.steps
        val rightPunches = listOf(PunchType.STRIKE, PunchType.RIGHT_HOOK, PunchType.RIGHT_UPPERCUT)

        for (step in steps) {
            if (step.title.contains("Left", ignoreCase = true)) {
                assertTrue("${step.title} should accept LEFT_JAB", step.matches(PunchType.LEFT_JAB))
                for (right in rightPunches) {
                    assertFalse("${step.title} must reject $right", step.matches(right))
                }
            }
        }
    }

    @Test
    fun hooksBurnout_leftAndRightSteps_matchOnlyRespectiveArm() {
        val steps = PresetExercises.DRILL_HOOKS_BURNOUT.steps
        val leftHookStep = steps.first { it.id == "step_1_left_hook" }
        val rightHookStep = steps.first { it.id == "step_2_right_hook" }

        assertTrue(leftHookStep.matches(PunchType.LEFT_HOOK))
        assertFalse(leftHookStep.matches(PunchType.RIGHT_HOOK))
        assertFalse(leftHookStep.matches(PunchType.STRIKE))

        assertTrue(rightHookStep.matches(PunchType.RIGHT_HOOK))
        assertFalse(rightHookStep.matches(PunchType.LEFT_HOOK))
        assertFalse(rightHookStep.matches(PunchType.LEFT_JAB))
    }
}
