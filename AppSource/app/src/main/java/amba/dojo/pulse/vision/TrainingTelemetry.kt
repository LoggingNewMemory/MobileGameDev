package amba.dojo.pulse.vision

enum class PunchType(val id: Int, val displayName: String, val tag: String) {
    JAB(1, "Lead Jab", "PUNCH 1"),
    CROSS(2, "Rear Cross", "PUNCH 2"),
    LEAD_HOOK(3, "Lead Hook", "PUNCH 3"),
    REAR_HOOK(4, "Rear Hook", "PUNCH 4"),
    LEAD_UPPERCUT(5, "Lead Uppercut", "PUNCH 5"),
    REAR_UPPERCUT(6, "Rear Uppercut", "PUNCH 6"),
    GUARD(0, "Guard Active", "DEFENSE"),
    IDLE(-1, "Ready Stance", "NEUTRAL");

    companion object {
        fun fromId(id: Int): PunchType = entries.find { it.id == id } ?: IDLE
    }
}

enum class FighterStance {
    ORTHODOX, // Left hand lead, Right hand rear
    SOUTHPAW  // Right hand lead, Left hand rear
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
        PunchType.JAB to 0,
        PunchType.CROSS to 0,
        PunchType.LEAD_HOOK to 0,
        PunchType.REAR_HOOK to 0,
        PunchType.LEAD_UPPERCUT to 0,
        PunchType.REAR_UPPERCUT to 0
    ),
    val feedbackMessage: String = "Stance ready. Guard up!",
    val landmarks: Map<Int, PosePoint> = emptyMap(),
    val imageWidth: Int = 480,
    val imageHeight: Int = 640
)
