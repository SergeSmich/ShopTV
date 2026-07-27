package com.shoptv.feature.magnit.di

import com.shoptv.feature.magnit.data.MagnitRepository
import com.shoptv.feature.magnit.data.local.MagnitLocalCatalog
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val magnitModule = module {
    single { MagnitLocalCatalog(androidContext()) }
    single { MagnitRepository(localCatalog = get()) }
}
