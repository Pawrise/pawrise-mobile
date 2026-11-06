plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pawrise.core.designsystem"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.material3)
    api(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
