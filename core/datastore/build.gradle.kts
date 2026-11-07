plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.koin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.lucent.firefly.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    // DataStore 1.3.0 + Tink encryption
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.tink)
    implementation(libs.tink.android)

    // Kotlin Serialization (JSON)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
