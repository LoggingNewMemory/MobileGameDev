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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import amba.dojo.pulse.ui.theme.GoogleSans
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import amba.dojo.pulse.ui.theme.DojoBottomBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// ─── Helpers ────────────────────────────────────────────────────────────────

fun saveAvatarBitmap(context: Context, bitmap: Bitmap, userId: String): Uri {
    val avatarDir = File(context.filesDir, "avatars")
    if (!avatarDir.exists()) avatarDir.mkdirs()
    val file = File(avatarDir, "$userId.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    return Uri.fromFile(file)
}

fun loadAvatarBitmap(context: Context, userId: String): Bitmap? {
    val file = File(context.filesDir, "avatars/$userId.png")
    return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
}

// ─── Colour tokens (Matching code_2.html) ────────────────────────────────────

private val BgDark        = Color(0xFF060A12)
private val BgCard        = Color(0xFF0D1524)
private val BgCardBorder  = Color(0xFF1D2D4A)
private val BgDeep        = Color(0xFF080E1A)
private val AccentRed     = Color(0xFF00B8D4)
private val AccentCoral   = Color(0xFF22D3EE)
private val TextPrimary   = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFFCBD5E1)
private val TextMuted     = Color(0xFF94A3B8)
private val TextDim       = Color(0xFF64748B)
private val EmeraldGreen  = Color(0xFF34D399)
private val AmberYellow   = Color(0xFFFBBF24)
private val SkyBlue       = Color(0xFF38BDF8)

// ─── Data model ──────────────────────────────────────────────────────────────

private data class RedeemItem(
    val category: String,
    val title: String,
    val subtitle: String,
    val cost: Int,
    val iconType: String
)

private val redeemCatalog = listOf(
    RedeemItem("GEAR SHOPPING VOUCHER", "Rp 50,000 Store Discount",            "All gear items in Dojo Store",          1_000, "voucher"),
    RedeemItem("SPECIAL ACCESS",        "Free 1 Session VR Sparring / AI Arena","MediaPipe Biomechanics Analysis",      1_500, "vr"),
    RedeemItem("HARDWARE REWARD",       "Smart Biometric Handwraps Marker",     "Optical reticle compatible with sensor HUD", 2_000, "hardware"),
    RedeemItem("OFFICIAL MERCH",        "Tactical Velcro Patch DojoPulse",      "Limited edition tactical embroidered patch",  800, "patch")
)

// ─── Root composable ─────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val auth    = FirebaseAuth.getInstance()
    val user    = auth.currentUser
    val userId  = user?.uid ?: "77409"

    var displayName   by remember { mutableStateOf(user?.displayName ?: "") }
    val email         = user?.email?.takeIf { it.isNotBlank() } ?: "jeremyarden2808@gmail.com"
    var avatarBitmap  by remember { mutableStateOf<Bitmap?>(loadAvatarBitmap(context, userId)) }
    var isSaving      by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMsg  by remember { mutableStateOf(true) }
    var rawBitmap     by remember { mutableStateOf<Bitmap?>(null) }
    var showCrop      by remember { mutableStateOf(false) }
    var points        by remember { mutableIntStateOf(2_450) }
    var tabAktif      by remember { mutableStateOf(true) }
    var showEdit      by remember { mutableStateOf(false) }
    var toast         by remember { mutableStateOf<String?>(null) }
    var catalogScrollY by remember { mutableIntStateOf(0) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { s ->
                    BitmapFactory.decodeStream(s)?.let { bmp -> rawBitmap = bmp; showCrop = true }
                }
            } catch (_: Exception) {
                toast = "Failed to load image"
            }
        }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pickerLauncher.launch("image/*")
    }
    fun pickImage() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED)
            pickerLauncher.launch("image/*")
        else permLauncher.launch(perm)
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(4_000)
            toast = null
        }
    }

    Scaffold(
        containerColor = BgDark,
        bottomBar = { DojoBottomBar("profil", onNavigate) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 28.dp)
            ) {
                PfTopBar()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PfIdentityCard(userId, displayName, email, avatarBitmap) { showEdit = true }

                    PfPointsCard(
                        points = points,
                        onScrollToCatalog = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(catalogScrollY)
                            }
                        }
                    )

                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            catalogScrollY = coords.positionInParent().y.roundToInt()
                        }
                    ) {
                        PfRedeemSection(
                            items = redeemCatalog,
                            balance = points,
                            onRedeem = { item ->
                                if (points >= item.cost) {
                                    points -= item.cost
                                    toast = "Successfully redeemed ${"%,d".format(item.cost)} DP for: ${item.title}"
                                } else {
                                    toast = "Insufficient points (Need ${"%,d".format(item.cost)} DP)"
                                }
                            }
                        )
                    }

                    PfVoucherSection(tabAktif, { tabAktif = it }) { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        toast = "Code $code copied to clipboard!"
                    }

                    PfSettingsSection()

                    PfLogoutSection {
                        auth.signOut()
                        onLogout()
                    }
                }
            }

            // Toast Notification
            toast?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .background(Color(0xFF1A1720), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF442834), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentCoral)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            msg,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { toast = null },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "CLOSE",
                                color = AccentCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEdit) {
        PfEditDialog(
            displayName = displayName,
            email = email,
            isSaving = isSaving,
            statusMessage = statusMessage,
            isSuccessMsg = isSuccessMsg,
            avatarBitmap = avatarBitmap,
            onPickImage = { pickImage() },
            onNameChange = { displayName = it },
            onSave = {
                if (user != null && displayName.isNotBlank()) {
                    isSaving = true
                    statusMessage = null
                    user.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()
                    ).addOnCompleteListener { task ->
                        isSaving = false
                        if (task.isSuccessful) {
                            statusMessage = "Profile updated successfully!"
                            isSuccessMsg = true
                        } else {
                            statusMessage = "Failed: ${task.exception?.localizedMessage}"
                            isSuccessMsg = false
                        }
                    }
                }
            },
            onDismiss = { showEdit = false }
        )
    }

    if (showCrop && rawBitmap != null) {
        CropImageDialog(rawBitmap!!, onDismiss = { showCrop = false }) { cropped ->
            avatarBitmap = cropped
            saveAvatarBitmap(context, cropped, userId)
            showCrop = false
            toast = "Profile photo saved successfully!"
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun PfTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Profile",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PfNavIcon(icon: ImageVector, desc: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF16161C))
            .border(1.dp, Color(0xFF262630), CircleShape)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = TextSecondary, modifier = Modifier.size(17.dp))
    }
}

