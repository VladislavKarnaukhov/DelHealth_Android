plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.delhealth.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.delhealth.app"
        minSdk = 28 // Health Connect SDK требует API 26+; 28 — разумный минимум для WebView
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Адрес сайта. Для проверки с локальным сервером: 10.0.2.2 — это localhost хоста в эмуляторе.
        buildConfigField("String", "SITE_URL", "\"https://vladkarnaukhov.pythonanywhere.com/\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")
}
