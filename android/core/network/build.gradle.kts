plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// URL de base de l'API (chemins du contrat relatifs : « auth/login »…). Prod : https://…/v1/ (convention 7).
// Par défaut : le mock Prism du docker compose, joint via `adb reverse tcp:4010 tcp:4010`.
// Surcharge : `./gradlew assembleDebug -Ppawrise.apiBaseUrl=https://…/` ou dans ~/.gradle/gradle.properties.
val apiBaseUrl = providers.gradleProperty("pawrise.apiBaseUrl").getOrElse("http://localhost:4010/")

android {
    namespace = "com.pawrise.core.network"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.retrofit)
    api(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
