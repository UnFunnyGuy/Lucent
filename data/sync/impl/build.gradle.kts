plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.koin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.lucent.firefly.data.sync.impl"
}

dependencies {
    implementation(project(":data:sync:api"))
    implementation(project(":data:auth:api"))
    implementation(project(":data:accounts:api"))
    implementation(project(":data:transactions:api"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.androidx.workmanager)

    // Serialization & Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.work.testing)
}
