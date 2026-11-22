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
    // Idle / standing jitter is 0.003 - 0.020.
    // Active punches (Uppercut, Hook, Jab, Strike) produce intentional displacement >= 0.040.
    private val minStrikeDisplacement = 0.040f

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

        // Strict landmark visibility & active camera frame verification.
        // Boxing punches strictly require shoulder, elbow, and wrist to all be visible
        // and safely inside the active camera view (not down in lap / cropped at frame edges).
        val minShoulderVisibility = 0.55f
        val minElbowVisibility = 0.55f
        val minWristVisibility = 0.65f

        val isLeftShoulderValid = leftShoulder != null && leftShoulder.inFrameLikelihood >= minShoulderVisibility
        val isRightShoulderValid = rightShoulder != null && rightShoulder.inFrameLikelihood >= minShoulderVisibility

        val isLeftElbowValid = isLeftShoulderValid && leftElbow != null &&
                leftElbow.inFrameLikelihood >= minElbowVisibility &&
                leftElbow.position.y in (0.05f * heightF)..(0.98f * heightF)

        val isRightElbowValid = isRightShoulderValid && rightElbow != null &&
                rightElbow.inFrameLikelihood >= minElbowVisibility &&
                rightElbow.position.y in (0.05f * heightF)..(0.98f * heightF)

        val isLeftWristValid = isLeftElbowValid && leftWrist != null &&
                leftWrist.inFrameLikelihood >= minWristVisibility &&
                leftWrist.position.x in (0.04f * widthF)..(0.96f * widthF) &&
                leftWrist.position.y in (0.04f * heightF)..(0.90f * heightF)

        val isRightWristValid = isRightElbowValid && rightWrist != null &&
                rightWrist.inFrameLikelihood >= minWristVisibility &&
                rightWrist.position.x in (0.04f * widthF)..(0.96f * widthF) &&
                rightWrist.position.y in (0.04f * heightF)..(0.90f * heightF)

        // Calculate elbow angles
        val leftElbowAngle = if (isLeftElbowValid && isLeftWristValid) {
            calculateAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
        } else 0.0

        val rightElbowAngle = if (isRightElbowValid && isRightWristValid) {
            calculateAngle(rightShoulder!!, rightElbow!!, rightWrist!!)
        } else 0.0

        val activeAngle = maxOf(leftElbowAngle, rightElbowAngle)

        // Guard detection (requires wrist visible near shoulder)
        val isLeftGuard = isLeftWristValid && isHandInGuard(leftWrist, leftShoulder, widthF, heightF)
        val isRightGuard = isRightWristValid && isHandInGuard(rightWrist, rightShoulder, widthF, heightF)
        val isCurrentlyInGuard = isLeftGuard && isRightGuard

        // Track normalized wrist positions (sliding 4-frame window) - only when genuinely visible!
        if (isLeftWristValid) {
            val nx = leftWrist!!.position.x / widthF
            val ny = leftWrist.position.y / heightF
            if (leftWristQueue.size >= 4) leftWristQueue.removeFirst()
            leftWristQueue.addLast(WristPoint(nx, ny))
        } else {
            leftWristQueue.clear()
        }

        if (isRightWristValid) {
            val nx = rightWrist!!.position.x / widthF
            val ny = rightWrist.position.y / heightF
            if (rightWristQueue.size >= 4) rightWristQueue.removeFirst()
            rightWristQueue.addLast(WristPoint(nx, ny))
        } else {
            rightWristQueue.clear()
        }

        val leftDisplacement = calculateQueueDisplacement(leftWristQueue)
        val rightDisplacement = calculateQueueDisplacement(rightWristQueue)

        // --- AI Movement Classifier Execution ---
        if (aiClassifier != null) {
            val aiPrediction = aiClassifier.processPose(pose, imageWidth, imageHeight)
            if (aiPrediction == null) {
                // Sequence buffer is still calibrating: do not trigger any false heuristic strikes!
                currentPunch = PunchType.IDLE
                feedback = if (!isLeftWristValid && !isRightWristValid) {
                    "Show hands & guard to start"
                } else {
                    "Calibrating stance..."
                }
            } else {
                val rawPredictedType = PunchType.fromLabel(aiPrediction.label)
                currentConfidence = aiPrediction.confidence

                var predictedType = rawPredictedType
                val isLeftPunch = predictedType == PunchType.LEFT_JAB ||
                        predictedType == PunchType.LEFT_HOOK ||
                        predictedType == PunchType.LEFT_UPPERCUT
                val isRightPunch = predictedType == PunchType.STRIKE ||
                        predictedType == PunchType.RIGHT_HOOK ||
                        predictedType == PunchType.RIGHT_UPPERCUT

                // Validate that the striking arm is fully visible and in active frame
                val isStrikingArmVisible = when {
                    isLeftPunch -> isLeftWristValid
                    isRightPunch -> isRightWristValid
                    else -> false
                }

                val strikingDisplacement = when {
                    isLeftPunch -> leftDisplacement
                    isRightPunch -> rightDisplacement
                    else -> 0f
                }

                // Strict Kinematic Validation
                val activeQueue = if (isLeftPunch) leftWristQueue else rightWristQueue
                val strikingShoulder = if (isLeftPunch) leftShoulder else rightShoulder
                val strikingElbowAngle = if (isLeftPunch) leftElbowAngle else rightElbowAngle

                var isKinematicallyValid = false
                if (isStrikingArmVisible && activeQueue.size >= 3 && strikingShoulder != null) {
                    val oldest = activeQueue.first()
                    val latest = activeQueue.last()
                    val upwardDy = oldest.y - latest.y // Positive = moved upward in frame
                    val lateralDx = abs(latest.x - oldest.x)
                    val shoulderNormY = strikingShoulder.position.y / heightF

                    // Hook / Uppercut disambiguation
                    if (rawPredictedType == PunchType.LEFT_HOOK) {
                        val upperProb = aiPrediction.allProbabilities["Left Uppercut"] ?: 0f
                        if (upwardDy > 0.025f && upwardDy >= lateralDx * 0.60f && upperProb >= 0.30f) {
                            predictedType = PunchType.LEFT_UPPERCUT
                        }
                    } else if (rawPredictedType == PunchType.RIGHT_HOOK) {
                        val upperProb = aiPrediction.allProbabilities["Right UpperCut"] ?: 0f
                        if (upwardDy > 0.025f && upwardDy >= lateralDx * 0.60f && upperProb >= 0.30f) {
                            predictedType = PunchType.RIGHT_UPPERCUT
                        }
                    }

                    when (predictedType) {
                        PunchType.LEFT_UPPERCUT, PunchType.RIGHT_UPPERCUT -> {
                            // Must have clear vertical elevation (at least 3.5% screen height)
                            val hasUpwardMotion = upwardDy >= 0.035f && upwardDy >= lateralDx * 0.60f
                            // Uppercut apex must reach near shoulder / chin level (not low down at waist)
                            val reachedChestLevel = latest.y <= shoulderNormY + 0.12f
                            isKinematicallyValid = hasUpwardMotion && reachedChestLevel
                        }
                        PunchType.LEFT_JAB, PunchType.STRIKE -> {
                            // Arm extension: elbow angle expands and arm moves forward at chest level
                            val isArmExtended = strikingElbowAngle >= 120.0
                            val atChestLevel = latest.y <= shoulderNormY + 0.20f
                            isKinematicallyValid = isArmExtended && atChestLevel
                        }
                        PunchType.LEFT_HOOK, PunchType.RIGHT_HOOK -> {
                            // Lateral rotation: lateral movement dominates
                            val isLateralMotion = lateralDx >= 0.035f && lateralDx >= upwardDy * 0.80f
                            val atChestLevel = latest.y <= shoulderNormY + 0.20f
                            val isCurvedElbow = strikingElbowAngle in 60.0..145.0
                            isKinematicallyValid = isLateralMotion && atChestLevel && isCurvedElbow
                        }
                        else -> {
                            isKinematicallyValid = false
                        }
                    }
                }

                val isPunch = isStrikingArmVisible &&
                        isKinematicallyValid &&
                        predictedType != PunchType.GUARD &&
                        predictedType != PunchType.IDLE &&
                        aiPrediction.confidence >= minPunchConfidence

                val isActivelyMoving = strikingDisplacement >= minStrikeDisplacement
                val cooldownPassed = (currentTime - lastStrikeTimestamp) >= strikeCooldownMs

                if (isArmedForStrike && isPunch && isActivelyMoving && cooldownPassed) {
                    totalStrikes++
                    punchCounts[predictedType] = (punchCounts[predictedType] ?: 0) + 1
                    currentPunch = predictedType
                    lastDetectedPunch = predictedType
                    lastStrikeTimestamp = currentTime
                    isArmedForStrike = false

                    val confPercent = (aiPrediction.confidence * 100).toInt()
                    feedback = "${predictedType.displayName.uppercase()} ($confPercent% MATCH)"
                    formScore = (formScore + 1).coerceAtMost(99)
                } else if (!isArmedForStrike) {
                    val backToNeutral = predictedType == PunchType.GUARD ||
                            predictedType == PunchType.IDLE ||
                            aiPrediction.confidence < 0.45f ||
                            isCurrentlyInGuard ||
                            strikingDisplacement < (minStrikeDisplacement * 0.7f)

                    if (backToNeutral && (currentTime - lastStrikeTimestamp >= 280L)) {
                        isArmedForStrike = true
                    }
                }
            }
        } else {
            // Heuristic fallback ONLY if AI classifier is not loaded
            evaluateHeuristicStrike(
                leftDisplacement = leftDisplacement,
                rightDisplacement = rightDisplacement,
                leftElbowAngle = leftElbowAngle,
                rightElbowAngle = rightElbowAngle,
                isLeftWristValid = isLeftWristValid,
                isRightWristValid = isRightWristValid,
                leftShoulderNormY = (leftShoulder?.position?.y ?: 0f) / heightF,
                rightShoulderNormY = (rightShoulder?.position?.y ?: 0f) / heightF,
                currentTime = currentTime
            )
        }

        // --- Visual HUD Display Banner Management ---
        if (currentTime - lastStrikeTimestamp >= strikeDisplayHoldMs) {
            if (!isLeftWristValid && !isRightWristValid) {
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
        if (wrist.inFrameLikelihood < 0.65f || shoulder.inFrameLikelihood < 0.55f) return false
        val wristX = wrist.position.x / widthF
        val wristY = wrist.position.y / heightF
        val shoulderX = shoulder.position.x / widthF
        val shoulderY = shoulder.position.y / heightF

        // Guard zone: wrist elevated near shoulder / chin level
        val isNearShoulderY = wristY in (shoulderY - 0.28f)..(shoulderY + 0.12f)
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
        isLeftWristValid: Boolean,
        isRightWristValid: Boolean,
        leftShoulderNormY: Float,
        rightShoulderNormY: Float,
        currentTime: Long
    ) {
        if (!isArmedForStrike || (currentTime - lastStrikeTimestamp < strikeCooldownMs)) {
            if (currentTime - lastStrikeTimestamp >= 300L) {
                isArmedForStrike = true
            }
            return
        }

        if (isLeftWristValid && leftWristQueue.size >= 3 && leftDisplacement >= minStrikeDisplacement) {
            val oldest = leftWristQueue.first()
            val latest = leftWristQueue.last()
            val upwardDy = oldest.y - latest.y
            val lateralDx = abs(latest.x - oldest.x)

            val punch = when {
                upwardDy >= 0.035f && upwardDy >= lateralDx * 0.60f && latest.y <= leftShoulderNormY + 0.12f -> PunchType.LEFT_UPPERCUT
                leftElbowAngle >= 120.0 && latest.y <= leftShoulderNormY + 0.20f -> PunchType.LEFT_JAB
                lateralDx >= 0.035f && latest.y <= leftShoulderNormY + 0.20f && leftElbowAngle in 60.0..145.0 -> PunchType.LEFT_HOOK
                else -> null
            }

            if (punch != null) {
                totalStrikes++
                punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
                currentPunch = punch
                lastDetectedPunch = punch
                lastStrikeTimestamp = currentTime
                isArmedForStrike = false
                feedback = "${punch.displayName.uppercase()} (FORM DETECTED)"
                return
            }
        }

        if (isRightWristValid && rightWristQueue.size >= 3 && rightDisplacement >= minStrikeDisplacement) {
            val oldest = rightWristQueue.first()
            val latest = rightWristQueue.last()
            val upwardDy = oldest.y - latest.y
            val lateralDx = abs(latest.x - oldest.x)

            val punch = when {
                upwardDy >= 0.035f && upwardDy >= lateralDx * 0.60f && latest.y <= rightShoulderNormY + 0.12f -> PunchType.RIGHT_UPPERCUT
                rightElbowAngle >= 120.0 && latest.y <= rightShoulderNormY + 0.20f -> PunchType.STRIKE
                lateralDx >= 0.035f && latest.y <= rightShoulderNormY + 0.20f && rightElbowAngle in 60.0..145.0 -> PunchType.RIGHT_HOOK
                else -> null
            }

            if (punch != null) {
                totalStrikes++
                punchCounts[punch] = (punchCounts[punch] ?: 0) + 1
                currentPunch = punch
                lastDetectedPunch = punch
                lastStrikeTimestamp = currentTime
                isArmedForStrike = false
                feedback = "${punch.displayName.uppercase()} (FORM DETECTED)"
            }
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
