rootProject.name = "WakeyWakey"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")
include(":apps:android")
include(":apps:windows")
// include(":apps:ios")    // Fase 8
// include(":apps:macos")  // Fase 8
