pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lucent"
include(":app")
include(":core:common")
include(":core:security")
include(":core:network")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:ui")
include(":core:navigation")

include(":data:auth:api")
include(":data:auth:impl")

include(":data:accounts:api")
include(":data:accounts:impl")

include(":data:transactions:api")
include(":data:transactions:impl")

include(":data:sync:api")
include(":data:sync:impl")

