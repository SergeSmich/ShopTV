package com.shoptv.feature.magnit.di

import com.shoptv.core.network.NetworkModule
import com.shoptv.feature.magnit.data.*
import com.shoptv.feature.magnit.data.local.MagnitLocalCatalog
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val MAGNIT_BASE_URL = "https://web-gateway.middle-api.magnit.ru/"

val magnitModule = module {

    // ─── Локальный каталог (offline-фолбэк) ───
    single { MagnitLocalCatalog(androidContext()) }

    // ─── Retrofit-клиент ───
    single {
        NetworkModule.createRetrofit(
            baseUrl = MAGNIT_BASE_URL,
            client = NetworkModule.createOkHttpClient()
        )
    }

    // ─── API-интерфейсы (из декомпиляции APK) ───
    single { get<retrofit2.Retrofit>().create(MagnitCatalogApi::class.java) }
    single { get<retrofit2.Retrofit>().create(MagnitCartApi::class.java) }
    single { get<retrofit2.Retrofit>().create(MagnitStoresApi::class.java) }
    single { get<retrofit2.Retrofit>().create(MagnitAuthApi::class.java) }

    // ─── Репозиторий ───
    single {
        MagnitRepository(
            localCatalog = get(),
            catalogApi = get(),
            cartApi = get(),
            storesApi = get()
        )
    }
}
