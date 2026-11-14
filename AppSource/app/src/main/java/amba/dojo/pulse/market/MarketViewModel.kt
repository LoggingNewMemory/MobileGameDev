package amba.dojo.pulse.market

import androidx.lifecycle.ViewModel
import amba.dojo.pulse.market.data.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MarketViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        val dummyData = listOf(
            Product("1", "DojoPulse Pro Striking Gloves 14oz", "Gloves", 689000.0, null, "url_gambar", 4.9, 142, "BEST SELLER"),
            Product("2", "Smart Biometric Combat Wraps", "Wraps", 189000.0, null, "url_gambar", 4.8, 89, "AI VISION MARKER")
        )
        _products.value = dummyData
    }
}
