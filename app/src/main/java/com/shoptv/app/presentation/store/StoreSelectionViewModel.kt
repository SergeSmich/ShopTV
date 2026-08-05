package com.shoptv.app.presentation.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoptv.core.common.Result
import com.shoptv.feature.magnit.data.MagnitRepository
import com.shoptv.feature.magnit.data.StoreDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreSelectionViewModel(
    private val magnitRepository: MagnitRepository
) : ViewModel() {

    private val _stores = MutableStateFlow<List<StoreDetail>>(emptyList())
    val stores: StateFlow<List<StoreDetail>> = _stores.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null

    fun searchStores(query: String = "") {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null

            if (query.isNotBlank()) delay(300) // debounce

            when (val result = magnitRepository.searchStores(
                query = query.ifBlank { null },
                storeType = DEFAULT_STORE_TYPE
            )) {
                is Result.Success -> _stores.value = result.data
                is Result.Error -> {
                    _stores.value = emptyList()
                    _error.value = result.message
                }
            }

            _loading.value = false
        }
    }

    fun loadDefaultStores() {
        searchStores()
    }

    companion object {
        private const val DEFAULT_STORE_TYPE = "express"
    }
}
