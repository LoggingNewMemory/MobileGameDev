package amba.dojo.pulse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

// Helper to save avatar bitmap locally and get file uri
fun saveAvatarBitmap(context: Context, bitmap: Bitmap, userId: String): Uri {
    val avatarDir = File(context.filesDir, "avatars")
    if (!avatarDir.exists()) avatarDir.mkdirs()
    val file = File(avatarDir, "$userId.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return Uri.fromFile(file)
}

fun loadAvatarBitmap(context: Context, userId: String): Bitmap? {
    val file = File(context.filesDir, "avatars/$userId.png")
    return if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid ?: "guest"

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    val email = currentUser?.email ?: "-"
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(loadAvatarBitmap(context, userId)) }

    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(true) }

    // State for crop dialog
    var rawSelectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream)
                    if (original != null) {
                        rawSelectedBitmap = original
                        showCropDialog = true
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Gagal memuat gambar: ${e.localizedMessage}"
                isSuccessMessage = false
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            // Even if storage permission denied on Android 13+, Photo Picker usually works, but let's inform user
            imagePickerLauncher.launch("image/*")
        }
    }

    fun requestMediaStorageAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    val backgroundColor = Color(0xFF131314)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentRed = Color(0xFF4A1525)
    val primaryPink = Color(0xFFfab4b8)
    val textPrimary = Color.White
    val textSecondary = Color.LightGray
    val textMuted = Color.Gray

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "EDIT PROFIL",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // User Icon Avatar with Edit Badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(110.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(accentRed)
                        .border(2.dp, primaryPink, CircleShape)
                        .clickable { requestMediaStorageAndPickImage() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "User Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initials = if (displayName.isNotBlank()) {
                            displayName.trim().split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString("")
                                .uppercase()
                        } else "AC"
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    }
                }

                // Camera overlay button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primaryPink)
                        .clickable { requestMediaStorageAndPickImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color(0xFF4A1525),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ketuk untuk ganti foto profil",
                color = textMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Form container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NAMA LENGKAP",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryPink,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = primaryPink
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "EMAIL",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = email,
                        color = textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ID ANGGOTA",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DOJO PULSE // ID.${userId.take(6).uppercase()}",
                        color = textSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusMessage ?: "",
                    color = if (isSuccessMessage) Color(0xFF81C784) else Color(0xFFE57373),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (currentUser != null && displayName.isNotBlank()) {
                        isSaving = true
                        statusMessage = null
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.trim())
                            .build()

                        currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
                            isSaving = false
                            if (task.isSuccessful) {
                                statusMessage = "Profil berhasil diperbarui!"
                                isSuccessMessage = true
                            } else {
                                statusMessage = "Gagal memperbarui: ${task.exception?.localizedMessage}"
                                isSuccessMessage = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryPink),
                enabled = !isSaving && displayName.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4A1525),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "SIMPAN PERUBAHAN",
                        color = Color(0xFF4A1525),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            OutlinedButton(
                onClick = {
                    auth.signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7A2020))
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOG OUT",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 1:1 Customizable Crop Dialog
    if (showCropDialog && rawSelectedBitmap != null) {
        CropImageDialog(
            bitmap = rawSelectedBitmap!!,
            onDismiss = { showCropDialog = false },
            onCropConfirmed = { croppedBitmap ->
                avatarBitmap = croppedBitmap
                saveAvatarBitmap(context, croppedBitmap, userId)
                showCropDialog = false
                statusMessage = "Foto profil berhasil dipotong & disimpan!"
                isSuccessMessage = true
            }
        )
    }
}

/**
 * 1:1 Customizable Crop Dialog with Pan (drag) and Zoom (pinch)
 */
@Composable
fun CropImageDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.White, fontSize = 16.sp)
                    }
                    Text(
                        text = "SESUAIKAN (1:1)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    TextButton(onClick = {
                        val cropped = cropSquareBitmap(bitmap, scale, offsetX, offsetY)
                        onCropConfirmed(cropped)
                    }) {
                        Text("Pilih", color = Color(0xFFfab4b8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Geser & cubit untuk menyesuaikan 1:1",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 1:1 Crop Viewport Area
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clipToBounds()
                        .border(2.dp, Color(0xFFfab4b8), RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Crop preview",
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                // Grid guide / circle hint
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                    }
                    IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(5f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                    }
                    IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Creates a cropped 1:1 square bitmap based on scale and pan offsets.
 * Uses Android Canvas drawing to avoid invalid source rects or empty bitmaps.
 */
fun cropSquareBitmap(
    source: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Bitmap {
    val outputSize = 512
    val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)

    val boxSize = 280f
    val srcW = source.width.toFloat()
    val srcH = source.height.toFloat()

    // Base scale fits the image into the 280dp box (ContentScale.Fit)
    val baseScale = boxSize / maxOf(srcW, srcH)
    val totalScale = baseScale * scale

    // In 280dp coordinates:
    val drawnW = srcW * totalScale
    val drawnH = srcH * totalScale
    val drawnLeft = (boxSize - drawnW) / 2f + offsetX
    val drawnTop = (boxSize - drawnH) / 2f + offsetY

    // Ratio to convert from 280dp box space to 512px output bitmap space
    val outputRatio = outputSize / boxSize

    val matrix = Matrix().apply {
        // First scale source to its drawn size in box
        postScale(totalScale, totalScale)
        // Then translate to drawn position in box
        postTranslate(drawnLeft, drawnTop)
        // Finally scale from box space (280) to output space (512)
        postScale(outputRatio, outputRatio)
    }

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)

    return result
}
