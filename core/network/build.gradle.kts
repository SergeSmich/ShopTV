plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.shoptv.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
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
    api(project(":core:model"))
    api(libs.retrofit)
    api(libs.retrofit.converter.kotlinx)
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
