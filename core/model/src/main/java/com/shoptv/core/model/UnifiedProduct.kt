package com.shoptv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UnifiedProduct(
    val id: String,
    val title: String,
    val priceCurrent: Double,
    val priceOld: Double? = null,
    val imageUrl: String? = null,
    val storeType: StoreType,
    val categoryName: String? = null,
    val deepLink: String? = null
) {
    val discountPercent: Int?
        get() = if (priceOld != null && priceOld > priceCurrent) {
            ((priceOld - priceCurrent) / priceOld * 100).toInt()
        } else null

    val formattedPrice: String
        get() = "%.2f ₽".format(priceCurrent)
}
