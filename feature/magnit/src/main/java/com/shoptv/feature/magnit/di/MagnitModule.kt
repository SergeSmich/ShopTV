package com.shoptv.feature.magnit.di

import com.shoptv.feature.magnit.data.MagnitRepository
import org.koin.dsl.module

val magnitModule = module {
    single { MagnitRepository() }
}
