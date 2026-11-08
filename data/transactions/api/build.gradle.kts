plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.koin)
}

android {
    namespace = "app.lucent.firefly.data.transactions.api"
}

dependencies {
    implementation(project(":core:common"))
    api(libs.androidx.paging.common)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
