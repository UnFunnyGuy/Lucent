plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.android.library.compose)
}

android {
    namespace = "app.lucent.firefly.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
