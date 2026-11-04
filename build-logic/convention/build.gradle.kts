import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "app.lucent.firefly.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.koin.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.lucent.android.application.asProvider().get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = libs.plugins.lucent.android.application.compose.get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.lucent.android.library.asProvider().get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = libs.plugins.lucent.android.library.compose.get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeatureApi") {
            id = libs.plugins.lucent.android.feature.api.get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidFeatureApiConventionPlugin"
        }
        register("androidFeatureImpl") {
            id = libs.plugins.lucent.android.feature.impl.get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidFeatureImplConventionPlugin"
        }
        register("koin") {
            id = libs.plugins.lucent.koin.get().pluginId
            implementationClass = "app.lucent.firefly.convention.KoinConventionPlugin"
        }
        register("androidRoom") {
            id = libs.plugins.lucent.android.room.get().pluginId
            implementationClass = "app.lucent.firefly.convention.AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.lucent.jvm.library.get().pluginId
            implementationClass = "app.lucent.firefly.convention.JvmLibraryConventionPlugin"
        }
    }
}
