package amba.dojo.pulse.market

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import amba.dojo.pulse.ui.theme.GoogleSans
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CkBgDark       = Color(0xFF060A12)
private val CkBgCard       = Color(0xFF0D1524)
private val CkBgDeep       = Color(0xFF080E1A)
private val CkBorder       = Color(0xFF1D2D4A)
private val CkRed          = Color(0xFF00B8D4)
private val CkCoral        = Color(0xFF22D3EE)
private val CkTextPrimary  = Color(0xFFF1F5F9)
private val CkTextSec      = Color(0xFFCBD5E1)
private val CkTextMuted    = Color(0xFF94A3B8)
private val CkTextDim      = Color(0xFF64748B)
private val CkEmerald      = Color(0xFF34D399)
private val CkAmber        = Color(0xFFFBBF24)

@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onProceedToPayment: (totalAmount: Int) -> Unit
) {
    var useDpPoints by remember { mutableStateOf(true) }
    var voucherApplied by remember { mutableStateOf(true) }
    var voucherCodeInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("qris") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val subtotal = 878_000
    val voucherDiscount = if (voucherApplied) 75_000 else 0
    val pointsDiscount = if (useDpPoints) 25_000 else 0
    val serviceFee = 2_000
    val finalTotal = subtotal - voucherDiscount - pointsDiscount + serviceFee
    val totalSavings = voucherDiscount + pointsDiscount

    LaunchedEffect(feedbackMsg) {
        if (feedbackMsg != null) {
            kotlinx.coroutines.delay(3_000)
            feedbackMsg = null
        }
    }

    Scaffold(
        containerColor = CkBgDark,
        topBar = {
            CkTopBar(onBack = onNavigateBack)
        },
        bottomBar = {
            CkStickyBottomBar(
                totalAmount = finalTotal,
                savingsAmount = totalSavings,
                onPayClick = { onProceedToPayment(finalTotal) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CkBgDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CkDojoHubCard()

                CkOrderedItemsSection()

                CkVoucherSection(
                    voucherApplied = voucherApplied,
                    voucherInput = voucherCodeInput,
                    onVoucherInputChange = { voucherCodeInput = it },
                    onRemoveVoucher = {
                        voucherApplied = false
                        feedbackMsg = "Kupon DOJOCHAMP dilepas"
                    },
                    onApplyVoucher = {
                        if (voucherCodeInput.isNotBlank()) {
                            voucherApplied = true
                            feedbackMsg = "Kupon ${voucherCodeInput.uppercase()} diterapkan!"
                            voucherCodeInput = ""
                        }
                    }
                )

                CkPointsToggleCard(
                    checked = useDpPoints,
                    onToggle = { useDpPoints = it }
                )

                CkPaymentMethodSection(
                    selectedMethod = selectedMethod,
                    onSelectMethod = { selectedMethod = it }
                )

                CkBillBreakdownCard(
                    subtotal = subtotal,
                    voucherApplied = voucherApplied,
                    voucherDiscount = voucherDiscount,
                    pointsApplied = useDpPoints,
                    pointsDiscount = pointsDiscount,
                    serviceFee = serviceFee,
                    finalTotal = finalTotal
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = CkTextDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Garansi Penggantian Alat Dojo Resmi 100% Original",
                        color = CkTextDim,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            feedbackMsg?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .background(Color(0xFF1A1720), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF442834), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(msg, color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


@Composable
private fun CkTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181E))
                        .border(1.dp, Color(0xFF24242E), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel Checkout",
                        tint = CkTextSec,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        "ORDER SUMMARY",
                        color = CkCoral,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        "Checkout & Bill",
                        color = CkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier
                    .background(Color(0xFF1A1A22), CircleShape)
                    .border(1.dp, Color(0xFF2B2B36), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CkCoral)
                )
                Text(
                    "2,450 DP",
                    color = CkTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        HorizontalDivider(color = Color(0xFF1A1A20))
    }
}


@Composable
private fun CkDojoHubCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgCard, RoundedCornerShape(16.dp))
            .border(1.dp, CkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF21151B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3E1D27), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = CkCoral, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(
                        "Pickup Point / Dojo Hub",
                        color = CkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Self-Pickup at Dojo Partner",
                        color = CkTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFF241D24), CircleShape)
                    .border(1.dp, Color(0xFF3E2329), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "FREE SHIPPING",
                    color = CkCoral,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CkBgDeep, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E1E26), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Campus Martial Arts Hall",
                        color = CkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        Modifier
                            .background(CkRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "LOCKER #08",
                            color = CkCoral,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Sports Hall Sector B • 1st Floor",
                    color = CkTextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFF1A1A22), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2B2B36), RoundedCornerShape(8.dp))
                    .clickable { }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    "Change",
                    color = CkCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}


