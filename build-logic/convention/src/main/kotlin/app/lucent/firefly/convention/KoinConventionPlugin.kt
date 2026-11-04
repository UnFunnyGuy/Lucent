package app.lucent.firefly.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "io.insert-koin.compiler.plugin")

            dependencies {
                add("implementation", platform(libs.findLibrary("koin-bom").get()))
                add("implementation", libs.findLibrary("koin-core").get())
                add("implementation", libs.findLibrary("koin-annotations").get())
            }
        }
    }
}
