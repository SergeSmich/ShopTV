package com.shoptv.app

import android.app.Application
import com.shoptv.app.di.appModule
import com.shoptv.feature.magnit.di.magnitModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ShopTvApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ShopTvApplication)
            modules(listOf(appModule, magnitModule))
        }
    }
}