@Composable
private fun CkOrderedItemsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "EQUIPMENT SELECTED (2 ITEM)",
                color = CkTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                softWrap = false
            )
            Text(
                "OFFICIAL GEAR",
                color = CkTextDim,
                fontSize = 10.sp,
                fontFamily = GoogleSans,
                maxLines = 1,
                softWrap = false
            )
        }

        CkProductCard(
            badgeText = "BEST SELLER",
            badgeBg = CkRed,
            badgeTextColor = CkTextPrimary,
            icon = Icons.Default.SportsMma,
            title = "DojoPulse Pro Striking Gloves 14oz",
            variant = "Black Burgundy Edition · 1x",
            price = "Rp 689.000",
            qty = "1"
        )

        CkProductCard(
            badgeText = "AI MARKER",
            badgeBg = Color(0xFF27272A),
            badgeTextColor = CkTextSec,
            icon = Icons.Default.Watch,
            title = "Smart Biometric Combat Wraps",
            variant = "MediaPipe AI Marker · 1x",
            price = "Rp 189.000",
            qty = "1"
        )
    }
}

@Composable
private fun CkProductCard(
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    icon: ImageVector,
    title: String,
    variant: String,
    price: String,
    qty: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgCard, RoundedCornerShape(16.dp))
            .border(1.dp, CkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFF0A0A0D), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF23232A), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CkCoral, modifier = Modifier.size(32.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .background(badgeBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    badgeText,
                    color = badgeTextColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .height(76.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    title,
                    color = CkTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1B1C24), CircleShape)
                        .border(1.dp, Color(0xFF232430), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        variant,
                        color = CkTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    price,
                    color = CkCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    modifier = Modifier
                        .background(Color(0xFF17171E), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF262632), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Qty:", color = CkTextMuted, fontSize = 10.sp, softWrap = false)
                    Text(qty, color = CkTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, softWrap = false)
                }
            }
        }
    }
}


@Composable
private fun CkVoucherSection(
    voucherApplied: Boolean,
    voucherInput: String,
    onVoucherInputChange: (String) -> Unit,
    onRemoveVoucher: () -> Unit,
    onApplyVoucher: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgCard, RoundedCornerShape(16.dp))
            .border(1.dp, CkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.LocalOffer, null, tint = CkCoral, modifier = Modifier.size(17.dp))
                Text("Kupon & Promosi Taktis", color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (voucherApplied) "1 Voucher Aktif" else "0 Voucher",
                color = if (voucherApplied) CkEmerald else CkTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                softWrap = false
            )
        }

        if (voucherApplied) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF21151B), Color(0xFF1A141A), Color(0xFF16161C))
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color(0xFF3E1D27), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CkRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, CkRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, null, tint = CkCoral, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "DOJOCHAMP",
                                color = CkTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GoogleSans,
                                softWrap = false
                            )
                            Box(
                                Modifier
                                    .background(CkRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("-30%", color = CkTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, softWrap = false)
                            }
                        }
                        Text(
                            "Hemat Rp 75.000 (Paket Gear Bundle)",
                            color = CkTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A1B22))
                        .clickable { onRemoveVoucher() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Voucher", tint = CkTextMuted, modifier = Modifier.size(14.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = voucherInput,
                onValueChange = onVoucherInputChange,
                placeholder = {
                    Text("Enter promo code...", color = CkTextDim, fontSize = 11.sp, maxLines = 1, softWrap = false)
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF101014),
                    unfocusedContainerColor = Color(0xFF101014),
                    focusedBorderColor = CkCoral,
                    unfocusedBorderColor = CkBorder,
                    focusedTextColor = CkTextPrimary,
                    unfocusedTextColor = CkTextPrimary
                )
            )
            Button(
                onClick = onApplyVoucher,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B1C24),
                    contentColor = CkTextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D2E3D))
            ) {
                Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = false)
            }
        }
    }
}

@Composable
private fun CkPointsToggleCard(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgCard, RoundedCornerShape(16.dp))
            .border(1.dp, CkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF241D14), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF3E2E18), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MonetizationOn, null, tint = CkAmber, modifier = Modifier.size(20.dp))
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Gunakan 500 DP Points",
                        color = CkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        Modifier
                            .background(CkAmber.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Tukar Poin", color = CkAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
                Text(
                    "-Rp 25.000 Potongan Langsung",
                    color = CkCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    softWrap = false
                )
                Text(
                    "Saldo Tersedia: 2,450 DP Points",
                    color = CkTextDim,
                    fontSize = 10.sp,
                    softWrap = false
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CkRed,
                uncheckedThumbColor = CkTextMuted,
                uncheckedTrackColor = Color(0xFF24242E)
            )
        )
    }
}

