package com.shoptv.feature.magnit.data

import android.util.Log
import com.shoptv.core.common.Result
import com.shoptv.core.model.StoreType
import com.shoptv.core.model.UnifiedProduct
import com.shoptv.feature.magnit.data.local.CatalogRow
import com.shoptv.feature.magnit.data.local.MagnitLocalCatalog
import retrofit2.HttpException
import java.io.IOException

/**
 * Репозиторий каталога Магнита.
 *
 * Использует реальный API из декомпиляции: POST /v2/goods/get
 * При недоступности API переключается на offline-слепок из assets.
 */
class MagnitRepository(
    private val localCatalog: MagnitLocalCatalog,
    private val catalogApi: MagnitCatalogApi,
    private val cartApi: MagnitCartApi? = null,
    private val storesApi: MagnitStoresApi? = null
) {

    // ════════════════════════════════════════════════════════════════════
    // КАТАЛОГ
    // ════════════════════════════════════════════════════════════════════

    /**
     * Каталог строками для TV-экрана.
     *
     * Стратегия: сначала offline, потом пробуем API.
     */
    suspend fun getCatalogRows(
        storeCode: String = DEFAULT_STORE,
        storeType: String = DEFAULT_STORE_TYPE
    ): Result<List<CatalogRow>> {
        // Сразу пробуем API (с коротким таймаутом)
        when (val remote = getProducts(storeCode, storeType)) {
            is Result.Success -> {
                if (remote.data.isNotEmpty()) {
                    Log.i(TAG, "Каталог из API: ${remote.data.size} товаров")
                    return Result.Success(groupByCategory(remote.data))
                }
            }
            is Result.Error -> {
                Log.w(TAG, "API: ${remote.message}")
            }
        }

        // Фолбэк на offline
        val local = localCatalog.getRows()
        return if (local.isEmpty()) {
            Result.Error("Offline-каталог пуст")
        } else {
            Log.i(TAG, "Каталог из offline: ${local.sumOf { it.products.size }} товаров")
            Result.Success(local)
        }
    }

    /**
     * Загрузка товаров через реальный API.
     * POST /v2/goods/get
     */
    suspend fun getProducts(
        storeCode: String,
        storeType: String = DEFAULT_STORE_TYPE,
        catalogType: String = DEFAULT_CATALOG_TYPE,
        limit: Int = PAGE_SIZE,
        offset: Int = 0
    ): Result<List<UnifiedProduct>> = try {
        val request = MagnitGetGoodsRequest(
            catalogType = catalogType,
            pagination = MagnitPagination(limit = limit, offset = offset),
            storeCode = storeCode,
            storeType = storeType
        )
        Log.i(TAG, "API запрос: storeCode=$storeCode, storeType=$storeType, limit=$limit, offset=$offset")
        val response = catalogApi.getGoods(
            headers = MagnitHeaders.default(),
            request = request
        )
        val items = response.items
        Log.i(TAG, "API ответ: ${items?.size ?: 0} товаров")
        val products = items?.mapNotNull { it.toUnified() } ?: emptyList()
        Result.Success(products)
    } catch (e: IOException) {
        Log.e(TAG, "API ошибка сети: ${e.message}")
        Result.Error("Ошибка сети: ${e.message}", e)
    } catch (e: HttpException) {
        Log.e(TAG, "API ошибка сервера: ${e.code()}")
        Result.Error("Ошибка сервера: ${e.code()}", e)
    } catch (e: Exception) {
        Log.e(TAG, "API ошибка: ${e.message}")
        Result.Error("Ошибка: ${e.message}", e)
    }

    /**
     * Постраничная загрузка всего каталога.
     */
    suspend fun getAllProducts(
        storeCode: String,
        storeType: String = DEFAULT_STORE_TYPE
    ): Result<List<UnifiedProduct>> {
        val allProducts = mutableListOf<UnifiedProduct>()
        var offset = 0

        while (true) {
            when (val result = getProducts(storeCode, storeType, offset = offset)) {
                is Result.Success -> {
                    allProducts.addAll(result.data)
                    if (result.data.size < PAGE_SIZE) break
                    offset += PAGE_SIZE
                    if (offset > MAX_OFFSET) break
                }
                is Result.Error -> {
                    if (allProducts.isEmpty()) return result
                    break
                }
            }
        }

        return Result.Success(allProducts)
    }

    // ════════════════════════════════════════════════════════════════════
    // ПОИСК (локальный)
    // ════════════════════════════════════════════════════════════════════

    suspend fun search(query: String, limit: Int = 60): List<UnifiedProduct> {
        val needle = query.trim().lowercase()
        if (needle.length < MIN_QUERY) return emptyList()

        val words = needle.split(' ').filter { it.isNotBlank() }

        return localCatalog.getAllProducts()
            .mapNotNull { product ->
                val title = product.title.lowercase()
                if (words.any { !title.contains(it) }) return@mapNotNull null

                val position = title.indexOf(words.first())
                val startsWord = position == 0 || title.getOrNull(position - 1) == ' '
                val rank = when {
                    position == 0 -> 0
                    startsWord -> 1
                    else -> 2
                }
                product to rank
            }
            .sortedWith(
                compareBy(
                    { it.second },
                    { -(it.first.discountPercent ?: 0) },
                    { it.first.title.length }
                )
            )
            .take(limit)
            .map { it.first }
    }

    // ════════════════════════════════════════════════════════════════════
    // КОРЗИНА
    // ════════════════════════════════════════════════════════════════════

    /**
     * Добавить товар в корзину по ID.
     * PUT /v2/carts/lite?storeCode=...&service=express
     */
    suspend fun addToCart(
        goodId: String,
        storeCode: String = DEFAULT_STORE
    ): Result<CartUpsertResponse> {
        val api = cartApi ?: return Result.Error("API корзины не настроен")
        return try {
            val request = CartHelper.addOne(goodId, goodId, storeCode)
            val response = api.updateCart(
                headers = MagnitHeaders.default(),
                storeCode = storeCode,
                service = "express",
                request = request
            )
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error("Ошибка корзины: ${e.message}", e)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // МАГАЗИНЫ
    // ════════════════════════════════════════════════════════════════════

    /**
     * Поиск магазинов по городу.
     * POST /v1/stores-facade/search/detail
     */
    suspend fun searchStores(
        cityFiasId: String? = null,
        query: String? = null,
        storeType: String? = null
    ): Result<List<StoreDetail>> {
        val api = storesApi ?: return Result.Error("API магазинов не настроен")
        return try {
            val request = StoresSearchRequest(
                filters = StoresFilters(
                    cityFiasId = cityFiasId,
                    query = query,
                    storeTypeListV2 = storeType?.let { listOf(it) }
                )
            )
            val response = api.searchStores(request)
            if (response.isSuccessful) {
                Result.Success(response.body()?.data ?: emptyList())
            } else {
                Result.Error("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Ошибка поиска магазинов: ${e.message}", e)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ════════════════════════════════════════════════════════════════════

    private fun MagnitGood.toUnified(): UnifiedProduct? {
        if (id.isBlank() || name.isBlank()) return null
        val priceValue = price ?: return null

        return UnifiedProduct(
            id = id,
            title = name,
            priceCurrent = priceValue / 100.0,
            priceOld = promotion.oldPrice?.let { it / 100.0 },
            imageUrl = gallery.firstOrNull { it.url.isNotBlank() }?.url,
            storeType = StoreType.MAGNIT,
            categoryName = catalogType,
            deepLink = "https://magnit.ru/product/${id}-${seoCode ?: ""}?shopCode=$storeCode&shopType=express"
        )
    }

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

    companion object {
        private const val TAG = "MagnitRepository"
        private const val DEFAULT_STORE = "781225"
        private const val DEFAULT_STORE_TYPE = "express"
        private const val DEFAULT_CATALOG_TYPE = "main"
        private const val PAGE_SIZE = 36
        private const val MAX_OFFSET = 1000
        private const val MIN_QUERY = 2
    }
}
