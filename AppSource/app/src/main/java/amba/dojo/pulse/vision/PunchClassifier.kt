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
    /**
     * Arm motion lifecycle state machine.
     * Prevents multi-counting of a single punch and false triggers while holding hand:
     * - READY_IN_GUARD: Arm is in guard/neutral, ready to initiate a punch.
     * - RETRACTING: Punch peak was registered. Arm MUST retract back towards guard
     *   before another punch can be initiated.
     */
    private enum class ArmMotionState {
        READY_IN_GUARD,
        RETRACTING
    }

    private data class WristSnapshot(
        val normX: Float,
        val normY: Float,
        val timestamp: Long
    )

    private var leftArmMotionState = ArmMotionState.READY_IN_GUARD
    private var rightArmMotionState = ArmMotionState.READY_IN_GUARD

    private var leftPeakWristY = 1.0f
    private var rightPeakWristY = 1.0f

    private var lastLeftStrikeTimestamp = 0L
    private var lastRightStrikeTimestamp = 0L
    private var lastGlobalStrikeTimestamp = 0L
    private var strikeDisplayUntil = 0L

    // Sliding window of wrist history (last 8 frames) for reliable motion vector & velocity
    private val leftWristHistory = ArrayDeque<WristSnapshot>(8)
    private val rightWristHistory = ArrayDeque<WristSnapshot>(8)

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

    // Constants
    private val globalCooldownMs = 360L       // Min interval between ANY strikes
    private val sameArmCooldownMs = 460L      // Min interval between strikes with same hand
    private val displayHoldMs = 450L          // Duration to display active strike banner
    private val minConfidenceThreshold = 0.60f

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

        // Guard detection (normalized, resolution independent)
        val isLeftGuard = isHandInGuard(leftWrist, leftShoulder, widthF, heightF)
        val isRightGuard = isHandInGuard(rightWrist, rightShoulder, widthF, heightF)
        val isCurrentlyInGuard = isLeftGuard && isRightGuard

        // Update normalized wrist history buffers
        if (leftWrist != null) {
            val normX = leftWrist.position.x / widthF
            val normY = leftWrist.position.y / heightF
            if (leftWristHistory.size >= 8) leftWristHistory.removeFirst()
            leftWristHistory.addLast(WristSnapshot(normX, normY, currentTime))
        }

        if (rightWrist != null) {
            val normX = rightWrist.position.x / widthF
            val normY = rightWrist.position.y / heightF
            if (rightWristHistory.size >= 8) rightWristHistory.removeFirst()
            rightWristHistory.addLast(WristSnapshot(normX, normY, currentTime))
        }

        // Analyze wrist displacement & velocity over the sliding history window
        val leftMetrics = calculateMotionMetrics(leftWristHistory)
        val rightMetrics = calculateMotionMetrics(rightWristHistory)

        // 1. UPDATE RETRACTION STATE (Re-arming arms for subsequent punches)
        updateArmRetraction(
            isLeft = true,
            currentWrist = leftWrist,
            heightF = heightF,
            elbowAngle = leftElbowAngle,
            isInGuard = isLeftGuard,
            currentTime = currentTime
        )

        updateArmRetraction(
            isLeft = false,
            currentWrist = rightWrist,
            heightF = heightF,
            elbowAngle = rightElbowAngle,
            isInGuard = isRightGuard,
            currentTime = currentTime
        )

        // 2. AI MODEL INFERENCE & PUNCH EVALUATION
        val aiPrediction = aiClassifier?.processPose(pose, imageWidth, imageHeight)
        if (aiPrediction != null) {
            val predictedType = PunchType.fromLabel(aiPrediction.label)
            currentConfidence = aiPrediction.confidence

            if (predictedType != PunchType.GUARD &&
                predictedType != PunchType.IDLE &&
                aiPrediction.confidence >= minConfidenceThreshold
            ) {
                val isLeftStrike = isLeftPunch(predictedType)
                val armMotionState = if (isLeftStrike) leftArmMotionState else rightArmMotionState
                val lastArmStrikeTime = if (isLeftStrike) lastLeftStrikeTimestamp else lastRightStrikeTimestamp
                val metrics = if (isLeftStrike) leftMetrics else rightMetrics
                val elbowAngle = if (isLeftStrike) leftElbowAngle else rightElbowAngle

                val passedGlobalCooldown = (currentTime - lastGlobalStrikeTimestamp) >= globalCooldownMs
                val passedArmCooldown = (currentTime - lastArmStrikeTime) >= sameArmCooldownMs
                val isArmReady = armMotionState == ArmMotionState.READY_IN_GUARD

                // Verify physical kinematics match the claimed punch type
                val matchesKinematics = verifyPunchKinematics(predictedType, metrics, elbowAngle)

                if (isArmReady && passedGlobalCooldown && passedArmCooldown && matchesKinematics) {
                    // VALIDATED SINGLE STRIKE TRIGGER
                    registerValidatedStrike(
                        punch = predictedType,
                        confidence = aiPrediction.confidence,
                        isLeft = isLeftStrike,
                        currentWristNormY = if (isLeftStrike) metrics.latestY else rightMetrics.latestY,
                        currentTime = currentTime
                    )
                }
            }
        } else {
            // Heuristic fallback if AI classifier is unavailable
            evaluateHeuristicStrike(
                leftMetrics = leftMetrics,
                rightMetrics = rightMetrics,
                leftElbowAngle = leftElbowAngle,
                rightElbowAngle = rightElbowAngle,
                currentTime = currentTime
            )
        }

        // 3. UI STATE BANNER MANAGEMENT
        if (currentTime >= strikeDisplayUntil) {
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
     * Verifies that the physical motion vector matches the claimed punch type.
     * Crucially: Uppercuts require actual UPWARD wrist displacement (deltaY > 0)
     * and high velocity, preventing static held hands from triggering.
     */
    private fun verifyPunchKinematics(
        punch: PunchType,
        metrics: MotionMetrics,
        elbowAngle: Double
    ): Boolean {
        return when (punch) {
            PunchType.LEFT_UPPERCUT, PunchType.RIGHT_UPPERCUT -> {
                // Uppercut: wrist MUST move upwards (upwardDeltaY >= 0.042)
                // with bent elbow (elbow between 55 deg and 130 deg) and sufficient velocity.
                // Holding hand stationary has upwardDeltaY ≈ 0, completely rejecting false positives!
                metrics.upwardDeltaY >= 0.042f &&
                        metrics.velocity >= 0.16f &&
                        elbowAngle in 55.0..130.0
            }

            PunchType.LEFT_JAB -> {
                // Jab: lead arm extension outwards, elbow opening to >= 125 deg
                metrics.netDisplacement >= 0.055f &&
                        metrics.velocity >= 0.18f &&
                        elbowAngle >= 125.0
            }

            PunchType.STRIKE -> {
                // Rear Strike / Cross: rear arm full extension
                metrics.netDisplacement >= 0.055f &&
                        metrics.velocity >= 0.18f &&
                        elbowAngle >= 125.0
            }

            PunchType.LEFT_HOOK, PunchType.RIGHT_HOOK -> {
                // Hook: sweeping horizontal motion (|deltaX| >= 0.045) with bent elbow
                abs(metrics.displacementX) >= 0.045f &&
                        metrics.velocity >= 0.16f &&
                        elbowAngle in 65.0..128.0
            }

            else -> false
        }
    }

    /**
     * Registers a confirmed punch exactly once and transitions the arm to RETRACTING.
     * The arm is locked until physical retraction is completed.
     */
    private fun registerValidatedStrike(
        punch: PunchType,
        confidence: Float,
        isLeft: Boolean,
        currentWristNormY: Float,
        currentTime: Long
    ) {
        totalStrikes++
        punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
        currentPunch = punch
        lastDetectedPunch = punch
        lastGlobalStrikeTimestamp = currentTime
        strikeDisplayUntil = currentTime + displayHoldMs

        if (isLeft) {
            leftArmMotionState = ArmMotionState.RETRACTING
            leftPeakWristY = currentWristNormY
            lastLeftStrikeTimestamp = currentTime
        } else {
            rightArmMotionState = ArmMotionState.RETRACTING
            rightPeakWristY = currentWristNormY
            lastRightStrikeTimestamp = currentTime
        }

        val confPercent = (confidence * 100).toInt()
        feedback = "${punch.displayName.uppercase()} ($confPercent% MATCH)"
        formScore = (formScore + 1).coerceAtMost(99)
    }

    /**
     * Monitors arm recovery. Arm only re-arms to READY_IN_GUARD when:
     * - Hand descends back down from uppercut peak (normY >= peakY + 0.04f)
     * - OR hand returns to guard position
     * - OR elbow angle contracts back down (< 110 deg for straight punches)
     */
    private fun updateArmRetraction(
        isLeft: Boolean,
        currentWrist: PoseLandmark?,
        heightF: Float,
        elbowAngle: Double,
        isInGuard: Boolean,
        currentTime: Long
    ) {
        val state = if (isLeft) leftArmMotionState else rightArmMotionState
        if (state != ArmMotionState.RETRACTING) return

        val strikeTime = if (isLeft) lastLeftStrikeTimestamp else lastRightStrikeTimestamp
        // Minimum refractory time before retraction can be considered complete
        if (currentTime - strikeTime < 220L) return

        val currentNormY = (currentWrist?.position?.y ?: 0f) / heightF
        val peakY = if (isLeft) leftPeakWristY else rightPeakWristY

        // Retraction criteria:
        // 1. Hand descended downwards by at least 4% of frame
        val descended = currentNormY >= peakY + 0.045f
        // 2. Hand returned to guard zone
        val backInGuard = isInGuard
        // 3. Elbow contracted back
        val elbowContracted = elbowAngle in 40.0..110.0

        if (descended || backInGuard || elbowContracted) {
            val recoverySec = (currentTime - strikeTime) / 1000.0
            if (recoverySec in 0.12..0.85) {
                lastReturnGuardTime = recoverySec
            }

            if (isLeft) {
                leftArmMotionState = ArmMotionState.READY_IN_GUARD
            } else {
                rightArmMotionState = ArmMotionState.READY_IN_GUARD
            }
        }
    }

    private fun isLeftPunch(punch: PunchType): Boolean {
        return when (punch) {
            PunchType.LEFT_HOOK, PunchType.LEFT_JAB, PunchType.LEFT_UPPERCUT -> true
            else -> false
        }
    }

    private data class MotionMetrics(
        val latestY: Float,
        val displacementX: Float,
        val upwardDeltaY: Float,
        val netDisplacement: Float,
        val velocity: Float
    )

    private fun calculateMotionMetrics(history: ArrayDeque<WristSnapshot>): MotionMetrics {
        if (history.size < 2) {
            val y = history.lastOrNull()?.normY ?: 0f
            return MotionMetrics(y, 0f, 0f, 0f, 0f)
        }

        val oldest = history.first()
        val latest = history.last()

        val dx = latest.normX - oldest.normX
        // In image coordinates, Y=0 is at top. Moving UPWARD means oldest.normY > latest.normY.
        val upwardDy = oldest.normY - latest.normY
        val netDist = sqrt(dx * dx + upwardDy * upwardDy)
        val timeSpanSec = ((latest.timestamp - oldest.timestamp).coerceAtLeast(1L)) / 1000f
        val vel = netDist / timeSpanSec

        return MotionMetrics(
            latestY = latest.normY,
            displacementX = dx,
            upwardDeltaY = upwardDy,
            netDisplacement = netDist,
            velocity = vel
        )
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

        // Wrist in guard is near shoulder/chin height (between above shoulder and slightly below shoulder)
        val isAtGuardY = wristY in (shoulderY - 0.22f)..(shoulderY + 0.18f)
        val isCloseX = abs(wristX - shoulderX) < 0.22f
        return isAtGuardY && isCloseX
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
        leftMetrics: MotionMetrics,
        rightMetrics: MotionMetrics,
        leftElbowAngle: Double,
        rightElbowAngle: Double,
        currentTime: Long
    ) {
        val passedGlobal = (currentTime - lastGlobalStrikeTimestamp) >= globalCooldownMs

        // Check Left Uppercut
        if (passedGlobal &&
            leftArmMotionState == ArmMotionState.READY_IN_GUARD &&
            leftMetrics.upwardDeltaY >= 0.05f &&
            leftMetrics.velocity >= 0.18f &&
            leftElbowAngle in 60.0..125.0
        ) {
            registerValidatedStrike(PunchType.LEFT_UPPERCUT, 0.85f, true, leftMetrics.latestY, currentTime)
            return
        }

        // Check Right Uppercut
        if (passedGlobal &&
            rightArmMotionState == ArmMotionState.READY_IN_GUARD &&
            rightMetrics.upwardDeltaY >= 0.05f &&
            rightMetrics.velocity >= 0.18f &&
            rightElbowAngle in 60.0..125.0
        ) {
            registerValidatedStrike(PunchType.RIGHT_UPPERCUT, 0.85f, false, rightMetrics.latestY, currentTime)
            return
        }

        // Check Left Jab
        if (passedGlobal &&
            leftArmMotionState == ArmMotionState.READY_IN_GUARD &&
            leftMetrics.netDisplacement >= 0.06f &&
            leftMetrics.velocity >= 0.20f &&
            leftElbowAngle >= 135.0
        ) {
            registerValidatedStrike(PunchType.LEFT_JAB, 0.85f, true, leftMetrics.latestY, currentTime)
            return
        }

        // Check Rear Strike
        if (passedGlobal &&
            rightArmMotionState == ArmMotionState.READY_IN_GUARD &&
            rightMetrics.netDisplacement >= 0.06f &&
            rightMetrics.velocity >= 0.20f &&
            rightElbowAngle >= 135.0
        ) {
            registerValidatedStrike(PunchType.STRIKE, 0.85f, false, rightMetrics.latestY, currentTime)
            return
        }
    }

    fun reset() {
        totalStrikes = 0
        formScore = 88
        punchCounts.keys.forEach { punchCounts[it] = 0 }
        currentPunch = PunchType.IDLE
        currentConfidence = 0f
        feedback = "Stance ready. Guard up!"
        leftArmMotionState = ArmMotionState.READY_IN_GUARD
        rightArmMotionState = ArmMotionState.READY_IN_GUARD
        leftWristHistory.clear()
        rightWristHistory.clear()
        lastGlobalStrikeTimestamp = 0L
        lastLeftStrikeTimestamp = 0L
        lastRightStrikeTimestamp = 0L
        strikeDisplayUntil = 0L
        aiClassifier?.reset()
    }
}
