pluginManagement {
    // Репозитории для поиска плагинов (ВАЖНО: Google() всегда должен быть первым для Android)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    // Объявление плагинов для использования в проекте
    plugins {
        // Стандартные плагины Android и Kotlin
        id("com.android.application") version "8.5.0" apply false // Обновлено до 8.5.0
        id("org.jetbrains.kotlin.android") version "2.0.0" apply false // Обновлено до 2.0.0

        // Плагин Firebase (Google Services)
        id("com.google.gms.google-services") version "4.4.1" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SQL_Game"
include(":app")