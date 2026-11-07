plugins {
    alias(libs.plugins.lucent.android.library)
    alias(libs.plugins.lucent.android.library.compose)
}

android {
    namespace = "app.lucent.firefly.core.common"
}

dependencies {
    api(libs.javax.inject)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
