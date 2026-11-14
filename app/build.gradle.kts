plugins {
    alias(libs.plugins.lucent.android.application)
    alias(libs.plugins.lucent.android.application.compose)
    alias(libs.plugins.lucent.koin)
}

android {
    namespace = "app.lucent.firefly"

    defaultConfig {
        applicationId = "app.lucent.firefly"
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE_PATH") ?: System.getenv("KEYSTORE_FILE")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:security"))
    implementation(project(":core:network"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    implementation(project(":data:auth:api"))
    implementation(project(":data:auth:impl"))

    implementation(project(":data:accounts:api"))
    implementation(project(":data:accounts:impl"))

    implementation(project(":data:transactions:api"))
    implementation(project(":data:transactions:impl"))

    implementation(project(":data:sync:api"))
    implementation(project(":data:sync:impl"))

    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:onboarding:impl"))

    implementation(project(":feature:dashboard:api"))
    implementation(project(":feature:dashboard:impl"))

    implementation(project(":feature:capture:api"))
    implementation(project(":feature:capture:impl"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Dependency injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
