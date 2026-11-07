plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.android.library.compose)
    alias(libs.plugins.lucent.koin)
}

android {
    namespace = "app.lucent.firefly.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    testImplementation(libs.junit)
}
