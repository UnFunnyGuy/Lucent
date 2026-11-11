plugins {
    alias(libs.plugins.lucent.android.feature.impl)
}

android {
    namespace = "app.lucent.firefly.feature.capture.impl"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":feature:capture:api"))
    implementation(project(":data:auth:api"))
    implementation(project(":data:accounts:api"))
    implementation(project(":data:transactions:api"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
