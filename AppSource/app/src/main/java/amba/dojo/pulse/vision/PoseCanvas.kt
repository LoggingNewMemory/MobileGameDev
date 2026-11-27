package amba.dojo.pulse.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseCanvas(
    telemetry: TrainingTelemetry,
    isFrontCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val landmarks = telemetry.landmarks
        if (landmarks.isEmpty()) return@Canvas

        val srcWidth = telemetry.imageWidth.toFloat().coerceAtLeast(1f)
        val srcHeight = telemetry.imageHeight.toFloat().coerceAtLeast(1f)

        // Match PreviewView.ScaleType.FILL_CENTER: uniform scale to cover canvas, centered.
        val scale = maxOf(size.width / srcWidth, size.height / srcHeight)
        val offsetX = (size.width - srcWidth * scale) / 2f
        val offsetY = (size.height - srcHeight * scale) / 2f

        fun mapPoint(point: PosePoint): Offset {
            val rawX = if (isFrontCamera) (srcWidth - point.x) else point.x
            return Offset(rawX * scale + offsetX, point.y * scale + offsetY)
        }

        fun drawBone(startType: Int, endType: Int, color: Color, strokeWidth: Float = 6f) {
            val start = landmarks[startType]
            val end = landmarks[endType]
            if (start != null && end != null && start.confidence > 0.4f && end.confidence > 0.4f) {
                drawLine(
                    color = color,
                    start = mapPoint(start),
                    end = mapPoint(end),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        fun drawJoint(type: Int, color: Color, radius: Float = 8f) {
            val p = landmarks[type]
            if (p != null && p.confidence > 0.4f) {
                drawCircle(
                    color = color,
                    center = mapPoint(p),
                    radius = radius
                )
            }
        }

        val boneColor = Color(0xCC22D3EE)
        val torsoColor = Color(0x880891B2)
        val accentColor = Color(0xFF00E5FF)
        val jointColor = Color(0xFFF1F5F9)

        // Shoulders & Torso
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, torsoColor, 8f)
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, torsoColor, 6f)
        drawBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, torsoColor, 6f)
        drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, torsoColor, 6f)

        // Arms (Striking limbs)
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, boneColor, 8f)
        drawBone(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, accentColor, 8f)

        drawBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, boneColor, 8f)
        drawBone(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, accentColor, 8f)

        // Legs
        drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, torsoColor, 6f)
        drawBone(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, torsoColor, 6f)
        drawBone(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, torsoColor, 6f)
        drawBone(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, torsoColor, 6f)

        // Draw Joints
        val keyJoints = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP
        )

        keyJoints.forEach { jointType ->
            drawJoint(jointType, jointColor, 6f)
        }

        val leftWristColor = if (telemetry.leftArmTracked) Color(0xFF4CAF50) else Color(0x889E9E9E)
        val rightWristColor = if (telemetry.rightArmTracked) Color(0xFF4CAF50) else Color(0x889E9E9E)

        // Highlight active wrists with larger glowing circles
        drawJoint(PoseLandmark.LEFT_WRIST, leftWristColor, 12f)
        drawJoint(PoseLandmark.RIGHT_WRIST, rightWristColor, 12f)
    }
}
