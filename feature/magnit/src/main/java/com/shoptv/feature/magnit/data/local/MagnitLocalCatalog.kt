package com.shoptv.feature.magnit.data.local

import android.content.Context
import com.shoptv.core.model.StoreType
import com.shoptv.core.model.UnifiedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Offline-слепок каталога Магнита из assets.
 *
 * Используется как запасной источник, когда API недоступен: magnit.ru
 * закрыт антибот-защитой и часто не отвечает на запросы из эмулятора.
 * Данные собраны скриптом ParseMagnit/build_catalog.py.
 */
class MagnitLocalCatalog(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile
    private var cache: List<CatalogRow>? = null

    /** Каталог строками: "Скидки дня", "Молочное", ... */
    suspend fun getRows(): List<CatalogRow> = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }

        val rows = try {
            val text = context.assets.open(ASSET_NAME)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<LocalCatalog>(text).rows.map { row ->
                CatalogRow(
                    title = row.title,
                    products = row.items.map { it.toUnified() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        cache = rows
        rows
    }

    /** Плоский список всех товаров без разбиения на строки */
    suspend fun getAllProducts(): List<UnifiedProduct> =
        getRows().flatMap { it.products }.distinctBy { it.id.ifBlank { it.title } }

    private fun LocalProduct.toUnified(): UnifiedProduct = UnifiedProduct(
        id = id.ifBlank { title },
        title = title,
        priceCurrent = price,
        priceOld = oldPrice,
        imageUrl = imageUrl,
        storeType = StoreType.MAGNIT,
        categoryName = category,
        deepLink = detailUrl?.let {
            if (it.startsWith("http")) it else "https://magnit.ru$it"
        }
    )

    companion object {
        private const val ASSET_NAME = "magnit_catalog.json"
    }
}

/** Строка каталога с готовыми к показу товарами */
data class CatalogRow(
    val title: String,
    val products: List<UnifiedProduct>
)

@Serializable
private data class LocalCatalog(
    val shop: String = "",
    val totalItems: Int = 0,
    val rows: List<LocalRow> = emptyList()
)

@Serializable
private data class LocalRow(
    val title: String = "",
    val items: List<LocalProduct> = emptyList()
)

@Serializable
private data class LocalProduct(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val price: Double = 0.0,
    @SerialName("oldPrice") val oldPrice: Double? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("detailUrl") val detailUrl: String? = null
)
