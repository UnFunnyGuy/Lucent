plugins {
    alias(libs.plugins.lucent.android.feature.api)
}

android {
    namespace = "app.lucent.firefly.feature.capture.api"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
}
