// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Плагин Android Application с указанием версии
    id("com.android.application") version "8.9.0" apply false

    // Плагин Kotlin Android с указанием версии
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false

    // Плагин Google Services (Firebase) с указанием версии
    id("com.google.gms.google-services") version "4.4.4" apply false
}
