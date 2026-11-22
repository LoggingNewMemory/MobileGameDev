package amba.dojo.pulse.vision

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.sqrt

class PunchClassifier(
    private val stance: FighterStance = FighterStance.ORTHODOX,
    private val aiClassifier: AIPoseMovementClassifier? = null
) {
    private data class WristPoint(val x: Float, val y: Float)

    // Sliding window of last 4 frames for calculating 3-frame displacement & velocity
    private val leftWristQueue = ArrayDeque<WristPoint>(4)
    private val rightWristQueue = ArrayDeque<WristPoint>(4)

    /**
     * Strike State Machine:
     * - isArmedForStrike = true: Ready to register a new strike on rising edge.
     * - Once a strike triggers, isArmedForStrike becomes FALSE (locked).
     *   Even if the user holds their hand at the peak or the AI continues to output
     *   the strike label, NO duplicate strike will register.
     * - It ONLY re-arms when the athlete transitions back through Guard / Idle or neutral stance.
     */
    private var isArmedForStrike = true
    private var lastStrikeTimestamp = 0L
    private val strikeCooldownMs = 450L
    private val strikeDisplayHoldMs = 500L
    private val minPunchConfidence = 0.55f

    // Scale-invariant displacement threshold (3 frames)
    // Idle / standing jitter is 0.003 - 0.015.
    // Active punches (Uppercut, Hook, Jab, Strike) are 0.035 - 0.20.
    private val minStrikeDisplacement = 0.024f

    private var totalStrikes = 0
    private var formScore = 88
    private var lastReturnGuardTime = 0.18

    private val punchCounts = mutableMapOf(
        PunchType.LEFT_HOOK to 0,
        PunchType.LEFT_JAB to 0,
        PunchType.LEFT_UPPERCUT to 0,
        PunchType.RIGHT_HOOK to 0,
        PunchType.RIGHT_UPPERCUT to 0,
        PunchType.STRIKE to 0
    )

    private var lastDetectedPunch = PunchType.LEFT_JAB
    private var currentPunch = PunchType.IDLE
    private var currentConfidence = 0f
    private var feedback = "Stance ready. Guard up!"

    fun processPose(pose: Pose, imageWidth: Int, imageHeight: Int): TrainingTelemetry {
        val landmarksMap = mutableMapOf<Int, PosePoint>()

        pose.allPoseLandmarks.forEach { landmark ->
            landmarksMap[landmark.landmarkType] = PosePoint(
                x = landmark.position.x,
                y = landmark.position.y,
                z = landmark.position3D.z,
                confidence = landmark.inFrameLikelihood
            )
        }

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val currentTime = System.currentTimeMillis()
        val widthF = imageWidth.toFloat().coerceAtLeast(1f)
        val heightF = imageHeight.toFloat().coerceAtLeast(1f)

        // Calculate elbow angles
        val leftElbowAngle = if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            calculateAngle(leftShoulder, leftElbow, leftWrist)
        } else 0.0

        val rightElbowAngle = if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            calculateAngle(rightShoulder, rightElbow, rightWrist)
        } else 0.0

        val activeAngle = maxOf(leftElbowAngle, rightElbowAngle)

        // Guard detection
        val isLeftGuard = isHandInGuard(leftWrist, leftShoulder, widthF, heightF)
        val isRightGuard = isHandInGuard(rightWrist, rightShoulder, widthF, heightF)
        val isCurrentlyInGuard = isLeftGuard && isRightGuard

        // Track normalized wrist positions (sliding 4-frame window)
        if (leftWrist != null) {
            val nx = leftWrist.position.x / widthF
            val ny = leftWrist.position.y / heightF
            if (leftWristQueue.size >= 4) leftWristQueue.removeFirst()
            leftWristQueue.addLast(WristPoint(nx, ny))
        }

        if (rightWrist != null) {
            val nx = rightWrist.position.x / widthF
            val ny = rightWrist.position.y / heightF
            if (rightWristQueue.size >= 4) rightWristQueue.removeFirst()
            rightWristQueue.addLast(WristPoint(nx, ny))
        }

        val leftDisplacement = calculateQueueDisplacement(leftWristQueue)
        val rightDisplacement = calculateQueueDisplacement(rightWristQueue)
        val maxDisplacement = maxOf(leftDisplacement, rightDisplacement)

        // --- AI Movement Classifier Execution ---
        val aiPrediction = aiClassifier?.processPose(pose, imageWidth, imageHeight)
        if (aiPrediction != null) {
            val predictedType = PunchType.fromLabel(aiPrediction.label)
            currentConfidence = aiPrediction.confidence

            val isPunch = predictedType != PunchType.GUARD &&
                    predictedType != PunchType.IDLE &&
                    aiPrediction.confidence >= minPunchConfidence

            // Determine active hand motion
            val activeHandDisplacement = when (predictedType) {
                PunchType.LEFT_HOOK, PunchType.LEFT_JAB, PunchType.LEFT_UPPERCUT -> leftDisplacement
                PunchType.RIGHT_HOOK, PunchType.RIGHT_UPPERCUT, PunchType.STRIKE -> rightDisplacement
                else -> maxDisplacement
            }

            // Real punch requires genuine physical movement (distinguishes punch from holding hand)
            val isActivelyMoving = activeHandDisplacement >= minStrikeDisplacement
            val cooldownPassed = (currentTime - lastStrikeTimestamp) >= strikeCooldownMs

            if (isArmedForStrike && isPunch && isActivelyMoving && cooldownPassed) {
                // SINGLE VALIDATED STRIKE EVENT
                totalStrikes++
                punchCounts[predictedType] = (punchCounts[predictedType] ?: 0) + 1
                currentPunch = predictedType
                lastDetectedPunch = predictedType
                lastStrikeTimestamp = currentTime
                isArmedForStrike = false // Lock until returning to guard / idle

                val confPercent = (aiPrediction.confidence * 100).toInt()
                feedback = "${predictedType.displayName.uppercase()} ($confPercent% MATCH)"
                formScore = (formScore + 1).coerceAtMost(99)
            } else if (!isArmedForStrike) {
                // Re-arm when transitioning back to Guard or Idle
                val backToNeutral = predictedType == PunchType.GUARD ||
                        predictedType == PunchType.IDLE ||
                        aiPrediction.confidence < 0.45f ||
                        isCurrentlyInGuard

                if (backToNeutral && (currentTime - lastStrikeTimestamp >= 280L)) {
                    isArmedForStrike = true
                }
            }
        } else {
            // Heuristic fallback if AI classifier is not loaded
            evaluateHeuristicStrike(
                leftDisplacement = leftDisplacement,
                rightDisplacement = rightDisplacement,
                leftElbowAngle = leftElbowAngle,
                rightElbowAngle = rightElbowAngle,
                currentTime = currentTime
            )
        }

        // --- Visual HUD Display Banner Management ---
        if (currentTime - lastStrikeTimestamp >= strikeDisplayHoldMs) {
            if (isCurrentlyInGuard) {
                currentPunch = PunchType.GUARD
                feedback = "Guard active. Ready to strike!"
            } else {
                currentPunch = PunchType.IDLE
                feedback = "Ready stance. Guard up!"
            }
        }

        return TrainingTelemetry(
            detectedPunch = currentPunch,
            lastDetectedPunch = lastDetectedPunch,
            punchAngle = activeAngle,
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
            imageHeight = imageHeight
        )
    }

    /**
     * Calculates total displacement across the last 3 frames in normalized coordinates.
     */
    private fun calculateQueueDisplacement(queue: ArrayDeque<WristPoint>): Float {
        if (queue.size < 2) return 0f
        val oldest = queue.first()
        val latest = queue.last()
        val dx = latest.x - oldest.x
        val dy = latest.y - oldest.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun isHandInGuard(
        wrist: PoseLandmark?,
        shoulder: PoseLandmark?,
        widthF: Float,
        heightF: Float
    ): Boolean {
        if (wrist == null || shoulder == null) return false
        val wristX = wrist.position.x / widthF
        val wristY = wrist.position.y / heightF
        val shoulderX = shoulder.position.x / widthF
        val shoulderY = shoulder.position.y / heightF

        // Guard zone: wrist near shoulder / chest height
        val isNearShoulderY = wristY in (shoulderY - 0.25f)..(shoulderY + 0.25f)
        val isCloseX = abs(wristX - shoulderX) < 0.28f
        return isNearShoulderY && isCloseX
    }

    private fun calculateAngle(
        first: PoseLandmark,
        mid: PoseLandmark,
        last: PoseLandmark
    ): Double {
        val bAx = first.position.x - mid.position.x
        val bAy = first.position.y - mid.position.y
        val bCx = last.position.x - mid.position.x
        val bCy = last.position.y - mid.position.y

        val dot = (bAx * bCx) + (bAy * bCy)
        val magBA = sqrt((bAx * bAx + bAy * bAy).toDouble())
        val magBC = sqrt((bCx * bCx + bCy * bCy).toDouble())

        if (magBA * magBC == 0.0) return 0.0

        val cosAngle = (dot / (magBA * magBC)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle))
    }

    private fun evaluateHeuristicStrike(
        leftDisplacement: Float,
        rightDisplacement: Float,
        leftElbowAngle: Double,
        rightElbowAngle: Double,
        currentTime: Long
    ) {
        if (!isArmedForStrike || (currentTime - lastStrikeTimestamp < strikeCooldownMs)) {
            if (currentTime - lastStrikeTimestamp >= 300L) {
                isArmedForStrike = true
            }
            return
        }

        if (leftDisplacement >= minStrikeDisplacement) {
            val punch = if (leftElbowAngle >= 135.0) PunchType.LEFT_JAB
            else if (leftElbowAngle in 60.0..130.0) PunchType.LEFT_UPPERCUT
            else PunchType.LEFT_HOOK

            totalStrikes++
            punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
            currentPunch = punch
            lastDetectedPunch = punch
            lastStrikeTimestamp = currentTime
            isArmedForStrike = false
            feedback = "${punch.displayName.uppercase()} (FORM DETECTED)"
            return
        }

        if (rightDisplacement >= minStrikeDisplacement) {
            val punch = if (rightElbowAngle >= 135.0) PunchType.STRIKE
            else if (rightElbowAngle in 60.0..130.0) PunchType.RIGHT_UPPERCUT
            else PunchType.RIGHT_HOOK

            totalStrikes++
            punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
            currentPunch = punch
            lastDetectedPunch = punch
            lastStrikeTimestamp = currentTime
            isArmedForStrike = false
            feedback = "${punch.displayName.uppercase()} (FORM DETECTED)"
        }
    }

    fun reset() {
        totalStrikes = 0
        formScore = 88
        punchCounts.keys.forEach { punchCounts[it] = 0 }
        currentPunch = PunchType.IDLE
        currentConfidence = 0f
        feedback = "Stance ready. Guard up!"
        isArmedForStrike = true
        leftWristQueue.clear()
        rightWristQueue.clear()
        lastStrikeTimestamp = 0L
        aiClassifier?.reset()
    }
}
