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
        maven { url = uri("https://jitpack.io") }

        // ===== REPOSITORY for local I2P AAR =====
        // flatDir for client-release.aar in app/libs
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "apo"
include(":app")
