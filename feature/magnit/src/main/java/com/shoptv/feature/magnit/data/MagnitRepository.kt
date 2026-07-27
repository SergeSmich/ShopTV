package com.shoptv.feature.magnit.data

import android.util.Log
import com.shoptv.core.common.Result
import com.shoptv.core.model.StoreType
import com.shoptv.core.model.UnifiedProduct
import com.shoptv.core.network.NetworkModule
import com.shoptv.feature.magnit.data.local.CatalogRow
import com.shoptv.feature.magnit.data.local.MagnitLocalCatalog
import retrofit2.HttpException
import java.io.IOException

class MagnitRepository(
    private val localCatalog: MagnitLocalCatalog,
    private val api: MagnitApiService = NetworkModule
        .createRetrofit(
            baseUrl = BASE_URL,
            client = NetworkModule.createOkHttpClient()
        )
        .create(MagnitApiService::class.java)
) {

    /**
     * Каталог строками. Сначала пробуем API, при неудаче отдаём offline-слепок.
     *
     * Магнит закрыт антибот-защитой (Group-IB), поэтому запрос из эмулятора
     * часто не проходит — без фолбэка экран остаётся пустым.
     */
    suspend fun getCatalogRows(storeCode: String = DEFAULT_STORE): Result<List<CatalogRow>> {
        when (val remote = getProducts(storeCode)) {
            is Result.Success -> {
                if (remote.data.isNotEmpty()) {
                    Log.i(TAG, "Каталог получен из API: ${remote.data.size} товаров")
                    return Result.Success(groupByCategory(remote.data))
                }
                Log.w(TAG, "API вернул пустой список, берём offline-слепок")
            }
            is Result.Error -> {
                Log.w(TAG, "API недоступен (${remote.message}), берём offline-слепок")
            }
        }

        val local = localCatalog.getRows()
        return if (local.isEmpty()) {
            Result.Error("Нет данных: API недоступен и offline-каталог пуст")
        } else {
            Log.i(TAG, "Каталог из offline-слепка: ${local.sumOf { it.products.size }} товаров")
            Result.Success(local)
        }
    }

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

    /** Товары из API приходят плоским списком — раскладываем на строки для экрана */
    private fun groupByCategory(products: List<UnifiedProduct>): List<CatalogRow> {
        val rows = mutableListOf<CatalogRow>()

        val discounts = products.filter { it.discountPercent != null }
            .sortedByDescending { it.discountPercent }
        if (discounts.isNotEmpty()) {
            rows.add(CatalogRow("Скидки дня", discounts))
        }

        products.groupBy { it.categoryName ?: "Каталог" }
            .toSortedMap()
            .forEach { (category, items) -> rows.add(CatalogRow(category, items)) }

        return rows
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

    companion object {
        private const val TAG = "MagnitRepository"
        private const val BASE_URL = "https://web-gateway.middle-api.magnit.ru/"
        private const val DEFAULT_STORE = "543358"
    }
}
