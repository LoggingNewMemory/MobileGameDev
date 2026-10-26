package amba.dojo.pulse.vision

import android.content.Context
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.acos
import kotlin.math.sqrt

data class MovementPrediction(
    val label: String,
    val confidence: Float,
    val allProbabilities: Map<String, Float>
)

class AIPoseMovementClassifier(
    private val context: Context,
    private val modelFileName: String = "boxing_classifier_quantized.tflite",
    private val labelsFileName: String = "boxing_classifier_labels.txt"
) {
    companion object {
        const val WINDOW_SIZE = 16
        const val FEATURE_DIM = 62

        private val KEY_LANDMARKS = intArrayOf(
            PoseLandmark.NOSE,            // 0
            PoseLandmark.LEFT_SHOULDER,   // 11
            PoseLandmark.RIGHT_SHOULDER,  // 12
            PoseLandmark.LEFT_ELBOW,      // 13
            PoseLandmark.RIGHT_ELBOW,     // 14
            PoseLandmark.LEFT_WRIST,      // 15
            PoseLandmark.RIGHT_WRIST,     // 16
            PoseLandmark.LEFT_HIP,        // 23
            PoseLandmark.RIGHT_HIP,       // 24
            PoseLandmark.LEFT_KNEE,       // 25
            PoseLandmark.RIGHT_KNEE,      // 26
            PoseLandmark.LEFT_ANKLE,      // 27
            PoseLandmark.RIGHT_ANKLE      // 28
        )
    }

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    private val sequenceBuffer = ArrayDeque<FloatArray>(WINDOW_SIZE)

    init {
        try {
            loadLabels()
            loadModel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        labels.clear()
        context.assets.open(labelsFileName).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    labels.add(trimmed)
                }
            }
        }
    }

    private fun loadModel() {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(modelByteBuffer, options)
    }

    fun processPose(pose: Pose, imageWidth: Int = 0, imageHeight: Int = 0): MovementPrediction? {
        val features = extractFeatures(pose, imageWidth, imageHeight)
        if (sequenceBuffer.size >= WINDOW_SIZE) {
            sequenceBuffer.removeFirst()
        }
        sequenceBuffer.addLast(features)

        if (sequenceBuffer.size < WINDOW_SIZE || interpreter == null || labels.isEmpty()) {
            return null
        }

        val inputBuffer = ByteBuffer.allocateDirect(1 * WINDOW_SIZE * FEATURE_DIM * 4).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        for (frameFeatures in sequenceBuffer) {
            for (value in frameFeatures) {
                inputBuffer.putFloat(value)
            }
        }

        val outputArray = Array(1) { FloatArray(labels.size) }
        interpreter?.run(inputBuffer, outputArray)

        val probabilities = outputArray[0]
        var maxIndex = 0
        var maxConf = probabilities[0]

        val probMap = mutableMapOf<String, Float>()
        for (i in probabilities.indices) {
            probMap[labels[i]] = probabilities[i]
            if (probabilities[i] > maxConf) {
                maxConf = probabilities[i]
                maxIndex = i
            }
        }

        return MovementPrediction(
            label = labels[maxIndex],
            confidence = maxConf,
            allProbabilities = probMap
        )
    }

    private fun extractFeatures(pose: Pose, imageWidth: Int, imageHeight: Int): FloatArray {
        val features = FloatArray(FEATURE_DIM)

        val widthF = if (imageWidth > 0) imageWidth.toFloat() else 1f
        val heightF = if (imageHeight > 0) imageHeight.toFloat() else 1f

        // Convert 33 landmarks to normalized [0, 1] coordinate space matching MediaPipe Python
        val pts = Array(33) { FloatArray(3) }
        for (idx in 0 until 33) {
            val lm = pose.getPoseLandmark(idx)
            if (lm != null) {
                pts[idx][0] = lm.position.x / widthF
                pts[idx][1] = lm.position.y / heightF
                pts[idx][2] = lm.position3D.z / widthF
            }
        }

        val hipMidX = (pts[PoseLandmark.LEFT_HIP][0] + pts[PoseLandmark.RIGHT_HIP][0]) / 2f
        val hipMidY = (pts[PoseLandmark.LEFT_HIP][1] + pts[PoseLandmark.RIGHT_HIP][1]) / 2f
        val hipMidZ = (pts[PoseLandmark.LEFT_HIP][2] + pts[PoseLandmark.RIGHT_HIP][2]) / 2f

        val shoMidX = (pts[PoseLandmark.LEFT_SHOULDER][0] + pts[PoseLandmark.RIGHT_SHOULDER][0]) / 2f
        val shoMidY = (pts[PoseLandmark.LEFT_SHOULDER][1] + pts[PoseLandmark.RIGHT_SHOULDER][1]) / 2f
        val shoMidZ = (pts[PoseLandmark.LEFT_SHOULDER][2] + pts[PoseLandmark.RIGHT_SHOULDER][2]) / 2f

        val centerX = (hipMidX + shoMidX) / 2f
        val centerY = (hipMidY + shoMidY) / 2f
        val centerZ = (hipMidZ + shoMidZ) / 2f

        val torsoScale = sqrt(
            (shoMidX - hipMidX) * (shoMidX - hipMidX) +
            (shoMidY - hipMidY) * (shoMidY - hipMidY) +
            (shoMidZ - hipMidZ) * (shoMidZ - hipMidZ)
        )

        val shoulderWidth = sqrt(
            (pts[PoseLandmark.LEFT_SHOULDER][0] - pts[PoseLandmark.RIGHT_SHOULDER][0]) * (pts[PoseLandmark.LEFT_SHOULDER][0] - pts[PoseLandmark.RIGHT_SHOULDER][0]) +
            (pts[PoseLandmark.LEFT_SHOULDER][1] - pts[PoseLandmark.RIGHT_SHOULDER][1]) * (pts[PoseLandmark.LEFT_SHOULDER][1] - pts[PoseLandmark.RIGHT_SHOULDER][1]) +
            (pts[PoseLandmark.LEFT_SHOULDER][2] - pts[PoseLandmark.RIGHT_SHOULDER][2]) * (pts[PoseLandmark.LEFT_SHOULDER][2] - pts[PoseLandmark.RIGHT_SHOULDER][2])
        )

        val scale = if (torsoScale > 0.05f) torsoScale else if (shoulderWidth > 0.05f) shoulderWidth else 1f

        var cursor = 0
        for (lmType in KEY_LANDMARKS) {
            features[cursor++] = (pts[lmType][0] - centerX) / scale
            features[cursor++] = (pts[lmType][1] - centerY) / scale
            features[cursor++] = (pts[lmType][2] - centerZ) / scale
        }

        // Biomechanical angles (8 angles, normalized to [0, 1])
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.LEFT_SHOULDER], pts[PoseLandmark.LEFT_ELBOW], pts[PoseLandmark.LEFT_WRIST]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.RIGHT_SHOULDER], pts[PoseLandmark.RIGHT_ELBOW], pts[PoseLandmark.RIGHT_WRIST]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.LEFT_HIP], pts[PoseLandmark.LEFT_SHOULDER], pts[PoseLandmark.LEFT_ELBOW]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.RIGHT_HIP], pts[PoseLandmark.RIGHT_SHOULDER], pts[PoseLandmark.RIGHT_ELBOW]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.LEFT_SHOULDER], pts[PoseLandmark.LEFT_HIP], pts[PoseLandmark.LEFT_KNEE]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.RIGHT_SHOULDER], pts[PoseLandmark.RIGHT_HIP], pts[PoseLandmark.RIGHT_KNEE]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.LEFT_HIP], pts[PoseLandmark.LEFT_KNEE], pts[PoseLandmark.LEFT_ANKLE]) / 180f
        features[cursor++] = calculateAngle3D(pts[PoseLandmark.RIGHT_HIP], pts[PoseLandmark.RIGHT_KNEE], pts[PoseLandmark.RIGHT_ANKLE]) / 180f

        // Relative vectors (normalized by scale)
        val lW = pts[PoseLandmark.LEFT_WRIST]
        val rW = pts[PoseLandmark.RIGHT_WRIST]
        val lS = pts[PoseLandmark.LEFT_SHOULDER]
        val rS = pts[PoseLandmark.RIGHT_SHOULDER]
        val nS = pts[PoseLandmark.NOSE]

        // 1. L wrist - L shoulder
        features[cursor++] = (lW[0] - lS[0]) / scale
        features[cursor++] = (lW[1] - lS[1]) / scale
        features[cursor++] = (lW[2] - lS[2]) / scale

        // 2. R wrist - R shoulder
        features[cursor++] = (rW[0] - rS[0]) / scale
        features[cursor++] = (rW[1] - rS[1]) / scale
        features[cursor++] = (rW[2] - rS[2]) / scale

        // 3. L wrist - Nose
        features[cursor++] = (lW[0] - nS[0]) / scale
        features[cursor++] = (lW[1] - nS[1]) / scale
        features[cursor++] = (lW[2] - nS[2]) / scale

        // 4. R wrist - Nose
        features[cursor++] = (rW[0] - nS[0]) / scale
        features[cursor++] = (rW[1] - nS[1]) / scale
        features[cursor++] = (rW[2] - nS[2]) / scale

        // 5. L wrist - R wrist
        features[cursor++] = (lW[0] - rW[0]) / scale
        features[cursor++] = (lW[1] - rW[1]) / scale
        features[cursor++] = (lW[2] - rW[2]) / scale

        return features
    }

    private fun calculateAngle3D(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val baX = a[0] - b[0]
        val baY = a[1] - b[1]
        val baZ = a[2] - b[2]

        val bcX = c[0] - b[0]
        val bcY = c[1] - b[1]
        val bcZ = c[2] - b[2]

        val dot = (baX * bcX) + (baY * bcY) + (baZ * bcZ)
        val normBA = sqrt((baX * baX + baY * baY + baZ * baZ).toDouble())
        val normBC = sqrt((bcX * bcX + bcY * bcY + bcZ * bcZ).toDouble())

        if (normBA < 1e-6 || normBC < 1e-6) return 0f
        val cosine = (dot / (normBA * normBC)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosine)).toFloat()
    }

    fun reset() {
        sequenceBuffer.clear()
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
