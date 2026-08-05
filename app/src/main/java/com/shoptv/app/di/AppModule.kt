package com.shoptv.app.di

import com.shoptv.app.presentation.main.MainViewModel
import com.shoptv.app.presentation.search.SearchViewModel
import com.shoptv.app.presentation.store.StoreSelectionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { MainViewModel(magnitRepository = get()) }
    viewModel { SearchViewModel(magnitRepository = get()) }
    viewModel { StoreSelectionViewModel(magnitRepository = get()) }
}
