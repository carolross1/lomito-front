plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
   // id("androidx.navigation.safeargs.kotlin") version "2.7.7"
}

android {
    namespace = "com.lomito.seguro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lomito.seguro"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // ✅ Toma la IP del backend desde gradle.properties (una sola fuente)
        buildConfigField("String", "BACKEND_URL", "\"${project.findProperty("LOMITO_BACKEND_URL") ?: "http://10.31.0.55:3000"}\"")
        multiDexEnabled = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.gson)
    implementation(libs.glide)
    implementation(libs.datastore)
    implementation(libs.play.services.wearable)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
}