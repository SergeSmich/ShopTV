package com.example.shoptv.model

data class Product(
    val id: String,
    val title: String,
    val price: String,
    val imageUrl: String? = null,
    val detailUrl: String? = null
)

