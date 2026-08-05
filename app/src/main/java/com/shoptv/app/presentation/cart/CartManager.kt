package com.shoptv.app.presentation.cart

import com.shoptv.core.model.UnifiedProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Локальный менеджер корзины.
 *
 * Хранит товары в памяти.
 * Для persistence можно добавить Room или SharedPreferences.
 */
object CartManager {

    private val items = mutableListOf<UnifiedProduct>()

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    fun add(product: UnifiedProduct) {
        // Проверяем дубликаты по ID
        if (items.none { it.id == product.id }) {
            items.add(product)
            updateState()
        }
    }

    fun remove(product: UnifiedProduct) {
        items.removeAll { it.id == product.id }
        updateState()
    }

    fun getItems(): List<UnifiedProduct> = items.toList()

    fun clear() {
        items.clear()
        updateState()
    }

    private fun updateState() {
        _count.value = items.size
        _total.value = items.sumOf { it.priceCurrent }
    }
}
