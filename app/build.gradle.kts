// В этом файле применяются плагины, которые были объявлены в build.gradle.kts (SQL_Game)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    // Необходимо указать, чтобы Gradle знал, что это Android-проект
    // Здесь мы явно объявляем все необходимые Android-конфигурации
    namespace = "com.example.sql_game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sql_game"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // --- СТАНДАРТНЫЕ ANDROID ЗАВИСИМОСТИ ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- FIREBASE (АУТЕНТИФИКАЦИЯ и FIRESTORE) ---
    // Используем BOM для управления версиями Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))

    // Firebase Authentication (Auth) - используем -ktx
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firebase Firestore (База данных) - используем -ktx
    implementation("com.google.firebase:firebase-firestore-ktx")

    // --- MVVM (LIFECYCLE) ЗАВИСИМОСТИ ---
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Coroutine Lifecycle Scopes (полезно для ViewModel)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // --- КОТЛИН (КОРУТИНЫ) ---
    // Базовые Coroutines для асинхронной работы
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- ТЕСТЫ ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}