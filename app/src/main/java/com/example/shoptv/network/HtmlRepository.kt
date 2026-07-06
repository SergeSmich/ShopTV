package com.example.shoptv.network

import com.example.shoptv.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HtmlRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun fetchHtml(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = api.getHtml(url)
            if (response.isSuccessful) {
                response.body()?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductsFromPage(url: String): List<Product> = withContext(Dispatchers.IO) {
        val html = fetchHtml(url) ?: return@withContext emptyList()
        try {
            LentaParser.parseProducts(html, url)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

