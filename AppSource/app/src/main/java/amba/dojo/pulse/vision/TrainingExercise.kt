package amba.dojo.pulse.vision

import androidx.compose.ui.graphics.Color

data class ExerciseStep(
    val id: String,
    val title: String,
    val targetCount: Int,
    val targetPunchName: String,
    val instruction: String,
    val badgeColor: Color = Color(0xFF22D3EE),
    val matches: (PunchType) -> Boolean
)

data class TrainingExerciseRoutine(
    val id: String,
    val name: String,
    val code: String,
    val subtitle: String,
    val description: String,
    val steps: List<ExerciseStep>
)

object PresetExercises {
    /**
     * Primary Dummy Exercise Drill requested by user:
     * - Do 20 Uppercuts
     * - Do 15 Jabs
     * - Do 10 Jabs
     * - Do 15 Left Strikes
     */
    val DRILL_PRECISION_COMBO = TrainingExerciseRoutine(
        id = "combo_precision",
        name = "Precision Striking Drill",
        code = "SYS.DRL-01",
        subtitle = "TARGET: UPPERCUTS & JAB COMBO",
        description = "Sequential guided routine: focus on uppercut elevation and rapid jab snap.",
        steps = listOf(
            ExerciseStep(
                id = "step_1_uppercut",
                title = "Do 20 Uppercuts",
                targetCount = 20,
                targetPunchName = "Uppercuts",
                instruction = "Vertical strike: bend elbow ~90°, drive upward from hips to chin.",
                badgeColor = Color(0xFFFFB3BA),
                matches = { it == PunchType.LEFT_UPPERCUT || it == PunchType.RIGHT_UPPERCUT }
            ),
            ExerciseStep(
                id = "step_2_left_jab",
                title = "Do 15 Jabs",
                targetCount = 15,
                targetPunchName = "Jabs",
                instruction = "Snap lead arm straight forward and recover quickly to guard position.",
                badgeColor = Color(0xFF64B5F6),
                matches = { it == PunchType.JAB }
            ),
            ExerciseStep(
                id = "step_3_rapid_jab",
                title = "Do 10 Jabs",
                targetCount = 10,
                targetPunchName = "Rapid Jabs",
                instruction = "Burst speed: 10 rapid jabs in succession with tight guard.",
                badgeColor = Color(0xFF81C784),
                matches = { it == PunchType.JAB }
            ),
            ExerciseStep(
                id = "step_4_strikes",
                title = "Do 15 Left Strikes",
                targetCount = 15,
                targetPunchName = "Left Strikes",
                instruction = "Straight power: drive left hand straight with full torso rotation.",
                badgeColor = Color(0xFFFFB74D),
                matches = { it == PunchType.JAB }
            )
        )
    )

    /**
     * Rotational Hook & Uppercut conditioning drill
     */
    val DRILL_HOOKS_BURNOUT = TrainingExerciseRoutine(
        id = "rotational_burnout",
        name = "Power Hook & Uppercut",
        code = "SYS.DRL-02",
        subtitle = "TARGET: ROTATIONAL MOMENTUM",
        description = "Develop rotational kinetic power transfer via horizontal hooks and rapid uppercuts.",
        steps = listOf(
            ExerciseStep(
                id = "step_1_left_hook",
                title = "Do 15 Left Hooks",
                targetCount = 15,
                targetPunchName = "Left Hooks",
                instruction = "Swing left arm horizontally at chin level with lead hip rotation.",
                badgeColor = Color(0xFFFFB3BA),
                matches = { it == PunchType.LEFT_HOOK }
            ),
            ExerciseStep(
                id = "step_2_right_hook",
                title = "Do 15 Right Hooks",
                targetCount = 15,
                targetPunchName = "Right Hooks",
                instruction = "Drive off rear foot rotation and release right hook across target.",
                badgeColor = Color(0xFFBA68C8),
                matches = { it == PunchType.RIGHT_HOOK }
            ),
            ExerciseStep(
                id = "step_3_uppercuts",
                title = "Do 20 Uppercuts",
                targetCount = 20,
                targetPunchName = "Uppercuts",
                instruction = "Drive vertical upward strikes alternating left and right.",
                badgeColor = Color(0xFF4FC3F7),
                matches = { it == PunchType.LEFT_UPPERCUT || it == PunchType.RIGHT_UPPERCUT }
            )
        )
    )

    val ALL_ROUTINES = listOf(DRILL_PRECISION_COMBO, DRILL_HOOKS_BURNOUT)
}