// ─── 1. Fighter Identity Master Card & Telemetry ─────────────────────────────

@Composable
private fun PfIdentityCard(
    userId: String,
    displayName: String,
    email: String,
    avatarBitmap: Bitmap?,
    onEdit: () -> Unit
) {
    val shortId = "DP-${userId.take(5).uppercase()}"
    val initials = displayName.trim()
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "JA" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, BgCardBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Avatar + Fighter Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AccentRed, AccentCoral)))
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(BgCard),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                initials,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    // Emerald online status dot
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(BgCard)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "@${displayName.ifBlank { "jeremyarden2808" }}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        email,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1D1D26))
            Spacer(Modifier.height(14.dp))

            // Quick Telemetry Stats Metric Blocks (3 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PfStatBox("WORKOUTS", "5/7", "sessions", TextPrimary, Modifier.weight(1f))
                PfStatBox("FORM SCORE", "88%", "", AccentCoral, Modifier.weight(1f))
                PfStatBox("MAT TIME", "4h 25m", "", TextPrimary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PfStatBox(
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(BgDeep, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F1F28), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                value,
                color = valueColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            if (unit.isNotEmpty()) {
                Text(
                    unit,
                    color = TextDim,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// ─── 2. DP Points Reserve (Hero Loyalty Points Hub) ──────────────────────────

@Composable
private fun PfPointsCard(
    points: Int,
    onScrollToCatalog: () -> Unit
) {
    val estimasi = (points * 50L).toString().reversed().chunked(3).joinToString(".").reversed()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1B171C), Color(0xFF141419))))
            .border(1.dp, Color(0xFF2F2127), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(14.dp)) {

            // Card Top Info: REWARD HUB  |  TIER: ELITE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "REWARD HUB",
                    color = AccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    softWrap = false
                )

                // Single-line capsule pill for TIER: ELITE
                Row(
                    modifier = Modifier
                        .background(Color(0xFF0A2A3B), CircleShape)
                        .border(1.dp, Color(0xFF164E63), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TIER: ELITE",
                        color = TextPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSans,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Balance & Value Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        "FIGHTER POINTS BALANCE",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "%,d".format(points),
                            color = TextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 34.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            "DP",
                            color = AccentCoral,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            append("Est: ")
                            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                                append("Rp $estimasi")
                            }
                            append(" · ")
                            withStyle(SpanStyle(color = AccentCoral, fontWeight = FontWeight.Medium)) {
                                append("15% Off All Gear")
                            }
                        },
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = TextSecondary
                    )
                }

                // Single-line "Tukar Poin v" button matching code_2.html
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF241C22))
                        .border(1.dp, Color(0xFF44232C), RoundedCornerShape(12.dp))
                        .clickable { onScrollToCatalog() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Redeem Points",
                        color = AccentCoral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Tukar Poin",
                        tint = AccentCoral,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF232029))
            Spacer(Modifier.height(10.dp))

            // Segmented Cadence Meter for Tier Progress (Matching code_2.html)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VIP Striker Tier",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    "550 DP to Master Belt",
                    color = AccentCoral,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF201D24))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .weight(5f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(AccentRed)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color(0xFF35252B))
                )
            }
        }
    }
}

