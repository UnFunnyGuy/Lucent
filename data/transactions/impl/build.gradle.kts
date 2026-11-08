plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.koin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.lucent.firefly.data.transactions.impl"
}

dependencies {
    implementation(project(":data:transactions:api"))
    implementation(project(":data:accounts:api"))
    implementation(project(":data:auth:api"))
    implementation(project(":data:sync:api"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // Ktor & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
}
