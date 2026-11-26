package amba.dojo.pulse.vision

enum class PunchType(val id: Int, val displayName: String, val tag: String) {
    LEFT_HOOK(1, "Left Hook", "HOOK L"),
    JAB(2, "Jab", "JAB"),
    LEFT_UPPERCUT(3, "Left Uppercut", "UPPER L"),
    RIGHT_HOOK(4, "Right Hook", "HOOK R"),
    RIGHT_UPPERCUT(5, "Right UpperCut", "UPPER R"),
    STRIKE(6, "Strike", "STRIKE"),
    GUARD(0, "Guard Active", "DEFENSE"),
    IDLE(-1, "Ready Stance", "NEUTRAL");

    companion object {
        fun fromId(id: Int): PunchType = entries.find { it.id == id } ?: IDLE

        fun fromLabel(label: String): PunchType = when (label.trim()) {
            "Left Hook" -> LEFT_HOOK
            "Left Jab", "Jab" -> JAB
            "Left Uppercut" -> LEFT_UPPERCUT
            "Right Hook" -> RIGHT_HOOK
            "Right UpperCut", "Right Uppercut" -> RIGHT_UPPERCUT
            "Strike" -> STRIKE
            "Guard", "Guard Active" -> GUARD
            "Idle", "Ready Stance" -> IDLE
            "Guard / Idle" -> GUARD
            else -> IDLE
        }
    }
}

enum class FighterStance {
    ORTHODOX, // Left hand lead, Right hand rear
    SOUTHPAW  // Right hand lead, Left hand rear
}

enum class FramingMode(val label: String) {
    NONE("NO BODY"),
    TOO_CLOSE("STEP BACK"),
    HALF_BODY("HALF BODY"),
    FULL_BODY("FULL BODY")
}

data class PosePoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val confidence: Float = 1f
)

data class TrainingTelemetry(
    val detectedPunch: PunchType = PunchType.IDLE,
    val lastDetectedPunch: PunchType = PunchType.JAB,
    val punchAngle: Double = 0.0,
    val returnGuardTimeSeconds: Double = 0.18,
    val isGuardActive: Boolean = true,
    val totalStrikes: Int = 0,
    val targetStrikes: Int = 180,
    val formScore: Int = 88,
    val strikeCounts: Map<PunchType, Int> = mapOf(
        PunchType.LEFT_HOOK to 0,
        PunchType.JAB to 0,
        PunchType.LEFT_UPPERCUT to 0,
        PunchType.RIGHT_HOOK to 0,
        PunchType.RIGHT_UPPERCUT to 0,
        PunchType.STRIKE to 0
    ),
    val feedbackMessage: String = "Stance ready. Guard up!",
    val aiConfidence: Float = 0f,
    val landmarks: Map<Int, PosePoint> = emptyMap(),
    val imageWidth: Int = 480,
    val imageHeight: Int = 640,
    val framingMode: FramingMode = FramingMode.NONE,
    val leftArmTracked: Boolean = false,
    val rightArmTracked: Boolean = false
)
