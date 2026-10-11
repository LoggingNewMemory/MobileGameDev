package amba.dojo.pulse.vision

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class PunchClassifier(
    private val stance: FighterStance = FighterStance.ORTHODOX
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
        PunchType.JAB to 0,
        PunchType.CROSS to 0,
        PunchType.LEAD_HOOK to 0,
        PunchType.REAR_HOOK to 0,
        PunchType.LEAD_UPPERCUT to 0,
        PunchType.REAR_UPPERCUT to 0
    )

    private var lastDetectedPunch = PunchType.JAB
    private var currentPunch = PunchType.IDLE
    private var feedback = "Stance ready. Guard up!"

    // History of wrist positions for velocity / trajectory analysis
    private var prevLeftWristY = 0f
    private var prevLeftWristX = 0f
    private var prevRightWristY = 0f
    private var prevRightWristX = 0f

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
                    // Moving out of guard towards strike
                    if (isLeft) leftArmState = ArmState.EXTENDING else rightArmState = ArmState.EXTENDING
                }
            }

            ArmState.EXTENDING -> {
                if (angle >= 140.0) {
                    // Straight punch peak (Jab / Cross)
                    val punch = if (isLead) PunchType.JAB else PunchType.CROSS
                    registerStrike(punch, angle, currentTime, isLeft)
                } else if (angle in 80.0..125.0 && kotlin.math.abs(deltaX) > 25) {
                    // Horizontal hook motion
                    val punch = if (isLead) PunchType.LEAD_HOOK else PunchType.REAR_HOOK
                    registerStrike(punch, angle, currentTime, isLeft)
                } else if (deltaY < -25 && angle in 70.0..115.0) {
                    // Upward uppercut motion
                    val punch = if (isLead) PunchType.LEAD_UPPERCUT else PunchType.REAR_UPPERCUT
                    registerStrike(punch, angle, currentTime, isLeft)
                }
            }

            ArmState.PEAK_STRIKE -> {
                // Moving from peak back to guard
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
                            // Coach feedback based on Adnan's gold standard (target <= 0.23s)
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
                    currentPunch = PunchType.GUARD
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

        // Calculate form feedback
        if (punch == PunchType.JAB || punch == PunchType.CROSS) {
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
        val referenceY = nose?.position?.y ?: shoulder.position.y
        // Wrist is near shoulder or head height
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
        feedback = "Stance ready. Guard up!"
    }
}
