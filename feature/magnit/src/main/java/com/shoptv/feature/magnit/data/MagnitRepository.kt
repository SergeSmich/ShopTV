package com.shoptv.feature.magnit.data

import com.shoptv.core.common.Result
import com.shoptv.core.model.StoreType
import com.shoptv.core.model.UnifiedProduct
import com.shoptv.core.network.NetworkModule
import retrofit2.HttpException
import java.io.IOException

class MagnitRepository(
    private val api: MagnitApiService = NetworkModule
        .createRetrofit(
            baseUrl = "https://web-gateway.middle-api.magnit.ru/",
            client = NetworkModule.createOkHttpClient()
        )
        .create(MagnitApiService::class.java)
) {

    suspend fun getProducts(storeCode: String, page: Int = 1): Result<List<UnifiedProduct>> = try {
        val request = MagnitGoodsRequest(
            storeCodes = listOf(storeCode),
            pagination = Pagination(number = page, size = 36),
            onlyDiscount = false
        )
        val response = api.getGoods(request)
        val products = response.goods?.mapNotNull { it.toUnified() } ?: emptyList()
        Result.Success(products)
    } catch (e: IOException) {
        Result.Error("Ошибка сети: ${e.message}", e)
    } catch (e: HttpException) {
        Result.Error("Ошибка сервера: ${e.code()}", e)
    } catch (e: Exception) {
        Result.Error("Неизвестная ошибка: ${e.message}", e)
    }

    private fun MagnitGood.toUnified(): UnifiedProduct? {
        if (id == null || title == null || price == null) return null
        return UnifiedProduct(
            id = id,
            title = title,
            priceCurrent = price,
            priceOld = oldPrice,
            imageUrl = images?.firstOrNull()?.url,
            storeType = StoreType.MAGNIT,
            categoryName = categoryName
        )
    }
}
