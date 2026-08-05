package com.shoptv.feature.magnit.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.*

/**
 * API корзины Магнита.
 *
 * PUT /v2/carts/lite — обновить/создать корзину
 * DELETE /v1/carts/{cart_id} — удалить корзину
 *
 * Хедеры: x-client-name, x-device-id, Authorization
 */
interface MagnitCartApi {

    /** Добавить/обновить товары в корзине */
    @PUT("/v2/carts/lite")
    suspend fun updateCart(
        @HeaderMap headers: Map<String, String>,
        @Query("storeCode") storeCode: String,
        @Query("service") service: String,
        @Query("vendorChainID") vendorChainID: String? = null,
        @Body request: CartUpsertRequest
    ): CartUpsertResponse

    /** Удалить корзину */
    @DELETE("/v1/carts/{cart_id}")
    suspend fun deleteCart(
        @HeaderMap headers: Map<String, String>,
        @Path("cart_id") cartId: String
    ): retrofit2.Response<Unit>
}

// ──────────────────────────────────────────────────────────────────────
// Запрос: CartsUpsertLiteCartRequest
// ──────────────────────────────────────────────────────────────────────

@Serializable
data class CartUpsertRequest(
    @SerialName("items") val items: List<CartItem>
)

/**
 * Товар в корзине: CartsDtoLiteCartItem
 *
 * Обязательные: goodId, offerId, qnty
 * Остальные опциональны — для TV хватает минимального набора.
 */
@Serializable
data class CartItem(
    @SerialName("goodId") val goodId: String,
    @SerialName("offerId") val offerId: String,
    @SerialName("qnty") val qnty: Int,
    @SerialName("catalogPrice") val catalogPrice: Int? = null,
    @SerialName("goodStoreCode") val goodStoreCode: String? = null,
    @SerialName("goodService") val goodService: CartServiceType? = null,
    @SerialName("createdFromScreen") val createdFromScreen: String? = null,
    @SerialName("operationType") val operationType: String? = null
)

/**
 * Тип сервиса: CartsDtoServiceEnum
 *
 * Значения из APK ru.tander.models.gateway.carts.CartsDtoServiceEnum
 */
@Serializable
enum class CartServiceType(val value: String) {
    @SerialName("cosmetic") COSMETIC("cosmetic"),
    @SerialName("cosmetic_darkstore") COSMETIC_DARKSTORE("cosmetic_darkstore"),
    @SerialName("dostavka") DOSTAVKA("dostavka"),
    @SerialName("express") EXPRESS("express"),
    @SerialName("apteka") APTEKA("apteka"),
    @SerialName("market") MARKET("market"),
    @SerialName("rte") RTE("rte"),
    @SerialName("undecodable") UNDECODABLE("undecodable");

    override fun toString(): String = value
}

// ──────────────────────────────────────────────────────────────────────
// Ответ: CartsGetLiteCartsResponse (заглушка)
// ──────────────────────────────────────────────────────────────────────

/**
 * Ответ на обновление корзины.
 *
 * TODO: точная структура CartsGetLiteCartsResponse из APK.
 * Ожидаемая: { cartId, items, totalPrice, ... }
 */
@Serializable
data class CartUpsertResponse(
    @SerialName("cartId") val cartId: String? = null,
    @SerialName("items") val items: List<CartItem>? = null,
    @SerialName("totalPrice") val totalPrice: Int? = null,
    @SerialName("totalCount") val totalCount: Int? = null
)

// ──────────────────────────────────────────────────────────────────────
// Хелперы
// ──────────────────────────────────────────────────────────────────────

object CartHelper {

    /** Создать CartItem из MagnitGood */
    fun fromGood(good: MagnitGood, storeCode: String): CartItem = CartItem(
        goodId = good.id,
        offerId = good.productId ?: good.id,
        qnty = 1,
        catalogPrice = good.price,
        goodStoreCode = storeCode,
        goodService = CartServiceType.EXPRESS,
        createdFromScreen = "tv_catalog"
    )

    /** Минимальный запрос для добавления одного товара */
    fun addOne(goodId: String, offerId: String, storeCode: String): CartUpsertRequest =
        CartUpsertRequest(
            items = listOf(
                CartItem(
                    goodId = goodId,
                    offerId = offerId,
                    qnty = 1,
                    goodStoreCode = storeCode,
                    goodService = CartServiceType.EXPRESS,
                    createdFromScreen = "tv_catalog"
                )
            )
        )
}
