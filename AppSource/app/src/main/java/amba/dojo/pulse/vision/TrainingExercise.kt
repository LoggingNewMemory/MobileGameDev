package amba.dojo.pulse.vision

import androidx.compose.ui.graphics.Color

data class ExerciseStep(
    val id: String,
    val title: String,
    val targetCount: Int,
    val targetPunchName: String,
    val instruction: String,
    val badgeColor: Color = Color(0xFFFFB3BA),
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
     * - Do 15 Left Jabs
     * - Do 10 Left Jabs
     * - Do 15 Power Strikes
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
                instruction = "Pukulan vertikal: tekuk siku ~90°, dorong ke atas dari pinggang ke dagu.",
                badgeColor = Color(0xFFFFB3BA),
                matches = { it == PunchType.LEFT_UPPERCUT || it == PunchType.RIGHT_UPPERCUT }
            ),
            ExerciseStep(
                id = "step_2_left_jab",
                title = "Do 15 Left Jabs",
                targetCount = 15,
                targetPunchName = "Left Jabs",
                instruction = "Luruskan lead arm (kiri) langsung ke depan dan tarik cepat ke posisi guard.",
                badgeColor = Color(0xFF64B5F6),
                matches = { it == PunchType.LEFT_JAB }
            ),
            ExerciseStep(
                id = "step_3_rapid_jab",
                title = "Do 10 Left Jabs",
                targetCount = 10,
                targetPunchName = "Rapid Jabs",
                instruction = "Burst speed: 10 pukulan jab cepat berturut-turut dengan guard rapat.",
                badgeColor = Color(0xFF81C784),
                matches = { it == PunchType.LEFT_JAB }
            ),
            ExerciseStep(
                id = "step_4_strikes",
                title = "Do 15 Strikes / Cross",
                targetCount = 15,
                targetPunchName = "Strikes",
                instruction = "Straight power: lepaskan pukulan lurus tangan belakang dengan rotasi torso penuh.",
                badgeColor = Color(0xFFFFB74D),
                matches = { it == PunchType.STRIKE }
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
        description = "Melatih transfer tenaga rotasional melalui hook horizontal dan uppercut beruntun.",
        steps = listOf(
            ExerciseStep(
                id = "step_1_left_hook",
                title = "Do 15 Left Hooks",
                targetCount = 15,
                targetPunchName = "Left Hooks",
                instruction = "Ayunkan lengan kiri horizontal setinggi dagu dengan rotasi pinggul kiri.",
                badgeColor = Color(0xFFFFB3BA),
                matches = { it == PunchType.LEFT_HOOK }
            ),
            ExerciseStep(
                id = "step_2_right_hook",
                title = "Do 15 Right Hooks",
                targetCount = 15,
                targetPunchName = "Right Hooks",
                instruction = "Gunakan rotasi kaki belakang dan lepaskan hook kanan melintang.",
                badgeColor = Color(0xFFBA68C8),
                matches = { it == PunchType.RIGHT_HOOK }
            ),
            ExerciseStep(
                id = "step_3_uppercuts",
                title = "Do 20 Uppercuts",
                targetCount = 20,
                targetPunchName = "Uppercuts",
                instruction = "Drive pukulan vertikal ke atas bergantian kiri dan kanan.",
                badgeColor = Color(0xFF4FC3F7),
                matches = { it == PunchType.LEFT_UPPERCUT || it == PunchType.RIGHT_UPPERCUT }
            )
        )
    )

    val ALL_ROUTINES = listOf(DRILL_PRECISION_COMBO, DRILL_HOOKS_BURNOUT)
}