@Composable
private fun CkPaymentMethodSection(
    selectedMethod: String,
    onSelectMethod: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgCard, RoundedCornerShape(16.dp))
            .border(1.dp, CkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = CkCoral, modifier = Modifier.size(17.dp))
                Text("Payment Method", color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("256-BIT ENCRYPTION", color = CkTextDim, fontSize = 10.sp, fontFamily = GoogleSans, softWrap = false)
        }

        CkPaymentOptionRow(
            selected = selectedMethod == "qris",
            title = "QRIS Instant Pay",
            subtitle = "BCA, Mandiri, GoPay, OVO, ShopeePay",
            badgeText = "INSTANT VERIFIED",
            badgeBg = Color(0xFF2B1820),
            badgeBorder = Color(0xFF401D2A),
            badgeColor = CkCoral,
            onClick = { onSelectMethod("qris") }
        )

        CkPaymentOptionRow(
            selected = selectedMethod == "va",
            title = "Automated Virtual Account",
            subtitle = "BCA, BRI, Mandiri, BNI Virtual Account",
            badgeText = "01:00 EXP",
            badgeBg = Color.Transparent,
            badgeBorder = Color.Transparent,
            badgeColor = CkTextDim,
            onClick = { onSelectMethod("va") }
        )

        CkPaymentOptionRow(
            selected = selectedMethod == "wallet",
            title = "DojoPulse Vault Wallet",
            subtitle = "Active Balance: Rp 1,450,000",
            badgeText = "SUFFICIENT BALANCE",
            badgeBg = CkEmerald.copy(alpha = 0.14f),
            badgeBorder = Color(0xFF166534),
            badgeColor = CkEmerald,
            onClick = { onSelectMethod("wallet") }
        )
    }
}

@Composable
private fun CkPaymentOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBg: Color,
    badgeBorder: Color,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF1C1418) else Color(0xFF121216))
            .border(1.dp, if (selected) CkCoral else Color(0xFF24242E), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(2.dp, if (selected) CkCoral else Color(0xFF52525B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CkCoral)
                    )
                }
            }
            Column {
                Text(title, color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(subtitle, color = CkTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .background(badgeBg, CircleShape)
                .border(1.dp, badgeBorder, CircleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                badgeText,
                color = badgeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun CkBillBreakdownCard(
    subtotal: Int,
    voucherApplied: Boolean,
    voucherDiscount: Int,
    pointsApplied: Boolean,
    pointsDiscount: Int,
    serviceFee: Int,
    finalTotal: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CkBgCard, RoundedCornerShape(16.dp))
            .border(1.dp, CkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, null, tint = CkCoral, modifier = Modifier.size(17.dp))
                Text("PAYMENT DETAILS", color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = false)
            }
            Text("AUDIT // 2 ITEMS", color = CkTextDim, fontSize = 10.sp, fontFamily = GoogleSans, softWrap = false)
        }

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Product Subtotal", color = CkTextSec, fontSize = 12.sp)
            Text(formatRupiah(subtotal), color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (voucherApplied) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Voucher Discount", color = CkCoral, fontSize = 12.sp)
                    Box(
                        Modifier
                            .background(CkRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(1.dp, CkRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("DOJOCHAMP", color = CkCoral, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = GoogleSans)
                    }
                }
                Text("-${formatRupiah(voucherDiscount)}", color = CkCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (pointsApplied) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("DP Points Discount (500 DP)", color = CkCoral, fontSize = 12.sp)
                Text("-${formatRupiah(pointsDiscount)}", color = CkCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Shipping Fee (Dojo Hub)", color = CkTextSec, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rp 18.000",
                    color = CkTextDim,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.LineThrough
                )
                Text("Free (Rp 0)", color = CkEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Service & Gear Protection Fee", color = CkTextSec, fontSize = 12.sp)
            Text(formatRupiah(serviceFee), color = CkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = Color(0xFF202028))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Bill", color = CkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Includes tax & dojo protection", color = CkTextDim, fontSize = 10.sp)
            }
            Text(
                formatRupiah(finalTotal),
                color = CkTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF1C1822), Color(0xFF24171D))),
                    RoundedCornerShape(12.dp)
                )
                .border(1.dp, Color(0xFF3B232C), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = CkCoral, modifier = Modifier.size(15.dp))
                Text(
                    "+156 DP Points earned from this transaction",
                    color = CkTextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("Reward", color = CkAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, softWrap = false)
        }
    }
}

@Composable
private fun CkStickyBottomBar(
    totalAmount: Int,
    savingsAmount: Int,
    onPayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(CkBgDark)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1E1C24), Color(0xFF1A1720), Color(0xFF26151C))
                    ),
                    RoundedCornerShape(16.dp)
                )
                .border(1.dp, Color(0xFF442834), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(38.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CkRed, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingBag, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("Total Bill", color = CkTextMuted, fontSize = 10.sp, softWrap = false)
                        if (savingsAmount > 0) {
                            Box(
                                Modifier
                                    .background(Color(0xFF30161E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "Save ${savingsAmount / 1000}k",
                                    color = CkCoral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Text(
                        formatRupiah(totalAmount),
                        color = CkCoral,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false
                    )
                }
            }

            Button(
                onClick = onPayClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CkRed, contentColor = Color.White)
            ) {
                Text("Pay Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, softWrap = false)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(15.dp))
            }
        }
    }
}

internal fun formatRupiah(amount: Int): String {
    val formatted = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp $formatted"
}