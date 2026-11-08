plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.koin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.lucent.firefly.data.auth.impl"
}

dependencies {
    implementation(project(":data:auth:api"))
    implementation(project(":core:common"))
    implementation(project(":core:security"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    // Ktor Client & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
}
