package com.example.shoptv.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

import com.example.shoptv.model.CatalogResponse
import com.example.shoptv.model.CategoriesResponse

interface LentaApiService {
    @POST("/api-gateway/v1/catalog/items/collections")
    suspend fun getCatalogItems(
        @Body requestBody: RequestBody,
        @Header("Authorization") token: String
    ): CatalogResponse

    @GET("/api-gateway/v1/catalog/categories")
    suspend fun getCatalogCategories(
        @Query("timestamp") timestamp: Long
    ): CategoriesResponse
}
