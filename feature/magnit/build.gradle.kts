plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shoptv.feature.magnit"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // api, а не implementation: типы Result, UnifiedProduct и CatalogRow
    // возвращаются наружу и должны быть видны в :app
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(project(":core:network"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.leanback)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil)
    implementation(libs.koin.android)
}
