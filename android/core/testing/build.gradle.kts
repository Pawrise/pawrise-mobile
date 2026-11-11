// Faux dépôts partagés par les tests des modules feature et de l'app (testImplementation uniquement).
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pawrise.core.testing"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    api(project(":core:auth"))
    api(project(":core:pets"))
    api(libs.kotlinx.coroutines.test)
}