// ─── 3. Redeem Points Catalog (Tukar Poin Hadiah) ────────────────────────────

@Composable
private fun PfRedeemSection(
    items: List<RedeemItem>,
    balance: Int,
    onRedeem: (RedeemItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Single-line Header Row matching code_2.html
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "REWARDS REDEEM CATALOG",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1D1D26), CircleShape)
                        .border(1.dp, Color(0xFF2B2B38), CircleShape)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${items.size} Available",
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                "Weekly Update",
                color = AccentCoral,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }

        items.forEach { item ->
            PfRedeemCard(item, balance >= item.cost) { onRedeem(item) }
        }
    }
}

@Composable
private fun PfRedeemCard(item: RedeemItem, canAfford: Boolean, onRedeem: () -> Unit) {
    val (iconBg, iconBorder, iconTint) = when (item.iconType) {
        "voucher"  -> Triple(Color(0xFF0A2A3B), Color(0xFF164E63), AccentCoral)
        "vr"       -> Triple(Color(0xFF171C26), Color(0xFF23324A), SkyBlue)
        "hardware" -> Triple(Color(0xFF101014), Color(0xFF2C2C36), TextSecondary)
        else       -> Triple(Color(0xFF241D14), Color(0xFF3E2E18), AmberYellow)
    }
    val iconVec = when (item.iconType) {
        "voucher"  -> Icons.Default.ConfirmationNumber
        "vr"       -> Icons.Default.ViewInAr
        "hardware" -> Icons.Default.Watch
        else       -> Icons.Default.StarOutline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon container 48x48
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBg, RoundedCornerShape(12.dp))
                .border(1.dp, iconBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconVec, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        // Item details
        Column(Modifier.weight(1f)) {
            Text(
                item.category,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(2.dp))
            Text(
                item.title,
                color = TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                item.subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Cost + Tukar button
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "%,d DP".format(item.cost),
                color = AccentCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onRedeem,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) AccentRed else Color(0xFF131E33),
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    "Redeem",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// ─── 4. Promo & Voucher Saya ──────────────────────────────────────────────────

@Composable
private fun PfVoucherSection(isAktif: Boolean, onTab: (Boolean) -> Unit, onCopy: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MY PROMOS & VOUCHERS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        // Segmented Pill Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard, RoundedCornerShape(12.dp))
                .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(true to "Active Vouchers (2)", false to "Used History (1)").forEach { (aktif, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAktif == aktif) AccentRed else Color.Transparent)
                        .clickable { onTab(aktif) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isAktif == aktif) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isAktif == aktif) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (isAktif) {
            PfVoucherActive("DOJOCHAMP", "30% Off Mats & Wraps", "Valid until Nov 30, 2026", onCopy)
            PfVoucherActive("GEARCASH20", "20% Cashback Apparel & Fightwear", "Min. spend Rp 250,000", onCopy)
        } else {
            PfVoucherUsed()
        }
    }
}

@Composable
private fun PfVoucherActive(code: String, title: String, expiry: String, onCopy: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .background(Color(0xFF0A2A3B), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF164E63), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    code,
                    color = AccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSans,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Box(
                Modifier
                    .background(Color(0x1A34D399), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "Active",
                    color = EmeraldGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Text(
            title,
            color = TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(color = Color(0xFF1E1E28))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(expiry, color = TextMuted, fontSize = 11.sp, maxLines = 1, softWrap = false)
            Row(
                modifier = Modifier.clickable { onCopy(code) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, tint = AccentCoral, modifier = Modifier.size(13.dp))
                Text(
                    "Copy Code",
                    color = AccentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun PfVoucherUsed() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .background(Color(0xFF1C1C24), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "HBAG-OCT-FREE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = GoogleSans,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Box(
                Modifier
                    .background(Color(0xFF27272A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "Already Used",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Text(
            "Free 1 Hour Heavy Bag Session Zone B",
            color = TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(color = Color(0xFF1E1E28))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Redeemed Oct 02, 2026", color = TextDim, fontSize = 11.sp, maxLines = 1, softWrap = false)
        }
    }
}

// ─── 5. Pengaturan Akun & Sensor ─────────────────────────────────────────────

@Composable
private fun PfSettingsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "ACCOUNT & SENSOR SETTINGS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            softWrap = false
        )
        PfSettingsRow(
            Icons.Default.Lock,
            Color(0xFF171B26),
            Color(0xFF1F2D42),
            SkyBlue,
            "Change Password & Biometrics",
            "Update password and account security"
        )
    }
}

@Composable
private fun PfSettingsRow(
    icon: ImageVector,
    iconBg: Color,
    iconBorder: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF202028), RoundedCornerShape(12.dp))
            .clickable { }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(8.dp))
                .border(1.dp, iconBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    title,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) {
                    Box(
                        Modifier
                            .background(EmeraldGreen.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            badge,
                            color = EmeraldGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextDim, modifier = Modifier.size(16.dp))
    }
}

// ─── 6. Minimalist Logout Action ─────────────────────────────────────────────

@Composable
private fun PfLogoutSection(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .border(1.dp, Color(0xFF262632), RoundedCornerShape(12.dp))
                .clickable { onLogout() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AccentCoral, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "LOG OUT",
                color = AccentCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ─── Edit Profile Dialog ──────────────────────────────────────────────────────

@Composable
private fun PfEditDialog(
    displayName: String,
    email: String,
    isSaving: Boolean,
    statusMessage: String?,
    isSuccessMsg: Boolean,
    avatarBitmap: Bitmap?,
    onPickImage: () -> Unit,
        onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextPrimary) }
                    Text("EDIT PROFILE", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.size(48.dp))
                }
                Spacer(Modifier.height(24.dp))

                val initials = displayName.trim()
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
                    .ifBlank { "JA" }

                Box(Modifier.size(100.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AccentRed, AccentCoral)))
                            .clickable { onPickImage() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                avatarBitmap.asImageBitmap(),
                                null,
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(initials, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        }
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22D3EE))
                            .clickable { onPickImage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF060A12), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Tap to change photo", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(28.dp))

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("FULL NAME", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = onNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Color(0xFF22D3EE)
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("EMAIL", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(email, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }

                statusMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        color = if (isSuccessMsg) Color(0xFF81C784) else Color(0xFFE57373),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE)),
                    enabled = !isSaving && displayName.isNotBlank()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF060A12), strokeWidth = 2.dp)
                    } else {
                        Text("SAVE CHANGES", color = Color(0xFF060A12), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun CropImageDialog(bitmap: Bitmap, onDismiss: () -> Unit, onCropConfirmed: (Bitmap) -> Unit) {
    var scale   by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White, fontSize = 16.sp) }
                    Text("ADJUST (1:1)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { onCropConfirmed(cropSquareBitmap(bitmap, scale, offsetX, offsetY)) }) {
                        Text("Choose", color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Pan & pinch to adjust 1:1",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    Modifier
                        .size(280.dp)
                        .clipToBounds()
                        .border(2.dp, Color(0xFF22D3EE), RoundedCornerShape(8.dp))
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
                        bitmap.asImageBitmap(),
                        null,
                        Modifier.graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scale = 1f; offsetX = 0f; offsetY = 0f }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                    IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(5f) }) {
                        Icon(Icons.Default.ZoomIn, null, tint = Color.White)
                    }
                    IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

fun cropSquareBitmap(source: Bitmap, scale: Float, offsetX: Float, offsetY: Float): Bitmap {
    val outputSize = 512
    val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    val boxSize = 280f
    val srcW = source.width.toFloat()
    val srcH = source.height.toFloat()
    val baseScale = boxSize / maxOf(srcW, srcH)
    val totalScale = baseScale * scale
    val drawnW = srcW * totalScale
    val drawnH = srcH * totalScale
    val drawnLeft = (boxSize - drawnW) / 2f + offsetX
    val drawnTop  = (boxSize - drawnH) / 2f + offsetY
    val outputRatio = outputSize / boxSize
    val matrix = Matrix().apply {
        postScale(totalScale, totalScale)
        postTranslate(drawnLeft, drawnTop)
        postScale(outputRatio, outputRatio)
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return result
}