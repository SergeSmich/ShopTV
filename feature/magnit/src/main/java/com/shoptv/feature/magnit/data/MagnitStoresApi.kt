package com.shoptv.feature.magnit.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API поиска магазинов Магнита.
 *
 * POST /v1/stores-facade/search/detail — поиск магазинов
 * POST /v2/stores-facade/config/by-geo — конфиг по геолокации
 */
interface MagnitStoresApi {

    @POST("/v1/stores-facade/search/detail")
    suspend fun searchStores(
        @Body request: StoresSearchRequest
    ): Response<StoresSearchResponse>
}

// ──────────────────────────────────────────────────────────────────────
// Запрос: StoresFacadePostSearchDetailRequest
// ──────────────────────────────────────────────────────────────────────

@Serializable
data class StoresSearchRequest(
    @SerialName("filters") val filters: StoresFilters,
    @SerialName("pagination") val pagination: StoresPagination = StoresPagination(),
    @SerialName("sorting") val sorting: StoresSorting = StoresSorting()
)

/**
 * Фильтры: StoresFacadeFilters (8 полей из APK)
 *
 * Все поля опциональны — передаём только нужные.
 */
@Serializable
data class StoresFilters(
    @SerialName("OpenByWorkingTypes") val openByWorkingTypes: List<String>? = null,
    @SerialName("cityFiasId") val cityFiasId: String? = null,
    @SerialName("deliveryTypeList") val deliveryTypeList: List<String>? = null,
    @SerialName("favorites") val favorites: Boolean? = null,
    @SerialName("geo") val geo: StoresGeoFilter? = null,
    @SerialName("query") val query: String? = null,
    @SerialName("storeTypeList") val storeTypeList: List<String>? = null,
    @SerialName("storeTypeListV2") val storeTypeListV2: List<String>? = null
)

/**
 * Гео-фильтр: StoresFacadeGeoFilter
 *
 * TODO: точные поля из APK (StoresFacadeGeoFilter).
 * Ожидаемые: latitude, longitude, radius
 */
@Serializable
data class StoresGeoFilter(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("radius") val radius: Int? = null
)

@Serializable
data class StoresPagination(
    @SerialName("limit") val limit: Int = 20,
    @SerialName("offset") val offset: Int = 0
)

@Serializable
data class StoresSorting(
    @SerialName("field") val field: String = "distance",
    @SerialName("order") val order: String = "asc"
)

// ──────────────────────────────────────────────────────────────────────
// Ответ: StoresFacadePostSearchDetailReply
// ──────────────────────────────────────────────────────────────────────

@Serializable
data class StoresSearchResponse(
    @SerialName("data") val data: List<StoreDetail>? = null,
    @SerialName("totalCount") val totalCount: Int = 0
)

/**
 * Магазин: StoresFacadeStoreDetailItem
 *
 * TODO: точные поля из APK.
 * Ожидаемые: code, name, address, lat, lon, storeType, workSchedule
 */
@Serializable
data class StoreDetail(
    @SerialName("code") val code: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("storeType") val storeType: String? = null,
    @SerialName("cityName") val cityName: String? = null,
    @SerialName("workSchedule") val workSchedule: String? = null
)

// ──────────────────────────────────────────────────────────────────────
// API авторизации: POST /v1/auth/cross-token
// ──────────────────────────────────────────────────────────────────────

/**
 * Авторизация: CrossTokenService
 *
 * POST /v1/auth/cross-token — получение кросс-токена.
 * Параметров не принимает — авторизация через сессию/хедеры.
 *
 * Поток:
 * 1. POST /v1/auth/cross-token → CrossTokenResponse { token }
 * 2. Обмен token → accessToken + refreshToken
 * 3. Authorization: Bearer {accessToken}
 */
interface MagnitAuthApi {
    @POST("/v1/auth/cross-token")
    suspend fun getCrossToken(): Response<CrossTokenResponse>
}

/**
 * CrossTokenResponse — из APK
 * ru.tander.magnit.magnitid.crosstoken.data.model.remote.CrossTokenResponse
 */
@Serializable
data class CrossTokenResponse(
    @SerialName("token") val token: String = ""
)

/**
 * ExchangeTokenResponse — обмен токена
 * ru.tander.models.magnitid.AuthorizationExchangeCrossTokenResponse
 */
@Serializable
data class ExchangeTokenResponse(
    @SerialName("accessToken") val accessToken: String = "",
    @SerialName("refreshToken") val refreshToken: String = ""
)

/**
 * GenerateTokenResponse — генерация токена
 * ru.tander.models.magnitid.AuthorizationGenerateCrossTokenResponse
 */
@Serializable
data class GenerateTokenResponse(
    @SerialName("token") val token: String = ""
)
