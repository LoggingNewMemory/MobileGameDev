package amba.dojo.pulse.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import amba.dojo.pulse.market.data.Product
import amba.dojo.pulse.ui.theme.*

@Composable
fun MarketScreen(
    viewModel: MarketViewModel = viewModel(),
    onNavigate: (String) -> Unit = {},
    onOpenCart: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All") products
        else products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Dojo.Bg,
        bottomBar = { DojoBottomBar("market", onNavigate) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { TopSection() }

                item {
                    CategoryPills(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                item { PromoBanner() }

                item {
                    Text(
                        text = "OFFICIAL STRIKING GEAR",
                        color = Dojo.Text,
                        fontFamily = Dojo.Mono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(filteredProducts) { product ->
                    ProductCard(product = product)
                }

                item { MemberBenefits() }
            }

            CartPreview(
                onCheckoutClick = onOpenCart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun TopSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Dojo Market", color = Dojo.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            MonoLabel("Official Equipment & Telemetry Gear", size = 10)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBadge(Icons.Default.Search, size = 36.dp)
        }
    }
}

@Composable
fun CategoryPills(
    selectedCategory: String = "All",
    onCategorySelected: (String) -> Unit = {}
) {
    val categories = listOf("All", "Gloves", "Wraps", "Apparel", "Protection")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            DojoChip(
                text = cat,
                selected = cat.equals(selectedCategory, ignoreCase = true),
                onClick = { onCategorySelected(cat) }
            )
        }
    }
}

@Composable
fun PromoBanner() {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF0A2A3B), Color(0xFF060A12))))
            .border(1.dp, Dojo.Red.copy(alpha = 0.5f), shape)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DojoTag("SPECIAL OFFER", color = Dojo.Red)
                MonoLabel("20% OFF", size = 10, color = Dojo.Amber, bold = true)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "AI VISION MARKER GEAR",
                color = Dojo.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            MonoLabel(
                text = "Upgrade combat wraps with embedded vision tracking markers.",
                size = 10,
                color = Dojo.Muted
            )
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    DojoCard(padding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel(product.category.uppercase(), size = 9, color = Dojo.Muted)
                    if (product.badge != null) {
                        Spacer(Modifier.width(8.dp))
                        DojoTag(product.badge, color = Dojo.Amber)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.name,
                    color = Dojo.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Dojo.Amber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    MonoLabel("${product.rating} (${product.reviewsCount})", size = 9)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val formattedPrice = "Rp %,d".format(product.price.toLong()).replace(',', '.')
                    Text(
                        text = formattedPrice,
                        color = Dojo.Red,
                        fontFamily = Dojo.Mono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.originalPrice != null) {
                        Spacer(Modifier.width(8.dp))
                        val formattedOrig = "Rp %,d".format(product.originalPrice.toLong()).replace(',', '.')
                        Text(
                            text = formattedOrig,
                            color = Dojo.Muted,
                            fontFamily = Dojo.Mono,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Dojo.Red)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = "Add to Cart",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MemberBenefits() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoLabel("MEMBER ADVANTAGES", size = 10, bold = true)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BenefitItem(
                icon = Icons.Default.Verified,
                title = "AI Calibrated",
                subtitle = "Telemetry ready",
                modifier = Modifier.weight(1f)
            )
            BenefitItem(
                icon = Icons.Default.Shield,
                title = "Pro Grade",
                subtitle = "Impact tested",
                modifier = Modifier.weight(1f)
            )
            BenefitItem(
                icon = Icons.Default.LocalShipping,
                title = "Fast Ship",
                subtitle = "Free > Rp 500k",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    DojoCard(modifier = modifier, padding = 10.dp) {
        Icon(icon, contentDescription = null, tint = Dojo.Red, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, color = Dojo.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        MonoLabel(subtitle, size = 8)
    }
}

@Composable
fun CartPreview(
    onCheckoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Dojo.Surface)
            .border(1.dp, Dojo.Red, shape)
            .clickable { onCheckoutClick() }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Dojo.Red,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Shopping Cart",
                color = Dojo.Text,
                fontFamily = Dojo.Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = onCheckoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = Dojo.Red),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "CHECKOUT",
                color = Color.White,
                fontFamily = Dojo.Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}