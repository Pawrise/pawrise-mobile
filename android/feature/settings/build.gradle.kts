plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Pages publiques des CGU et de la politique de confidentialité (Paramètres → Légal).
// Vides tant qu'elles ne sont pas publiées : la ligne l'indique au lieu d'ouvrir une page vide.
// `-Ppawrise.termsUrl=https://…` / `-Ppawrise.privacyUrl=https://…` ou ~/.gradle/gradle.properties.
val termsUrl = providers.gradleProperty("pawrise.termsUrl").getOrElse("")
val privacyUrl = providers.gradleProperty("pawrise.privacyUrl").getOrElse("")

android {
    namespace = "com.pawrise.feature.settings"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "TERMS_URL", "\"$termsUrl\"")
        buildConfigField("String", "PRIVACY_URL", "\"$privacyUrl\"")
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

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose) // photo du chien
    implementation(libs.coil.network.okhttp) // photo du chien servie par l'API (http/https)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
