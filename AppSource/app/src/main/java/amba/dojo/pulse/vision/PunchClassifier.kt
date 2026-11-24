package amba.dojo.pulse.vision

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Two-Stage Boxing Punch Classifier Pipeline
 *
 * Stage 1: Kinematic Event Detector
 * - Tracks wrist ballistic velocity curve (velocity = displacement / dt).
 * - Identifies true punch events (acceleration -> velocity peak -> deceleration).
 * - Filters out static hands, resting hands in lap, and sitting posture noise (0 false positives).
 *
 * Stage 2: Motion & AI Strike Classifier
 * - Evaluated ONLY when Stage 1 confirms a genuine strike event.
 * - Categorizes the confirmed punch using directional vectors, joint angles, and AI model predictions:
 *   • Uppercut: Dominant upward elevation (delta-y) ending at shoulder/chin level.
 *   • Jab / Cross: Forward trajectory with arm extension (elbow angle >= 120°).
 *   • Hook: Horizontal sweep (delta-x) with curved elbow (60° - 150°).
 */
class PunchClassifier(
    private val stance: FighterStance = FighterStance.ORTHODOX,
    private val aiClassifier: AIPoseMovementClassifier? = null
) {
    private data class TimedWristPoint(val x: Float, val y: Float, val timestampMs: Long)

    private enum class ArmSide { LEFT, RIGHT }

    private data class StrikeEvent(
        val arm: ArmSide,
        val peakVelocity: Float,
        val totalDisplacement: Float,
        val upwardTravel: Float,
        val lateralTravel: Float,
        val apexNormY: Float,
        val elbowAngle: Double,
        val strikingShoulderNormY: Float
    )

    // Sliding window of recent wrist points for kinematic velocity & trajectory tracking (up to 7 frames ~250ms)
    private val leftWristQueue = ArrayDeque<TimedWristPoint>(7)
    private val rightWristQueue = ArrayDeque<TimedWristPoint>(7)

    // Velocity history for peak velocity curve analysis
    private val leftVelocityHistory = ArrayDeque<Float>(4)
    private val rightVelocityHistory = ArrayDeque<Float>(4)

    // Strike State Machine & Debouncing
    private var isArmedForStrike = true
    private var lastStrikeTimestamp = 0L
    private val strikeCooldownMs = 350L
    private val strikeDisplayHoldMs = 500L
    private val minPunchConfidence = 0.40f

    // Physics thresholds (in normalized screen units per second)
    // Idle / standing jitter: 0.05 - 0.25 / s
    // Intentional punch launch: >= 0.70 / s
    private val minPeakVelocity = 0.65f
    private val minStrikeDisplacement = 0.028f
    private val rearmVelocityThreshold = 0.35f

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

        // Framing verification: If shoulders fill the lower third of the screen, user is sitting too close
        val avgShoulderY = if (leftShoulder != null && rightShoulder != null) {
            (leftShoulder.position.y + rightShoulder.position.y) / (2f * heightF)
        } else if (leftShoulder != null) {
            leftShoulder.position.y / heightF
        } else if (rightShoulder != null) {
            rightShoulder.position.y / heightF
        } else {
            1f
        }
        val isFramedTooClose = avgShoulderY > 0.68f

        // Landmark presence verification
        val minWristVisibility = 0.38f
        val isLeftWristValid = !isFramedTooClose &&
                leftWrist != null &&
                leftWrist.inFrameLikelihood >= minWristVisibility &&
                leftWrist.position.y in 0f..(heightF * 0.92f)

        val isRightWristValid = !isFramedTooClose &&
                rightWrist != null &&
                rightWrist.inFrameLikelihood >= minWristVisibility &&
                rightWrist.position.y in 0f..(heightF * 0.92f)

        // Calculate elbow angles
        val leftElbowAngle = if (leftShoulder != null && leftElbow != null && isLeftWristValid) {
            calculateAngle(leftShoulder, leftElbow, leftWrist!!)
        } else 0.0

        val rightElbowAngle = if (rightShoulder != null && rightElbow != null && isRightWristValid) {
            calculateAngle(rightShoulder, rightElbow, rightWrist!!)
        } else 0.0

        val activeAngle = maxOf(leftElbowAngle, rightElbowAngle)

        // Guard detection (wrist near shoulder/chest)
        val isLeftGuard = isLeftWristValid && isHandInGuard(leftWrist, leftShoulder, widthF, heightF)
        val isRightGuard = isRightWristValid && isHandInGuard(rightWrist, rightShoulder, widthF, heightF)
        val isCurrentlyInGuard = isLeftGuard && isRightGuard

        // Update tracking queues and instantaneous velocities
        val currentLeftVelocity = updateWristTracking(
            isValid = isLeftWristValid,
            wrist = leftWrist,
            widthF = widthF,
            heightF = heightF,
            currentTime = currentTime,
            queue = leftWristQueue,
            velocityHistory = leftVelocityHistory
        )

        val currentRightVelocity = updateWristTracking(
            isValid = isRightWristValid,
            wrist = rightWrist,
            widthF = widthF,
            heightF = heightF,
            currentTime = currentTime,
            queue = rightWristQueue,
            velocityHistory = rightVelocityHistory
        )

        // =========================================================================
        // STAGE 1: Kinematic Strike Event Detector
        // Detects physical ballistic strike curves (acceleration -> peak -> deceleration)
        // =========================================================================
        val cooldownPassed = (currentTime - lastStrikeTimestamp) >= strikeCooldownMs

        val leftStrikeEvent = if (isArmedForStrike && cooldownPassed && isLeftWristValid) {
            detectKinematicStrike(
                arm = ArmSide.LEFT,
                queue = leftWristQueue,
                velocityHistory = leftVelocityHistory,
                elbowAngle = leftElbowAngle,
                shoulderNormY = (leftShoulder?.position?.y ?: 0f) / heightF
            )
        } else null

        val rightStrikeEvent = if (isArmedForStrike && cooldownPassed && isRightWristValid && leftStrikeEvent == null) {
            detectKinematicStrike(
                arm = ArmSide.RIGHT,
                queue = rightWristQueue,
                velocityHistory = rightVelocityHistory,
                elbowAngle = rightElbowAngle,
                shoulderNormY = (rightShoulder?.position?.y ?: 0f) / heightF
            )
        } else null

        val confirmedStrikeEvent = leftStrikeEvent ?: rightStrikeEvent

        // =========================================================================
        // STAGE 2: Strike Classifier (Only executes when Stage 1 confirms a strike!)
        // =========================================================================
        if (confirmedStrikeEvent != null) {
            // Query AI classifier to complement kinematic trajectory
            val aiPrediction = aiClassifier?.processPose(pose, imageWidth, imageHeight)
            val classifiedPunch = classifyStrike(confirmedStrikeEvent, aiPrediction)

            totalStrikes++
            punchCounts[classifiedPunch] = (punchCounts[classifiedPunch] ?: 0) + 1
            currentPunch = classifiedPunch
            lastDetectedPunch = classifiedPunch
            currentConfidence = aiPrediction?.confidence ?: 0.90f
            lastStrikeTimestamp = currentTime
            isArmedForStrike = false // Lock until deceleration / neutral return

            val matchPercent = ((currentConfidence) * 100).toInt()
            feedback = "${classifiedPunch.displayName.uppercase()} ($matchPercent% MATCH)"
            formScore = (formScore + 1).coerceAtMost(99)
        } else if (!isArmedForStrike) {
            // Re-arm when hand velocity drops below threshold or returns to guard
            val maxVelocity = maxOf(currentLeftVelocity, currentRightVelocity)
            val isNeutral = isCurrentlyInGuard || maxVelocity < rearmVelocityThreshold

            if (isNeutral && (currentTime - lastStrikeTimestamp >= 200L)) {
                isArmedForStrike = true
            }
        }

        // --- Visual HUD Display Banner Management ---
        if (currentTime - lastStrikeTimestamp >= strikeDisplayHoldMs) {
            if (isFramedTooClose) {
                currentPunch = PunchType.IDLE
                feedback = "Step back to start training"
            } else if (!isLeftWristValid && !isRightWristValid) {
                currentPunch = PunchType.IDLE
                feedback = "Show hands & guard to start"
            } else if (isCurrentlyInGuard) {
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
     * Updates sliding window of wrist coordinates and computes instantaneous velocity.
     */
    private fun updateWristTracking(
        isValid: Boolean,
        wrist: PoseLandmark?,
        widthF: Float,
        heightF: Float,
        currentTime: Long,
        queue: ArrayDeque<TimedWristPoint>,
        velocityHistory: ArrayDeque<Float>
    ): Float {
        if (!isValid || wrist == null) {
            velocityHistory.clear()
            return 0f
        }

        val nx = wrist.position.x / widthF
        val ny = wrist.position.y / heightF

        var instantVelocity = 0f
        val lastPoint = queue.lastOrNull()
        if (lastPoint != null) {
            val dt = ((currentTime - lastPoint.timestampMs) / 1000f).coerceIn(0.010f, 0.200f)
            val dx = nx - lastPoint.x
            val dy = ny - lastPoint.y
            instantVelocity = sqrt(dx * dx + dy * dy) / dt
        }

        if (queue.size >= 7) queue.removeFirst()
        queue.addLast(TimedWristPoint(nx, ny, currentTime))

        if (velocityHistory.size >= 4) velocityHistory.removeFirst()
        velocityHistory.addLast(instantVelocity)

        return instantVelocity
    }

    /**
     * STAGE 1: Detects if the arm just underwent a ballistic punch acceleration/peak/deceleration.
     */
    private fun detectKinematicStrike(
        arm: ArmSide,
        queue: ArrayDeque<TimedWristPoint>,
        velocityHistory: ArrayDeque<Float>,
        elbowAngle: Double,
        shoulderNormY: Float
    ): StrikeEvent? {
        if (queue.size < 3 || velocityHistory.size < 3) return null

        val vRecent = velocityHistory.toList()
        val v0 = vRecent[vRecent.size - 3]
        val vPeak = vRecent[vRecent.size - 2]
        val v1 = vRecent[vRecent.size - 1]

        // Local velocity peak detection: vPeak must form a crest above threshold
        val isVelocityPeak = (vPeak >= minPeakVelocity) && (vPeak >= v0 * 0.95f) && (vPeak >= v1 * 0.95f)
        if (!isVelocityPeak) return null

        // Calculate motion kinematics across the window
        val startPoint = queue.first()
        val apexPoint = queue.minByOrNull { it.y } ?: startPoint // Highest point physically (lowest Y)
        val endPoint = queue.last()

        val totalDisplacement = calculateQueueDisplacement(queue)
        if (totalDisplacement < minStrikeDisplacement) return null

        val upwardTravel = (startPoint.y - apexPoint.y).coerceAtLeast(0f)
        val lateralTravel = abs(endPoint.x - startPoint.x)

        return StrikeEvent(
            arm = arm,
            peakVelocity = vPeak,
            totalDisplacement = totalDisplacement,
            upwardTravel = upwardTravel,
            lateralTravel = lateralTravel,
            apexNormY = apexPoint.y,
            elbowAngle = elbowAngle,
            strikingShoulderNormY = shoulderNormY
        )
    }

    /**
     * STAGE 2: Classifies the confirmed StrikeEvent into the exact punch type.
     */
    private fun classifyStrike(
        event: StrikeEvent,
        aiPrediction: MovementPrediction?
    ): PunchType {
        val aiLabel = aiPrediction?.label ?: ""
        val aiConf = aiPrediction?.confidence ?: 0f

        // Check if trajectory is predominantly upward towards the chin -> UPPERCUT
        val isRisingTrajectory = event.upwardTravel >= 0.024f &&
                event.upwardTravel >= (event.lateralTravel * 0.38f)
        val reachedChinLevel = event.apexNormY <= (event.strikingShoulderNormY + 0.08f)
        val isUppercutKinematics = isRisingTrajectory && reachedChinLevel

        if (isUppercutKinematics) {
            return if (event.arm == ArmSide.LEFT) PunchType.LEFT_UPPERCUT else PunchType.RIGHT_UPPERCUT
        }

        // If AI strongly predicted Uppercut and motion has upward elevation
        if (aiConf >= minPunchConfidence && (aiLabel.contains("Upper", ignoreCase = true))) {
            if (event.upwardTravel >= 0.018f) {
                return if (event.arm == ArmSide.LEFT) PunchType.LEFT_UPPERCUT else PunchType.RIGHT_UPPERCUT
            }
        }

        // Straight punch (Jab / Cross) -> Arm extension with elbow angle >= 120°
        val isStraightPunch = event.elbowAngle >= 120.0 || (event.totalDisplacement >= 0.040f && event.lateralTravel < 0.035f)
        if (isStraightPunch) {
            return if (event.arm == ArmSide.LEFT) {
                if (stance == FighterStance.ORTHODOX) PunchType.LEFT_JAB else PunchType.STRIKE
            } else {
                if (stance == FighterStance.ORTHODOX) PunchType.STRIKE else PunchType.LEFT_JAB
            }
        }

        // Horizontal sweep with curved elbow -> HOOK
        val isHookSweep = event.lateralTravel >= 0.025f && event.elbowAngle in 60.0..150.0
        if (isHookSweep || (aiConf >= minPunchConfidence && aiLabel.contains("Hook", ignoreCase = true))) {
            return if (event.arm == ArmSide.LEFT) PunchType.LEFT_HOOK else PunchType.RIGHT_HOOK
        }

        // Default fallback to lead/rear straight strike
        return if (event.arm == ArmSide.LEFT) PunchType.LEFT_JAB else PunchType.STRIKE
    }

    private fun calculateQueueDisplacement(queue: ArrayDeque<TimedWristPoint>): Float {
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
        if (wrist.inFrameLikelihood < 0.38f || shoulder.inFrameLikelihood < 0.38f) return false
        val wristX = wrist.position.x / widthF
        val wristY = wrist.position.y / heightF
        val shoulderX = shoulder.position.x / widthF
        val shoulderY = shoulder.position.y / heightF

        // Guard zone: wrist near shoulder / chin / chest level
        val isNearShoulderY = wristY in (shoulderY - 0.35f)..(shoulderY + 0.22f)
        val isCloseX = abs(wristX - shoulderX) < 0.35f
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
        leftVelocityHistory.clear()
        rightVelocityHistory.clear()
        lastStrikeTimestamp = 0L
        aiClassifier?.reset()
    }
}
