package amba.dojo.pulse.market

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import amba.dojo.pulse.ui.theme.GoogleSans
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

private val QrSurface        = Color(0xFF131314)
private val QrContainerLow   = Color(0xFF1B1B1C)
private val QrContainer      = Color(0xFF1F2020)
private val QrContainerHigh  = Color(0xFF2A2A2A)
private val QrContainerLowest= Color(0xFF0E0E0F)
private val QrPrimary        = Color(0xFFFAB4B8)
private val QrOnPrimary      = Color(0xFF4F2227)
private val QrPrimaryCont    = Color(0xFF381015)
private val QrOnPrimaryCont  = Color(0xFFB27579)
private val QrSecondary      = Color(0xFFF5B6BE)
private val QrOnSurface      = Color(0xFFE4E2E2)
private val QrOnSurfaceVar   = Color(0xFFD6C2C2)
private val QrSegInactive    = Color(0xFF353535)

@Composable
fun QrisPaymentScreen(
    totalAmount: Int = 780_000,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var timeLeftSeconds by remember { mutableIntStateOf(14 * 60 + 59) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val totalSeconds = 15 * 60
    val orderId = "DP-ORD-892401"

    LaunchedEffect(Unit) {
        while (timeLeftSeconds > 0) {
            delay(1_000)
            timeLeftSeconds--
        }
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3_000)
            statusBanner = null
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val formattedTimer = "%02d:%02d".format(minutes, seconds)
    val activeSegments = ((timeLeftSeconds.toFloat() / totalSeconds.toFloat()) * 10f)
        .toInt()
        .coerceIn(0, 10)
    val numericAmount = formatRupiah(totalAmount).removePrefix("Rp ")

    Scaffold(
        containerColor = QrSurface,
        topBar = {
            QrTopBar(onBack = onNavigateBack)
        },
        bottomBar = {
            QrStickyBottomBar(
                onAlreadyPaid = { showSuccessDialog = true },
                onChangeMethod = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(QrSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .background(QrPrimary, RoundedCornerShape(2.dp))
                        )
                        Text(
                            "AWAITING PAYMENT",
                            color = QrPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            softWrap = false
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, tint = QrOnSurfaceVar, modifier = Modifier.size(15.dp))
                        Text(
                            "ASPI / BI VERIFIED",
                            color = QrOnSurfaceVar,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            softWrap = false
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrContainerLow, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TRANSACTION TIME LIMIT",
                                color = QrOnSurfaceVar,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    null,
                                    tint = QrPrimary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(bottom = 3.dp)
                                )
                                Text(
                                    formattedTimer,
                                    color = QrPrimary,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSans,
                                    softWrap = false
                                )
                                Text(
                                    "MINUTES",
                                    color = QrOnSurfaceVar,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    softWrap = false
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "TRANSACTION ID",
                                color = QrOnSurfaceVar,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(orderId))
                                    statusBanner = "Transaction ID #$orderId copied to clipboard!"
                                }
                            ) {
                                Text(
                                    "#$orderId",
                                    color = QrOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false
                                )
                                Icon(Icons.Default.ContentCopy, null, tint = QrOnSurfaceVar, modifier = Modifier.size(15.dp))
                            }
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(10) { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(if (idx < activeSegments) QrPrimary else QrSegInactive)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrPrimaryCont, RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "TOTAL PAYMENT BILL",
                        color = QrOnPrimaryCont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        softWrap = false
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Rp",
                            color = QrPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 5.dp),
                            softWrap = false
                        )
                        Text(
                            numericAmount,
                            color = QrPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            softWrap = false
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LockReset, null, tint = QrOnPrimaryCont, modifier = Modifier.size(14.dp))
                        Text(
                            "Amount automatically verified with no extra fees",
                            color = QrOnPrimaryCont,
                            fontSize = 11.sp,
                            softWrap = false
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrContainerLowest, RoundedCornerShape(6.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .background(QrOnSurface, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "QRIS",
                                    color = Color(0xFF303031),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    softWrap = false
                                )
                            }
                            Text(
                                "NATIONAL STANDARD",
                                color = QrOnSurfaceVar,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(QrContainer, RoundedCornerShape(2.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "GPN",
                                color = QrSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                softWrap = false
                            )
                            Icon(Icons.Default.SyncAlt, null, tint = QrSecondary, modifier = Modifier.size(13.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(252.dp)
                            .background(QrOnSurface)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DojoQrisMatrixCanvas(modifier = Modifier.fillMaxSize())
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "DOJOPULSE OFFICIAL STORE",
                        color = QrOnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        softWrap = false
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "NMID: ID1020268894120",
                        color = QrOnSurfaceVar,
                        fontSize = 10.sp,
                        fontFamily = GoogleSans,
                        softWrap = false
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(QrContainer, RoundedCornerShape(4.dp))
                                .clickable { statusBanner = "QR Code saved to Gallery!" }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, null, tint = QrOnSurface, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("DOWNLOAD QR", color = QrOnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, softWrap = false)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(QrContainer, RoundedCornerShape(4.dp))
                                .clickable { statusBanner = "QRIS payment link copied!" }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, null, tint = QrOnSurface, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SHARE", color = QrOnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, softWrap = false)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrContainer, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "SUPPORTED PAYMENT APPS:",
                        color = QrOnSurfaceVar,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        softWrap = false
                    )

                    val row1 = listOf("BCA" to QrOnSurface, "LIVIN" to QrOnSurface, "BRIMO" to QrOnSurface, "BNI" to QrOnSurface)
                    val row2 = listOf("GOPAY" to QrSecondary, "OVO" to QrPrimary, "DANA" to QrOnSurface, "SHOPEEPAY" to QrSecondary)

                    listOf(row1, row2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { (name, color) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(QrContainerHigh, RoundedCornerShape(4.dp))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        name,
                                        color = color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrContainerLow, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PAYMENT INSTRUCTIONS", color = QrOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("4 EASY STEPS", color = QrPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, softWrap = false)
                    }

                    val steps = listOf(
                        "Open your mobile banking (BCA, Livin, BRImo) or e-wallet (GoPay, OVO, DANA).",
                        "Select the Pay / QRIS menu on your app's home screen.",
                        "Scan the QR code on this screen or upload a screenshot from your gallery.",
                        "Verify recipient is DojoPulse Store with exact amount ${formatRupiah(totalAmount)}, then enter PIN."
                    )

                    steps.forEachIndexed { index, text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(QrContainer, RoundedCornerShape(4.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(QrPrimaryCont, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    color = QrPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text,
                                color = QrOnSurface,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QrContainer, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, null, tint = QrOnSurfaceVar, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Need verification help?", color = QrOnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("DojoPulse 24/7 Support Hotline", color = QrOnSurfaceVar, fontSize = 11.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(QrContainerHigh, RoundedCornerShape(4.dp))
                            .clickable { statusBanner = "Connecting to DojoPulse Support..." }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("HELP", color = QrPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            statusBanner?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                        .background(Color(0xFF24161A), RoundedCornerShape(8.dp))
                        .border(1.dp, QrPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(msg, color = QrOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF19191E), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF4F2227), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF163829))
                        .border(1.dp, Color(0xFF34D399), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(30.dp))
                }

                Text(
                    "PAYMENT VERIFIED",
                    color = QrPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )

                Text(
                    "Payment of ${formatRupiah(totalAmount)} received. Order #DP-ORD-892401 is ready for pickup at Locker #08.",
                    color = QrOnSurfaceVar,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = {
                        showSuccessDialog = false
                        onPaymentSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE63946),
                        contentColor = Color.White
                    )
                ) {
                    Text("RETURN TO DASHBOARD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QrTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QrContainerLowest)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Checkout",
                    tint = QrOnSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(QrPrimaryCont, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, null, tint = QrPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                "QRIS PAYMENT",
                color = QrOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                softWrap = false
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(QrPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = QrOnPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QrStickyBottomBar(
    onAlreadyPaid: () -> Unit,
    onChangeMethod: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(QrContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Security, null, tint = QrPrimary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "256-BIT ENCRYPTION PROTECTED BY BANK INDONESIA & ASPI",
                color = QrOnSurfaceVar,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                softWrap = false
            )
        }

        Button(
            onClick = onAlreadyPaid,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QrPrimaryCont,
                contentColor = QrPrimary
            )
        ) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "I HAVE PAID",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                softWrap = false
            )
        }

        Button(
            onClick = onChangeMethod,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QrContainer,
                contentColor = QrOnSurface
            )
        ) {
            Text(
                "CHANGE PAYMENT METHOD",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DojoQrisMatrixCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val darkColor = Color(0xFF1F2020)
        val lightColor = Color(0xFFE4E2E2)
        val scale = w / 240f

        fun drawFinder(x: Float, y: Float) {
            drawRect(darkColor, Offset(x * scale, y * scale), Size(60f * scale, 60f * scale))
            drawRect(lightColor, Offset((x + 10f) * scale, (y + 10f) * scale), Size(40f * scale, 40f * scale))
            drawRect(darkColor, Offset((x + 18f) * scale, (y + 18f) * scale), Size(24f * scale, 24f * scale))
        }

        drawFinder(10f, 10f)
        drawFinder(170f, 10f)
        drawFinder(10f, 170f)

        val blocks = listOf(
            floatArrayOf(80f, 15f, 10f, 20f), floatArrayOf(100f, 10f, 15f, 15f), floatArrayOf(130f, 15f, 25f, 10f),
            floatArrayOf(85f, 45f, 18f, 25f), floatArrayOf(120f, 40f, 12f, 12f), floatArrayOf(145f, 35f, 15f, 35f),
            floatArrayOf(15f, 80f, 12f, 25f), floatArrayOf(35f, 90f, 15f, 15f), floatArrayOf(60f, 80f, 10f, 30f),
            floatArrayOf(80f, 80f, 22f, 22f), floatArrayOf(110f, 75f, 18f, 28f), floatArrayOf(140f, 80f, 12f, 12f),
            floatArrayOf(160f, 85f, 20f, 15f), floatArrayOf(190f, 80f, 15f, 30f), floatArrayOf(215f, 95f, 15f, 15f),
            floatArrayOf(15f, 115f, 25f, 12f), floatArrayOf(50f, 120f, 20f, 20f), floatArrayOf(80f, 110f, 15f, 15f),
            floatArrayOf(145f, 105f, 25f, 25f), floatArrayOf(180f, 120f, 15f, 30f), floatArrayOf(205f, 115f, 25f, 15f),
            floatArrayOf(15f, 145f, 15f, 15f), floatArrayOf(40f, 145f, 30f, 15f), floatArrayOf(80f, 135f, 25f, 25f),
            floatArrayOf(115f, 130f, 18f, 22f), floatArrayOf(140f, 140f, 20f, 20f), floatArrayOf(170f, 160f, 20f, 10f),
            floatArrayOf(200f, 140f, 30f, 20f), floatArrayOf(80f, 170f, 15f, 25f), floatArrayOf(105f, 165f, 25f, 15f),
            floatArrayOf(140f, 170f, 15f, 30f), floatArrayOf(165f, 180f, 25f, 15f), floatArrayOf(200f, 170f, 30f, 30f),
            floatArrayOf(80f, 205f, 25f, 25f), floatArrayOf(115f, 195f, 15f, 35f), floatArrayOf(140f, 210f, 40f, 20f),
            floatArrayOf(190f, 210f, 15f, 20f), floatArrayOf(215f, 215f, 15f, 15f)
        )
        blocks.forEach { b ->
            drawRect(darkColor, Offset(b[0] * scale, b[1] * scale), Size(b[2] * scale, b[3] * scale))
        }

        drawRect(QrPrimaryCont, Offset(96f * scale, 96f * scale), Size(48f * scale, 48f * scale))
        val pulsePath = Path().apply {
            moveTo(106f * scale, 120f * scale)
            lineTo(112f * scale, 120f * scale)
            lineTo(116f * scale, 110f * scale)
            lineTo(124f * scale, 130f * scale)
            lineTo(128f * scale, 120f * scale)
            lineTo(134f * scale, 120f * scale)
        }
        drawPath(pulsePath, color = QrPrimary, style = Stroke(width = 3f * scale, cap = StrokeCap.Square))
        val bracketLen = 14f * scale
        val bracketStroke = 2.5f * scale

        drawLine(QrPrimary, Offset(0f, 0f), Offset(bracketLen, 0f), bracketStroke)
        drawLine(QrPrimary, Offset(0f, 0f), Offset(0f, bracketLen), bracketStroke)

        drawLine(QrPrimary, Offset(w - bracketLen, 0f), Offset(w, 0f), bracketStroke)
        drawLine(QrPrimary, Offset(w, 0f), Offset(w, bracketLen), bracketStroke)

        drawLine(QrPrimary, Offset(0f, h - bracketLen), Offset(0f, h), bracketStroke)
        drawLine(QrPrimary, Offset(0f, h), Offset(bracketLen, h), bracketStroke)

        drawLine(QrPrimary, Offset(w - bracketLen, h), Offset(w, h), bracketStroke)
        drawLine(QrPrimary, Offset(w, h - bracketLen), Offset(w, h), bracketStroke)
    }
}