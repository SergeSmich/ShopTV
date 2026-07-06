package com.example.shoptv.network

import com.example.shoptv.model.Product
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class CatalogRepository(private val apiService: LentaApiService) {
    suspend fun fetchCatalogItems(token: String): List<Product> {
        val requestBody = """{"pagination": {"offset": 0, "limit": 24}}"""
            .toRequestBody("application/json".toMediaType())
        val response = apiService.getCatalogItems(requestBody, "Bearer $token")
        return response.items
    }
}
