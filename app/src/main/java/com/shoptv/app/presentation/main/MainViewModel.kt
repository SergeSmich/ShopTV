package com.shoptv.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoptv.core.common.Result
import com.shoptv.feature.magnit.data.MagnitRepository
import com.shoptv.feature.magnit.data.local.CatalogRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val magnitRepository: MagnitRepository
) : ViewModel() {

    private val _rows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val rows: StateFlow<List<CatalogRow>> = _rows.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMagnitCatalog(storeCode: String = DEFAULT_STORE) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            when (val result = magnitRepository.getCatalogRows(storeCode)) {
                is Result.Success -> _rows.value = result.data
                is Result.Error -> {
                    _rows.value = emptyList()
                    _error.value = result.message
                }
            }

            _loading.value = false
        }
    }

    companion object {
        private const val DEFAULT_STORE = "543358"
    }
}
