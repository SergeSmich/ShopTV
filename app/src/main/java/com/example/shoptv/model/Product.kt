package com.example.shoptv.model

import com.google.gson.annotations.SerializedName

data class Product(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val price: Double = 0.0,
    @SerializedName("oldPrice")
    val oldPrice: Double? = null,
    @SerializedName("discountPercent")
    val discountPercent: Int = 0,
    val benefit: Double = 0.0,
    val imageUrl: String? = null,
    val detailUrl: String? = null,
    val rating: Double? = null,
    val quantity: Int = 0
) {
    val hasDiscount: Boolean get() = discountPercent > 0 && oldPrice != null

    /** "179,99 ₽" */
    val priceFormatted: String get() = formatPrice(price)

    /** "359,99 ₽" или null */
    val oldPriceFormatted: String? get() = oldPrice?.let { formatPrice(it) }

    /** "-50%" или null */
    val discountLabel: String? get() = if (hasDiscount) "-$discountPercent%" else null

    private fun formatPrice(value: Double): String {
        val rubles = value.toInt()
        val kopecks = Math.round((value - rubles) * 100).toInt()
        return if (kopecks == 0) "$rubles ₽" else String.format("%d,%02d ₽", rubles, kopecks)
    }

    /** Полная ссылка на товар на сайте Магнита */
    val fullUrl: String?
        get() = detailUrl?.let {
            if (it.startsWith("http")) it else "https://magnit.ru$it"
        }
}
