package app.lucent.firefly.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "lucent.android.library")
            apply(plugin = "lucent.android.library.compose")
            apply(plugin = "lucent.koin")

            dependencies {
                // Navigation 3
                add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
                add("implementation", libs.findLibrary("androidx-navigation3-ui").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-navigation3").get())

                // Lifecycle & Compose
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("koin-compose-viewmodel").get())

                // Coroutines
                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())

                // Testing
                add("testImplementation", kotlin("test"))
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }
        }
    }
}
