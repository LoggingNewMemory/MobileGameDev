package amba.dojo.pulse.market

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CkBgDark       = Color(0xFF0F0F11)
private val CkBgCard       = Color(0xFF141419)
private val CkBorder       = Color(0xFF23232C)
private val CkRed          = Color(0xFFE63946)
private val CkTextPrimary  = Color.White
private val CkTextSec      = Color(0xFFD4D4D8)
private val CkTextMuted    = Color(0xFFA1A1AA)
private val CkAmber        = Color(0xFFFBBF24)

data class CartItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    var quantity: Int,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    onCheckout: () -> Unit
) {
    var cartItems by remember {
        mutableStateOf(
            listOf(
                CartItem("1", "DojoPulse Pro Striking Gloves 14oz", "Gloves", 689_000, 1, "BEST SELLER"),
                CartItem("2", "Smart Biometric Combat Wraps", "Wraps", 189_000, 1, "AI VISION MARKER")
            )
        )
    }

    val subtotal = cartItems.sumOf { it.price * it.quantity }
    val shipping = if (subtotal >= 500_000) 0 else 25_000
    val totalAmount = subtotal + shipping

    Scaffold(
        containerColor = CkBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Keranjang Belanja",
                        color = CkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = CkTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CkBgDark)
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    color = CkBgCard,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, CkBorder),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Pembayaran",
                                color = CkTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(2.dp))
                            val formattedTotal = "Rp %,d".format(totalAmount).replace(',', '.')
                            Text(
                                text = formattedTotal,
                                color = CkTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = onCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = CkRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "Checkout",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CkBgDark)
        ) {
            if (cartItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = CkTextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Keranjang Kamu Kosong",
                        color = CkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Yuk lengkapi perlengkapan latihan tempurmu di Dojo Market!",
                        color = CkTextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = CkRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Kembali ke Market", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Daftar Produk (${cartItems.sumOf { it.quantity }} item)",
                            color = CkTextSec,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { cartItems = emptyList() }) {
                            Text("Kosongkan", color = CkRed, fontSize = 12.sp)
                        }
                    }

                    cartItems.forEach { item ->
                        CartItemCard(
                            item = item,
                            onQuantityChanged = { newQty ->
                                cartItems = cartItems.map {
                                    if (it.id == item.id) it.copy(quantity = newQty) else it
                                }.filter { it.quantity > 0 }
                            },
                            onRemove = {
                                cartItems = cartItems.filter { it.id != item.id }
                            }
                        )
                    }

                    // Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CkBgCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Ringkasan Belanja",
                                color = CkTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = CkBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal (${cartItems.sumOf { it.quantity }} item)", color = CkTextMuted, fontSize = 13.sp)
                                val formattedSub = "Rp %,d".format(subtotal).replace(',', '.')
                                Text(formattedSub, color = CkTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimasi Ongkir", color = CkTextMuted, fontSize = 13.sp)
                                val formattedShip = if (shipping == 0) "GRATIS" else "Rp %,d".format(shipping).replace(',', '.')
                                Text(formattedShip, color = if (shipping == 0) Color(0xFF34D399) else CkTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = CkBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", color = CkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                val formattedTotal = "Rp %,d".format(totalAmount).replace(',', '.')
                                Text(formattedTotal, color = CkRed, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChanged: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CkBgCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.category.uppercase(),
                            color = CkTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (item.badge != null) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(CkAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(item.badge, color = CkAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.name,
                        color = CkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = CkTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedPrice = "Rp %,d".format(item.price).replace(',', '.')
                Text(
                    text = formattedPrice,
                    color = CkRed,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, CkBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChanged(item.quantity - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = CkTextSec, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "${item.quantity}",
                        color = CkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { onQuantityChanged(item.quantity + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = CkTextSec, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
