package com.example.shoptv.repository

import android.content.Context
import android.util.Log
import com.example.shoptv.model.Catalog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Каталог Магнита из локального слепка в assets.
 *
 * Файл magnit_catalog.json генерируется скриптом ParseMagnit/build_catalog.py
 * на основе выгрузок парсеров. Сеть не используется — magnit.ru блокирует
 * запросы из эмулятора, поэтому на этом этапе данные offline.
 */
class MagnitCatalogRepository(private val context: Context) {

    private val gson = Gson()

    @Volatile
    private var cached: Catalog? = null

    suspend fun getCatalog(): Catalog = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }

        val catalog = try {
            context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
                gson.fromJson(reader, Catalog::class.java)
            } ?: Catalog()
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось прочитать $ASSET_NAME", e)
            Catalog()
        }

        // отбрасываем пустые строки, чтобы на экране не было пустых полок
        val cleaned = catalog.copy(rows = catalog.rows.filter { it.items.isNotEmpty() })
        cached = cleaned
        cleaned
    }

    companion object {
        private const val TAG = "MagnitCatalogRepo"
        private const val ASSET_NAME = "magnit_catalog.json"
    }
}
