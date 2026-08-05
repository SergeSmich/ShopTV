package com.shoptv.feature.magnit.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

/**
 * Реальный API Магнита, извлечённый из декомпиляции APK 8.109.0.
 *
 * Эндпоинт: POST /v2/goods/get
 * Класс в APK: ru.tander.catalog.favorites.flow.data.service.FavoritesService
 * Модели: ru.tander.models.gateway.catalogbff
 *
 * Базовый URL: https://web-gateway.middle-api.magnit.ru/
 */
interface MagnitCatalogApi {

    @POST("/v2/goods/get")
    suspend fun getGoods(
        @HeaderMap headers: Map<String, String>,
        @Body request: MagnitGetGoodsRequest
    ): MagnitGetGoodsResponse
}

// ──────────────────────────────────────────────────────────────────────
// Запрос: CatalogBffGoodsListGetGoodsV2Request
// ──────────────────────────────────────────────────────────────────────

@Serializable
data class MagnitGetGoodsRequest(
    @SerialName("catalogType") val catalogType: String,
    @SerialName("pagination") val pagination: MagnitPagination,
    @SerialName("storeCode") val storeCode: String,
    @SerialName("storeType") val storeType: String,
    @SerialName("cityId") val cityId: String? = null,
    @SerialName("listId") val listId: Int? = null
)

/**
 * Пагинация: CatalogBffCatalogBFFLOPagination
 */
@Serializable
data class MagnitPagination(
    @SerialName("limit") val limit: Int = 36,
    @SerialName("offset") val offset: Int = 0
)

// ──────────────────────────────────────────────────────────────────────
// Ответ: CatalogBffGoodsListGetGoodsV2ResponseSuccess
// ──────────────────────────────────────────────────────────────────────

@Serializable
data class MagnitGetGoodsResponse(
    @SerialName("containerConfig") val containerConfig: MagnitContainerConfig? = null,
    @SerialName("items") val items: List<MagnitGood>? = null,
    @SerialName("pagination") val pagination: MagnitPaginationResponse? = null
)

/**
 * Конфиг контейнера: CatalogBffContainerConfig
 */
@Serializable
data class MagnitContainerConfig(
    @SerialName("title") val title: String? = null
)

/**
 * Пагинация ответа: CatalogBffGoodsListPaginationResponse
 *
 * (поля совпадают с CatalogBffCatalogBFFLOPaginationResponse)
 */
@Serializable
data class MagnitPaginationResponse(
    @SerialName("hasMore") val hasMore: Boolean = false,
    @SerialName("limit") val limit: Int = 0,
    @SerialName("offset") val offset: Int = 0,
    @SerialName("totalCount") val totalCount: Int = 0,
    @SerialName("nextOffset") val nextOffset: Int? = null
)

// ──────────────────────────────────────────────────────────────────────
// Хедеры (из декомпиляции)
// ──────────────────────────────────────────────────────────────────────

object MagnitHeaders {

    fun default(): Map<String, String> = mapOf(
        "x-device-id" to "tv-shop-001",
        "x-device-platform" to "Android",
        "x-app-version" to "8.109.0",
        "x-platform-version" to "33",
        "x-app-type" to "OMNI",
        "x-device-tag" to "x-device-tag",
        "Content-Type" to "application/json",
        "Accept" to "application/json"
    )

    fun withAuth(token: String): Map<String, String> =
        default() + ("authorization" to "Bearer $token")
}
