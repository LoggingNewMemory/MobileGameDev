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
    onNavigateBack: () -> Unit
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
    val classifier = remember { PunchClassifier(FighterStance.ORTHODOX) }

    // 3-minute Drill Timer countdown (180 seconds)
    var timeLeftSeconds by remember { mutableIntStateOf(180) }
    LaunchedEffect(Unit) {
        while (timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
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
            onToggleCamera = { isFrontCamera = !isFrontCamera },
            onFinish = { onFinishTraining(telemetry) },
            onBack = onNavigateBack
        )
    }
}

@Composable
fun TrainingHudOverlay(
    telemetry: TrainingTelemetry,
    timeLeftSeconds: Int,
    isFrontCamera: Boolean,
    onToggleCamera: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val accentRed = Color(0xFF4A1525)
    val accentPink = Color(0xFFFFB3BA)
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(accentPink))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SENSOR LINK: 60FPS HUD",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onToggleCamera,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x88121212), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle & Session Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(accentRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "LATIHAN UTAMA [SYS.DRL-09]",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xAA121212), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
        }

        // Center HUD: Active Punch Feedback Callout
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = telemetry.detectedPunch != PunchType.IDLE && telemetry.detectedPunch != PunchType.GUARD,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xDD3B0918), RoundedCornerShape(8.dp))
                            .border(1.dp, accentPink, RoundedCornerShape(8.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${telemetry.detectedPunch.displayName.uppercase()} [${telemetry.detectedPunch.id}]",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Telemetry cards overlay (Left & Right stats like in the mockup image)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Punch Angle Card
                    Box(
                        modifier = Modifier
                            .background(Color(0xBB121212), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("PUNCH ANGLE", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${telemetry.punchAngle.toInt()}° PRECISION",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("VALIDASI SUDUT AKTIF", color = accentPink, fontSize = 9.sp)
                        }
                    }

                    // Right: Return Guard Card
                    Box(
                        modifier = Modifier
                            .background(Color(0xBB121212), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RETURN GUARD", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                String.format("%.2f DETIK", telemetry.returnGuardTimeSeconds),
                                color = accentPink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (telemetry.isGuardActive) "ORTHODOX GUARD [OK]" else "STRIKE EXTENDED",
                                color = if (telemetry.isGuardActive) Color(0xFF81C784) else accentPink,
                                fontSize = 9.sp
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

                    Column(horizontalAlignment = Alignment.End) {
                        Text("FORM SCORE", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${telemetry.formScore}% Akurat",
                            color = accentPink,
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

                // Breakdown of punches 1 to 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("JAB: ${telemetry.strikeCounts[PunchType.JAB] ?: 0}", color = Color.LightGray, fontSize = 10.sp)
                    Text("CROSS: ${telemetry.strikeCounts[PunchType.CROSS] ?: 0}", color = Color.LightGray, fontSize = 10.sp)
                    Text("HOOK: ${(telemetry.strikeCounts[PunchType.LEAD_HOOK] ?: 0) + (telemetry.strikeCounts[PunchType.REAR_HOOK] ?: 0)}", color = Color.LightGray, fontSize = 10.sp)
                    Text("UPPER: ${(telemetry.strikeCounts[PunchType.LEAD_UPPERCUT] ?: 0) + (telemetry.strikeCounts[PunchType.REAR_UPPERCUT] ?: 0)}", color = Color.LightGray, fontSize = 10.sp)
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
