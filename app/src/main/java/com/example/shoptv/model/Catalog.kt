package com.example.shoptv.model

/**
 * Каталог магазина: набор строк (rows), каждая — заголовок и список товаров.
 * Соответствует структуре app/src/main/assets/magnit_catalog.json,
 * который генерирует ParseMagnit/build_catalog.py
 */
data class Catalog(
    val shop: String = "",
    val shopCode: String = "",
    val totalItems: Int = 0,
    val rows: List<CatalogRow> = emptyList()
)

data class CatalogRow(
    val title: String = "",
    val items: List<Product> = emptyList()
)
