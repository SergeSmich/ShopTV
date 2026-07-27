package com.shoptv.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoptv.core.model.UnifiedProduct
import com.shoptv.feature.magnit.data.MagnitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val magnitRepository: MagnitRepository
) : ViewModel() {

    private val _results = MutableStateFlow<List<UnifiedProduct>>(emptyList())
    val results: StateFlow<List<UnifiedProduct>> = _results.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Пульт выдаёт по букве за нажатие, поэтому ждём паузу в наборе
     * и только потом ищем — иначе перебираем каталог на каждый символ.
     */
    fun onQueryChanged(text: String) {
        _query.value = text
        searchJob?.cancel()

        if (text.isBlank()) {
            _results.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _results.value = magnitRepository.search(text)
        }
    }

    /** Пользователь нажал «Найти» — ищем сразу, без паузы */
    fun onQuerySubmitted(text: String) {
        _query.value = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _results.value = magnitRepository.search(text)
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}
