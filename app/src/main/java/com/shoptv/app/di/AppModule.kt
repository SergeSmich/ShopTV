package com.shoptv.app.di

import com.shoptv.app.presentation.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { MainViewModel(magnitRepository = get()) }
}
