package amba.dojo.pulse.vision

import com.google.mlkit.vision.pose.Pose

/**
 * Two-stage boxing punch pipeline (ML Kit adapter).
 *
 * Stage 1 + 2 live in [StrikeDetector] (pure Kotlin, unit-tested): visibility-gated wrist tracking,
 * ballistic stroke segmentation and geometric classification per docs/PUNCH_DEFINITIONS.md.
 *
 * This class:
 *  - converts ML Kit [Pose] landmarks to a [PoseFrame] using the camera frame timestamp,
 *  - feeds the TFLite sequence model on EVERY frame (so its 16-frame window is a real, contiguous
 *    clip of the motion; previously it was only fed on strike frames, which made its input a
 *    collage of unrelated moments),
 *  - fuses the model as a re-ranker only, and only when the body is in-distribution (hips visible),
 *  - maps arm + kind to the app's [PunchType] according to [FighterStance],
 *  - keeps counts, HUD feedback and return-to-guard timing.
 */
class PunchClassifier(
    private val stance: FighterStance = FighterStance.ORTHODOX,
    private val aiClassifier: AIPoseMovementClassifier? = null,
    private val detector: StrikeDetector = StrikeDetector()
) {
    private val strikeDisplayHoldMs = 500L

    private var totalStrikes = 0
    private var formScore = 88
    private var lastReturnGuardTime = 0.18
    private var pendingGuardReturn: Pair<Arm, Long>? = null
    private var lastStrikeTimestamp = Long.MIN_VALUE / 2

    private val punchCounts = mutableMapOf(
        PunchType.LEFT_HOOK to 0,
        PunchType.JAB to 0,
        PunchType.LEFT_UPPERCUT to 0,
        PunchType.RIGHT_HOOK to 0,
        PunchType.RIGHT_UPPERCUT to 0,
        PunchType.STRIKE to 0
    )

    private var lastDetectedPunch = PunchType.JAB
    private var currentPunch = PunchType.IDLE
    private var currentConfidence = 0f
    private var feedback = "Stance ready. Guard up!"
    private var lastAi: MovementPrediction? = null

    fun processPose(
        pose: Pose,
        imageWidth: Int,
        imageHeight: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): TrainingTelemetry {
        val landmarksMap = HashMap<Int, PosePoint>()
        val joints = HashMap<Int, Joint>()
        pose.allPoseLandmarks.forEach { lm ->
            landmarksMap[lm.landmarkType] = PosePoint(
                x = lm.position.x,
                y = lm.position.y,
                z = lm.position3D.z,
                confidence = lm.inFrameLikelihood
            )
            joints[lm.landmarkType] = Joint(lm.position.x, lm.position.y, lm.position3D.z, lm.inFrameLikelihood)
        }

        // Feed the sequence model every frame; keep its latest output for fusion.
        val ai = aiClassifier?.processPose(pose, imageWidth, imageHeight, timestampMs)
        if (ai != null) lastAi = ai
        val aiProbs = lastAi?.takeIf { it.inDistribution && it.allProbabilities.isNotEmpty() }?.let { pred ->
            AiStrikeProbabilities { arm, kind -> pred.allProbabilities[aiLabelFor(arm, kind)] ?: 0f }
        }

        val result = detector.process(PoseFrame(timestampMs, imageWidth, imageHeight, joints), aiProbs)

        result.event?.let { ev ->
            val punch = toPunchType(ev.arm, ev.kind)
            totalStrikes++
            punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
            currentPunch = punch
            lastDetectedPunch = punch
            currentConfidence = ev.score.coerceIn(0f, 1f)
            lastStrikeTimestamp = timestampMs
            pendingGuardReturn = ev.arm to timestampMs
            feedback = "${punch.displayName.uppercase()} (${(currentConfidence * 100).toInt()}% MATCH)"
            formScore = (formScore + 1).coerceAtMost(99)
        }

        // Return-to-guard time of the last punching hand.
        pendingGuardReturn?.let { (arm, t0) ->
            val back = if (arm == Arm.LEFT) result.leftInGuard else result.rightInGuard
            val elapsed = timestampMs - t0
            if (back && elapsed > 80L) {
                lastReturnGuardTime = elapsed / 1000.0
                pendingGuardReturn = null
            } else if (elapsed > 2000L) {
                pendingGuardReturn = null
            }
        }

        val isCurrentlyInGuard = result.leftInGuard && result.rightInGuard
        val modePrefix = if (result.framingMode == FramingMode.FULL_BODY) "Full Body" else "Half Body"
        if (timestampMs - lastStrikeTimestamp >= strikeDisplayHoldMs) {
            when {
                result.isFramedTooClose -> {
                    currentPunch = PunchType.IDLE
                    feedback = "Step back slightly to start training"
                }
                !result.shouldersVisible -> {
                    currentPunch = PunchType.IDLE
                    feedback = "Show your shoulders to the camera"
                }
                !result.leftTracked && !result.rightTracked -> {
                    currentPunch = PunchType.IDLE
                    feedback = "$modePrefix: Show hands & guard to start"
                }
                isCurrentlyInGuard -> {
                    currentPunch = PunchType.GUARD
                    feedback = "$modePrefix: Guard active. Ready to strike!"
                }
                else -> {
                    currentPunch = PunchType.IDLE
                    feedback = "$modePrefix: Ready stance. Guard up!"
                }
            }
        }

        val angles = listOf(result.leftElbowAngle, result.rightElbowAngle).filter { !it.isNaN() }
        return TrainingTelemetry(
            detectedPunch = currentPunch,
            lastDetectedPunch = lastDetectedPunch,
            punchAngle = (angles.maxOrNull() ?: 0f).toDouble(),
            returnGuardTimeSeconds = lastReturnGuardTime,
            isGuardActive = isCurrentlyInGuard,
            totalStrikes = totalStrikes,
            targetStrikes = 180,
            formScore = formScore,
            strikeCounts = punchCounts.toMap(),
            feedbackMessage = feedback,
            aiConfidence = currentConfidence,
            landmarks = landmarksMap,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            framingMode = result.framingMode,
            leftArmTracked = result.leftTracked,
            rightArmTracked = result.rightTracked
        )
    }

    private fun toPunchType(arm: Arm, kind: StrikeKind): PunchType = when (kind) {
        StrikeKind.UPPERCUT -> if (arm == Arm.LEFT) PunchType.LEFT_UPPERCUT else PunchType.RIGHT_UPPERCUT
        StrikeKind.HOOK -> if (arm == Arm.LEFT) PunchType.LEFT_HOOK else PunchType.RIGHT_HOOK
        // Lead-hand straight = jab, rear-hand straight = cross ("Strike").
        StrikeKind.STRAIGHT -> {
            val isLead = (arm == Arm.LEFT) == (stance == FighterStance.ORTHODOX)
            if (isLead) PunchType.JAB else PunchType.STRIKE
        }
    }

    /** Labels of the bundled TFLite model (recorded orthodox: left = lead). */
    private fun aiLabelFor(arm: Arm, kind: StrikeKind): String = when (kind) {
        StrikeKind.UPPERCUT -> if (arm == Arm.LEFT) "Left Uppercut" else "Right UpperCut"
        StrikeKind.HOOK -> if (arm == Arm.LEFT) "Left Hook" else "Right Hook"
        StrikeKind.STRAIGHT -> if (arm == Arm.LEFT) "Left Jab" else "Strike"
    }

    /** Resets tracking/motion buffers (e.g. on camera switch) without clearing session strike counts. */
    fun resetTrackingState() {
        pendingGuardReturn = null
        lastAi = null
        detector.reset()
        aiClassifier?.reset()
    }

    fun reset() {
        totalStrikes = 0
        formScore = 88
        punchCounts.keys.forEach { punchCounts[it] = 0 }
        currentPunch = PunchType.IDLE
        currentConfidence = 0f
        feedback = "Stance ready. Guard up!"
        lastStrikeTimestamp = Long.MIN_VALUE / 2
        pendingGuardReturn = null
        lastAi = null
        detector.reset()
        aiClassifier?.reset()
    }
}
