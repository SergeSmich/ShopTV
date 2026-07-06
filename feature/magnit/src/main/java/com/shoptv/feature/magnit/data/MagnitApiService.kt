package com.shoptv.feature.magnit.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MagnitApiService {

    @POST("v3/goods")
    suspend fun getGoods(
        @Body request: MagnitGoodsRequest,
        @Header("x-client-name") clientName: String = "magnit",
        @Header("x-device-platform") platform: String = "Web",
        @Header("x-device-id") deviceId: String = "tv-device-001",
        @Header("x-app-version") appVersion: String = "1.0.0"
    ): MagnitGoodsResponse
}

@Serializable
data class MagnitGoodsRequest(
    val categoryIDs: List<Int>? = null,
    val includeForAdults: Boolean = true,
    val onlyDiscount: Boolean = false,
    val order: String = "desc",
    val pagination: Pagination = Pagination(),
    val shopType: String = "1",
    val sortBy: String = "price",
    val storeCodes: List<String>
)

@Serializable
data class Pagination(
    val number: Int = 1,
    val size: Int = 36
)

@Serializable
data class MagnitGoodsResponse(
    val goods: List<MagnitGood>? = null
)

@Serializable
data class MagnitGood(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("oldPrice") val oldPrice: Double? = null,
    @SerialName("images") val images: List<MagnitImage>? = null,
    @SerialName("categoryName") val categoryName: String? = null
)

@Serializable
data class MagnitImage(
    @SerialName("url") val url: String? = null
)
