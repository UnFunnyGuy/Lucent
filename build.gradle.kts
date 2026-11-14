// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(rootProject.libs.versions.ktlint.get())
        }
        kotlinGradle {
            target("*.kts")
            ktlint(rootProject.libs.versions.ktlint.get())
        }
    }
}

tasks.register<Exec>("installGitHooks") {
    description = "Configures git to use the repository githooks directory."
    group = "help"
    commandLine("git", "config", "core.hooksPath", "githooks")
}
