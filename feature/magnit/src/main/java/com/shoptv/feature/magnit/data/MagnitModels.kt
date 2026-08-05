package com.shoptv.feature.magnit.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Модель товара из каталога Магнита.
 *
 * Источник: CatalogBffListingGood.kt из APK ru.tander.magnit 8.109.0
 * Пакет: ru.tander.models.gateway.catalogbff
 *
 * Все цены в КОПЕЙКАХ (Int). Для отображения делим на 100.
 */
@Serializable
data class MagnitGood(
    // ─── Обязательные поля ───
    @SerialName("gallery") val gallery: List<MagnitMedia> = emptyList(),
    @SerialName("id") val id: String = "",
    @SerialName("isBiometryRequired") val isBiometryRequired: Boolean = false,
    @SerialName("isForAdults") val isForAdults: Boolean = false,
    @SerialName("name") val name: String = "",
    @SerialName("promotion") val promotion: MagnitPromotion = MagnitPromotion(),
    @SerialName("quantity") val quantity: Int = 0,
    @SerialName("storeCode") val storeCode: String = "",
    @SerialName("weighted") val weighted: MagnitWeighted = MagnitWeighted(),

    // ─── Опциональные поля ───
    @SerialName("badges") val badges: List<MagnitBadge>? = null,
    @SerialName("cashback") val cashback: Int? = null,
    @SerialName("catalogType") val catalogType: String? = null,
    @SerialName("fbs") val fbs: Boolean? = null,
    @SerialName("isAdditionalActionRequired") val isAdditionalActionRequired: Boolean? = null,
    @SerialName("isInFavorites") val isInFavorites: Boolean? = null,
    @SerialName("isLowStock") val isLowStock: Boolean? = null,
    @SerialName("nearestDelivery") val nearestDelivery: String? = null,
    @SerialName("nearestDeliveryDate") val nearestDeliveryDate: String? = null,
    @SerialName("needPassport") val needPassport: Boolean? = null,
    @SerialName("orderProperties") val orderProperties: MagnitOrderProperties? = null,
    @SerialName("orders") val orders: Int? = null,
    @SerialName("pickupOnly") val pickupOnly: Boolean? = null,
    @SerialName("price") val price: Int? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("profit") val profit: Int? = null,
    @SerialName("promoTag") val promoTag: String? = null,
    @SerialName("ratings") val ratings: MagnitRatings? = null,
    @SerialName("seoCode") val seoCode: String? = null,
    @SerialName("service") val service: String? = null,
    @SerialName("serviceLabel") val serviceLabel: MagnitServiceLabel? = null,
    @SerialName("skuGroupId") val skuGroupId: String? = null,
    @SerialName("skuIds") val skuIds: List<String>? = null,
    @SerialName("targetCart") val targetCart: String? = null
) {
    /** Цена в рублях (из копеек) */
    val priceRubles: Double
        get() = (price ?: 0) / 100.0

    /** Старая цена в рублях (из promotion) */
    val oldPriceRubles: Double?
        get() = promotion.oldPrice?.let { it / 100.0 }

    /** Скидка в процентах (из API или считаем сами) */
    val discountPercent: Int?
        get() {
            // API может вернуть discountPercent напрямую
            promotion.discountPercent?.let { if (it > 0) return it }
            // Или считаем из oldPrice
            val old = promotion.oldPrice ?: return null
            val cur = price ?: return null
            return if (old > cur) ((old - cur) * 100 / old) else null
        }

    /** Форматированная цена */
    val formattedPrice: String
        get() = "%.2f ₽".format(priceRubles)

    /** Первое изображение */
    val imageUrl: String?
        get() = gallery.firstOrNull()?.url

    /** Deep link для открытия в приложении Магнита */
    val deepLink: String
        get() = "https://magnit.ru/product/${id}-${seoCode ?: ""}"
}

// ──────────────────────────────────────────────────────────────────────
// Вложенные модели (из APK)
// ──────────────────────────────────────────────────────────────────────

/**
 * Изображение: CatalogBffMedia
 *
 * type — тип контента (image, video)
 * url  — URL изображения
 * key  — ключ (опционально)
 */
@Serializable
data class MagnitMedia(
    @SerialName("type") val type: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("key") val key: String? = null
)

/**
 * Акция/скидка: CatalogBffPromotion
 *
 * Все цены в КОПЕЙКАХ (Int).
 * isPromotion — есть ли акция
 * discountPercent — скидка в процентах (из приложения)
 * oldPrice — старая цена в копейках
 * endDate — дата окончания акции
 * bidId / newBidId — внутренние ID для рекламы
 */
@Serializable
data class MagnitPromotion(
    @SerialName("isPromotion") val isPromotion: Boolean = false,
    @SerialName("bidId") val bidId: Int? = null,
    @SerialName("discountPercent") val discountPercent: Int? = null,
    @SerialName("endDate") val endDate: String? = null,
    @SerialName("newBidId") val newBidId: Int? = null,
    @SerialName("oldPrice") val oldPrice: Int? = null
)

/**
 * Весовой товар: CatalogBffWeighted
 */
@Serializable
data class MagnitWeighted(
    @SerialName("isWeighted") val isWeighted: Boolean = false,
    @SerialName("weight") val weight: Double? = null,
    @SerialName("unit") val unit: String? = null
)

/**
 * Бейдж: CatalogBffBadge
 */
@Serializable
data class MagnitBadge(
    @SerialName("type") val type: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("color") val color: String? = null
)

/**
 * Рейтинг: CatalogBffRatings
 */
@Serializable
data class MagnitRatings(
    @SerialName("rating") val rating: Double? = null,
    @SerialName("reviewsCount") val reviewsCount: Int? = null
)

/**
 * Свойства заказа: CatalogBffOrderProperties
 */
@Serializable
data class MagnitOrderProperties(
    @SerialName("maxQuantity") val maxQuantity: Int? = null,
    @SerialName("minQuantity") val minQuantity: Int? = null,
    @SerialName("step") val step: Int? = null
)

/**
 * Метка сервиса: CatalogBffServiceLabel
 */
@Serializable
data class MagnitServiceLabel(
    @SerialName("text") val text: String? = null,
    @SerialName("type") val type: String? = null
)
