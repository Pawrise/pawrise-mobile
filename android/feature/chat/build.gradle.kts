plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// pawrise-assistant (POST /v1/turns/stream, SSE). Par défaut : en local via `adb reverse tcp:8100 tcp:8100`.
// Surcharge : `-Ppawrise.assistantBaseUrl=https://…/` (et `-Ppawrise.assistantPetRef=…`).
val assistantBaseUrl = providers.gradleProperty("pawrise.assistantBaseUrl").getOrElse("http://localhost:8100/")
val assistantPetRef = providers.gradleProperty("pawrise.assistantPetRef").getOrElse("")

android {
    namespace = "com.pawrise.feature.chat"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "ASSISTANT_BASE_URL", "\"$assistantBaseUrl\"")
    }

    buildTypes {
        // Dev : chien de démo de pawrise-assistant tant que la Core API n'existe pas (comme iOS Debug).
        debug { buildConfigField("String", "ASSISTANT_PET_REF", "\"$assistantPetRef\"") }
        release { buildConfigField("String", "ASSISTANT_PET_REF", "\"\"") }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:pets"))
    implementation(project(":core:auth"))
    implementation(project(":core:network"))
    implementation(project(":feature:home")) // chien affiché, avatar, icônes et styles de la maquette

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    debugImplementation(libs.compose.ui.tooling)


    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
