pluginManagement {
    repositories {
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine")
            name = "Volcengine"
        }
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine")
            name = "Volcengine"
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "IceCrystalSnowGlobe"

include(":app")
