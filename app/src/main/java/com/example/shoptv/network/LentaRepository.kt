package com.example.shoptv.network

import android.util.Log
import com.example.shoptv.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class LentaRepository(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun fetchCategoryHtml(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("DNT", "1")
            .header("Cache-Control", "max-age=0")
            .header("Upgrade-Insecure-Requests", "1")
            .build()
        client.newCall(req).execute().use { resp ->
            Log.d("ShopTV", "fetchCategoryHtml: url=$url, code=${resp.code}, isSuccessful=${resp.isSuccessful}")
            if (!resp.isSuccessful) {
                throw Exception("HTTP ${resp.code} for url=$url")
            }
            resp.body?.string() ?: ""
        }
    }

    suspend fun getProductsFromCategoryUrl(url: String): List<Product> = withContext(Dispatchers.IO) {
        val html = fetchCategoryHtml(url)
        LentaParser.parseProducts(html, url)
    }
}
