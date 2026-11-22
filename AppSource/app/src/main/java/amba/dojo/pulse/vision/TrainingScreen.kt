package amba.dojo.pulse.vision

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onFinishTraining: (TrainingTelemetry) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenTutorial: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isFrontCamera by remember { mutableStateOf(true) }
    var telemetry by remember { mutableStateOf(TrainingTelemetry()) }
    val aiClassifier = remember {
        AIPoseMovementClassifier(
            context = context,
            modelFileName = "boxing_classifier_quantized.tflite",
            labelsFileName = "boxing_classifier_labels.txt"
        )
    }
    val classifier = remember { PunchClassifier(FighterStance.ORTHODOX, aiClassifier) }

    // Guided Dummy Training Routine State
    var selectedRoutineIndex by remember { mutableIntStateOf(0) }
    val currentRoutine = PresetExercises.ALL_ROUTINES[selectedRoutineIndex]
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var currentStepCount by remember { mutableIntStateOf(0) }
    var lastRecordedStrikes by remember { mutableIntStateOf(0) }
    var isRoutineFinished by remember { mutableStateOf(false) }
    var stepNoticeMessage by remember { mutableStateOf<String?>(null) }

    // 3-minute Drill Timer countdown (180 seconds)
    var timeLeftSeconds by remember { mutableIntStateOf(180) }
    LaunchedEffect(Unit) {
        while (timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
        }
    }

    // Auto-clear step completion notice after 3 seconds
    LaunchedEffect(stepNoticeMessage) {
        if (stepNoticeMessage != null) {
            delay(3000L)
            stepNoticeMessage = null
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val poseDetector = remember {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            poseDetector.close()
            aiClassifier.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Live Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            @androidx.annotation.OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val image = InputImage.fromMediaImage(mediaImage, rotation)

                                poseDetector.process(image)
                                    .addOnSuccessListener { pose ->
                                        val isRotated = rotation == 90 || rotation == 270
                                        val width = if (isRotated) imageProxy.height else imageProxy.width
                                        val height = if (isRotated) imageProxy.width else imageProxy.height

                                        val updated = classifier.processPose(pose, width, height)
                                        telemetry = updated

                                        // Progress tracking for guided training exercise
                                        if (updated.totalStrikes > lastRecordedStrikes) {
                                            lastRecordedStrikes = updated.totalStrikes
                                            if (!isRoutineFinished && currentStepIndex < currentRoutine.steps.size) {
                                                val step = currentRoutine.steps[currentStepIndex]
                                                if (step.matches(updated.lastDetectedPunch)) {
                                                    val newCount = currentStepCount + 1
                                                    if (newCount >= step.targetCount) {
                                                        if (currentStepIndex + 1 < currentRoutine.steps.size) {
                                                            currentStepIndex++
                                                            currentStepCount = 0
                                                            stepNoticeMessage = "DRILL STEP SELESAI! LANJUT KE: ${currentRoutine.steps[currentStepIndex].title.uppercase()}"
                                                        } else {
                                                            currentStepCount = step.targetCount
                                                            isRoutineFinished = true
                                                            stepNoticeMessage = "LUAR BIASA! SELURUH TARGET LATIHAN TUNTAS!"
                                                        }
                                                    } else {
                                                        currentStepCount = newCount
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Real-Time Skeleton Overlay
            PoseCanvas(
                telemetry = telemetry,
                isFrontCamera = isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Request State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFFFB3BA),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Camera permission is required for AI Pose Tracking",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1525))
                    ) {
                        Text("Grant Permission", color = Color.White)
                    }
                }
            }
        }

        // Cyberpunk / Tactical HUD Layer
        TrainingHudOverlay(
            telemetry = telemetry,
            timeLeftSeconds = timeLeftSeconds,
            isFrontCamera = isFrontCamera,
            currentRoutine = currentRoutine,
            currentStepIndex = currentStepIndex,
            currentStepCount = currentStepCount,
            isRoutineFinished = isRoutineFinished,
            stepNoticeMessage = stepNoticeMessage,
            onCycleRoutine = {
                selectedRoutineIndex = (selectedRoutineIndex + 1) % PresetExercises.ALL_ROUTINES.size
                currentStepIndex = 0
                currentStepCount = 0
                isRoutineFinished = false
                stepNoticeMessage = "DRILL DIGANTI KE: ${PresetExercises.ALL_ROUTINES[(selectedRoutineIndex)].name.uppercase()}"
            },
            onResetRoutine = {
                currentStepIndex = 0
                currentStepCount = 0
                isRoutineFinished = false
                stepNoticeMessage = "DRILL DIMULAI ULANG DARI STEP 1"
            },
            onToggleCamera = { isFrontCamera = !isFrontCamera },
            onFinish = { onFinishTraining(telemetry) },
            onBack = onNavigateBack,
            onOpenTutorial = onOpenTutorial
        )
    }
}

@Composable
fun TrainingHudOverlay(
    telemetry: TrainingTelemetry,
    timeLeftSeconds: Int,
    isFrontCamera: Boolean,
    currentRoutine: TrainingExerciseRoutine,
    currentStepIndex: Int,
    currentStepCount: Int,
    isRoutineFinished: Boolean,
    stepNoticeMessage: String?,
    onCycleRoutine: () -> Unit,
    onResetRoutine: () -> Unit,
    onToggleCamera: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit = {}
) {
    val accentRed = Color(0xFF4A1525)
    val accentPink = Color(0xFFFFB3BA)
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)
    val currentStep = if (currentStepIndex < currentRoutine.steps.size) currentRoutine.steps[currentStepIndex] else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top HUD Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x88121212), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Session Timer
                Box(
                    modifier = Modifier
                        .background(Color(0xAA121212), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = accentPink, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            timerString,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guided Exercise Target Card (Precision Striking Drill)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD161622)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRoutineFinished) Color(0xFF81C784) else accentPink.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isRoutineFinished) Color(0xFF2E7D32) else accentRed,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (isRoutineFinished) "FINISHED" else "STEP ${currentStepIndex + 1}/${currentRoutine.steps.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isRoutineFinished) "ALL DRILLS COMPLETE" else currentRoutine.subtitle,
                                color = if (isRoutineFinished) Color(0xFF81C784) else accentPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Cycle routine button
                        IconButton(
                            onClick = onCycleRoutine,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Ganti Drill",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isRoutineFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "SELURUH TARGET LATIHAN TUNTAS!",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "Semua step latihan berhasil diselesaikan dengan baik.",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (currentStep != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentStep.title.uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$currentStepCount / ${currentStep.targetCount}",
                                color = currentStep.badgeColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val stepProgress = (currentStepCount.toFloat() / currentStep.targetCount.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { stepProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = currentStep.badgeColor,
                            trackColor = Color(0xFF2A2A38)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "💡 ${currentStep.instruction}",
                            color = Color(0xFFCCCCCC),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }

                    // Temporary step transition notice
                    stepNoticeMessage?.let { notice ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B5E20), RoundedCornerShape(4.dp))
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "⚡ $notice",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }


        // Bottom HUD Card: Progress, Form Score, and Finish Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xDD1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TARGET PUKULAN", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${telemetry.totalStrikes} / ${telemetry.targetStrikes} Strikes",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val progress = (telemetry.totalStrikes.toFloat() / telemetry.targetStrikes.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentPink,
                    trackColor = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown of punches matching Dataset categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("JAB: ${telemetry.strikeCounts[PunchType.LEFT_JAB] ?: 0}", color = Color.LightGray, fontSize = 10.sp)
                    Text("STRIKE: ${telemetry.strikeCounts[PunchType.STRIKE] ?: 0}", color = Color.LightGray, fontSize = 10.sp)
                    Text("HOOK: ${(telemetry.strikeCounts[PunchType.LEFT_HOOK] ?: 0) + (telemetry.strikeCounts[PunchType.RIGHT_HOOK] ?: 0)}", color = Color.LightGray, fontSize = 10.sp)
                    Text("UPPER: ${(telemetry.strikeCounts[PunchType.LEFT_UPPERCUT] ?: 0) + (telemetry.strikeCounts[PunchType.RIGHT_UPPERCUT] ?: 0)}", color = Color.LightGray, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SELESAIKAN LATIHAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
