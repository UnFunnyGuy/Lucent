package app.lucent.firefly.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            dependencies {
                add("api", libs.findLibrary("room-runtime").get())
                add("api", libs.findLibrary("room-ktx").get())
                add("ksp", libs.findLibrary("room-compiler").get())
            }
        }
    }
}
