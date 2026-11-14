package amba.dojo.pulse.market.data

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val originalPrice: Double? = null,
    val imageUrl: String = "",
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val badge: String? = null
)
