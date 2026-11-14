package amba.dojo.pulse.vision

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class PunchClassifier(
    private val stance: FighterStance = FighterStance.ORTHODOX,
    private val aiClassifier: AIPoseMovementClassifier? = null
) {
    private enum class ArmState {
        GUARD,
        EXTENDING,
        PEAK_STRIKE,
        RETURNING
    }

    private var leftArmState = ArmState.GUARD
    private var rightArmState = ArmState.GUARD

    private var leftPeakTimestamp = 0L
    private var rightPeakTimestamp = 0L

    private var leftPeakAngle = 0.0
    private var rightPeakAngle = 0.0

    private var lastReturnGuardTime = 0.18
    private var totalStrikes = 0
    private var formScore = 88
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
    private var lastAiStrikeTimestamp = 0L

    // History of wrist positions for velocity / trajectory analysis
    private var prevLeftWristY = 0f
    private var prevLeftWristX = 0f
    private var prevRightWristY = 0f
    private var prevRightWristX = 0f
    private var hasPrevWrist = false
    private val strikeDisplayHoldMs = 420L
    private val minStrikeVelocity = 0.022f

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

        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        val currentTime = System.currentTimeMillis()

        var activeAngle = 0.0
        var isCurrentlyInGuard = true

        // Left arm analysis
        if (leftShoulder != null && leftElbow != null && leftWrist != null) {
            val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
            val isLeftGuard = isHandInGuard(leftWrist, leftShoulder, nose)

            val dy = leftWrist.position.y - prevLeftWristY
            val dx = leftWrist.position.x - prevLeftWristX

            analyzeArm(
                isLead = stance == FighterStance.ORTHODOX,
                angle = leftAngle,
                isInGuard = isLeftGuard,
                deltaX = dx,
                deltaY = dy,
                currentTime = currentTime,
                isLeft = true
            )

            if (!isLeftGuard) isCurrentlyInGuard = false
            activeAngle = leftAngle
            prevLeftWristX = leftWrist.position.x
            prevLeftWristY = leftWrist.position.y
        }

        // Right arm analysis
        if (rightShoulder != null && rightElbow != null && rightWrist != null) {
            val rightAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
            val isRightGuard = isHandInGuard(rightWrist, rightShoulder, nose)

            val dy = rightWrist.position.y - prevRightWristY
            val dx = rightWrist.position.x - prevRightWristX

            analyzeArm(
                isLead = stance == FighterStance.SOUTHPAW,
                angle = rightAngle,
                isInGuard = isRightGuard,
                deltaX = dx,
                deltaY = dy,
                currentTime = currentTime,
                isLeft = false
            )

            if (!isRightGuard) isCurrentlyInGuard = false
            if (rightAngle > activeAngle) activeAngle = rightAngle
            prevRightWristX = rightWrist.position.x
            prevRightWristY = rightWrist.position.y
        }

        val widthF = imageWidth.toFloat().coerceAtLeast(1f)
        val heightF = imageHeight.toFloat().coerceAtLeast(1f)

        val leftWristNormX = (leftWrist?.position?.x ?: 0f) / widthF
        val leftWristNormY = (leftWrist?.position?.y ?: 0f) / heightF
        val rightWristNormX = (rightWrist?.position?.x ?: 0f) / widthF
        val rightWristNormY = (rightWrist?.position?.y ?: 0f) / heightF

        var leftWristVelocity = 0f
        var rightWristVelocity = 0f
        if (hasPrevWrist) {
            val dLwX = leftWristNormX - prevLeftWristX
            val dLwY = leftWristNormY - prevLeftWristY
            leftWristVelocity = sqrt(dLwX * dLwX + dLwY * dLwY)

            val dRwX = rightWristNormX - prevRightWristX
            val dRwY = rightWristNormY - prevRightWristY
            rightWristVelocity = sqrt(dRwX * dRwX + dRwY * dRwY)
        }
        prevLeftWristX = leftWristNormX
        prevLeftWristY = leftWristNormY
        prevRightWristX = rightWristNormX
        prevRightWristY = rightWristNormY
        hasPrevWrist = true

        val maxWristVelocity = maxOf(leftWristVelocity, rightWristVelocity)

        // Real-time AI Movement Classifier (Model Inference)
        val aiPrediction = aiClassifier?.processPose(pose, imageWidth, imageHeight)
        if (aiPrediction != null) {
            val predictedType = PunchType.fromLabel(aiPrediction.label)
            currentConfidence = aiPrediction.confidence

            // Active punch requires sufficient wrist velocity to prevent idle false positives
            if (predictedType != PunchType.GUARD && predictedType != PunchType.IDLE && aiPrediction.confidence >= 0.58f && maxWristVelocity >= minStrikeVelocity) {
                if (currentTime - lastAiStrikeTimestamp > 320L || currentPunch != predictedType) {
                    lastAiStrikeTimestamp = currentTime
                    currentPunch = predictedType
                    lastDetectedPunch = predictedType
                    totalStrikes++
                    punchCounts[predictedType] = (punchCounts[predictedType] ?: 0) + 1

                    val confPercent = (aiPrediction.confidence * 100).toInt()
                    feedback = "${predictedType.displayName.uppercase()} ($confPercent% MATCH)"
                    formScore = (formScore + 1).coerceAtMost(99)
                }
            } else if (currentTime - lastAiStrikeTimestamp >= strikeDisplayHoldMs) {
                if (isCurrentlyInGuard) {
                    currentPunch = PunchType.GUARD
                    feedback = "Guard active. Ready to strike!"
                } else {
                    currentPunch = PunchType.IDLE
                    feedback = "Ready stance. Guard up!"
                }
            }
        } else if (currentTime - lastAiStrikeTimestamp >= strikeDisplayHoldMs) {
            if (isCurrentlyInGuard) {
                currentPunch = PunchType.GUARD
            } else {
                currentPunch = PunchType.IDLE
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

    private fun analyzeArm(
        isLead: Boolean,
        angle: Double,
        isInGuard: Boolean,
        deltaX: Float,
        deltaY: Float,
        currentTime: Long,
        isLeft: Boolean
    ) {
        val currentState = if (isLeft) leftArmState else rightArmState

        when (currentState) {
            ArmState.GUARD -> {
                if (!isInGuard && angle > 115.0) {
                    if (isLeft) leftArmState = ArmState.EXTENDING else rightArmState = ArmState.EXTENDING
                }
            }

            ArmState.EXTENDING -> {
                // Heuristic fallback only if AI classifier is not active
                if (aiClassifier == null) {
                    if (angle >= 140.0) {
                        val punch = if (isLead) PunchType.LEFT_JAB else PunchType.STRIKE
                        registerStrike(punch, angle, currentTime, isLeft)
                    } else if (angle in 80.0..125.0 && kotlin.math.abs(deltaX) > 25) {
                        val punch = if (isLead) PunchType.LEFT_HOOK else PunchType.RIGHT_HOOK
                        registerStrike(punch, angle, currentTime, isLeft)
                    } else if (deltaY < -25 && angle in 70.0..115.0) {
                        val punch = if (isLead) PunchType.LEFT_UPPERCUT else PunchType.RIGHT_UPPERCUT
                        registerStrike(punch, angle, currentTime, isLeft)
                    }
                }
            }

            ArmState.PEAK_STRIKE -> {
                if (angle < 130.0 || isInGuard) {
                    if (isLeft) leftArmState = ArmState.RETURNING else rightArmState = ArmState.RETURNING
                }
            }

            ArmState.RETURNING -> {
                if (isInGuard) {
                    val peakTime = if (isLeft) leftPeakTimestamp else rightPeakTimestamp
                    if (peakTime > 0) {
                        val durationMs = currentTime - peakTime
                        if (durationMs in 80..800) {
                            lastReturnGuardTime = durationMs / 1000.0
                            if (lastReturnGuardTime <= 0.23) {
                                feedback = "GUARD EXCELLENT! Fast recovery (${String.format("%.2f", lastReturnGuardTime)}s)"
                                formScore = (formScore + 1).coerceAtMost(98)
                            } else if (lastReturnGuardTime > 0.35) {
                                feedback = "GUARD SLOW! Return to chin faster (Target: <0.23s)"
                                formScore = (formScore - 1).coerceAtLeast(60)
                            }
                        }
                    }
                    if (isLeft) leftArmState = ArmState.GUARD else rightArmState = ArmState.GUARD
                    if (aiClassifier == null) {
                        currentPunch = PunchType.GUARD
                    }
                }
            }
        }
    }

    private fun registerStrike(punch: PunchType, angle: Double, currentTime: Long, isLeft: Boolean) {
        if (isLeft) {
            leftArmState = ArmState.PEAK_STRIKE
            leftPeakTimestamp = currentTime
            leftPeakAngle = angle
        } else {
            rightArmState = ArmState.PEAK_STRIKE
            rightPeakTimestamp = currentTime
            rightPeakAngle = angle
        }

        totalStrikes++
        punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
        currentPunch = punch
        lastDetectedPunch = punch

        if (punch == PunchType.LEFT_JAB || punch == PunchType.STRIKE) {
            if (angle in 140.0..165.0) {
                feedback = "${punch.displayName} EXCELLENT! Perfect extension (${angle.toInt()}°)"
                formScore = (formScore + 1).coerceAtMost(98)
            } else if (angle < 140.0) {
                feedback = "${punch.displayName}: Extend elbow more fully (${angle.toInt()}°)"
                formScore = (formScore - 1).coerceAtLeast(65)
            } else {
                feedback = "${punch.displayName}: Snap back guard quickly!"
            }
        } else {
            feedback = "${punch.displayName} DETECTED! Keep guard tight."
        }
    }

    private fun isHandInGuard(
        wrist: PoseLandmark,
        shoulder: PoseLandmark,
        nose: PoseLandmark?
    ): Boolean {
        val isAtGuardHeight = wrist.position.y <= shoulder.position.y + 120
        val isCloseToShoulderX = kotlin.math.abs(wrist.position.x - shoulder.position.x) < 180
        return isAtGuardHeight && isCloseToShoulderX
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

    fun reset() {
        totalStrikes = 0
        formScore = 88
        punchCounts.keys.forEach { punchCounts[it] = 0 }
        currentPunch = PunchType.IDLE
        currentConfidence = 0f
        feedback = "Stance ready. Guard up!"
        hasPrevWrist = false
        lastAiStrikeTimestamp = 0L
        aiClassifier?.reset()
    }
}
