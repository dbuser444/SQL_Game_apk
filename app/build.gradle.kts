// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Это не корневой файл, это файл модуля 'app'
plugins {
    // Стандартные плагины (должны быть здесь)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 1. ПРИМЕНЕНИЕ ПЛАГИНА GOOGLE SERVICES
    id("com.google.gms.google-services") // <<--- ДОБАВЛЕНО ЭТО
}

android {
    namespace = "com.example.sql_game" // Замените на ваше фактическое пространство имен
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

    // Стандартные зависимости AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // 2. ИМПОРТ BOM ДЛЯ УПРАВЛЕНИЯ ВЕРСИЯМИ FIREBASE
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // 3. ДОБАВЛЕНИЕ ЗАВИСИМОСТЕЙ FIREBASE
    implementation("com.google.firebase:firebase-auth") // Для аутентификации (login/register)
    implementation("com.google.firebase:firebase-firestore") // Для профилей, уроков, групп

    // Возможно, вам также нужна эта библиотека для использования расширений ktx в LiveData/ViewModel
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
