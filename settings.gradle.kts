pluginManagement {
    // Convention plugins live in the included build; every module applies them
    // by id (hermes.*) instead of repeating AGP/Kotlin config.
    includeBuild("build-logic")
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
}

dependencyResolutionManagement {
    // No module may declare its own repositories — one central list.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "hermes-app"

// Modules are added in the next commit (M0 step 2): :app, :core:model,
// :core:brain, :core:data, :core:ui.
