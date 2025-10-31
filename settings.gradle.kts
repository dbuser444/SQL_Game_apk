pluginManagement {
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

    plugins {
        // Стандартные плагины Android и Kotlin
        id("com.android.application") version "8.9.0" apply false
        id("org.jetbrains.kotlin.android") version "1.8.10" apply false

        // Плагин Firebase (Google Services)
        // Он должен быть здесь, чтобы Gradle мог найти его.
        id("com.google.gms.google-services") version "4.4.0" apply false
    }
    // ==================================
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
