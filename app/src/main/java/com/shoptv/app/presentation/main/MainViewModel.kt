package com.shoptv.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoptv.core.common.Result
import com.shoptv.core.model.UnifiedProduct
import com.shoptv.feature.magnit.data.MagnitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val magnitRepository: MagnitRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<UnifiedProduct>>(emptyList())
    val products: StateFlow<List<UnifiedProduct>> = _products

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadMagnitProducts(storeCode: String = "543358") {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = magnitRepository.getProducts(storeCode)) {
                is Result.Success -> _products.value = result.data
                is Result.Error -> _error.value = result.message
            }
            _loading.value = false
        }
    }
}
