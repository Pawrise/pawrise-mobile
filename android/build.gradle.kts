// Build Docker (compose.yaml, PAWRISE_DOCKER_BUILD=1) : sorties dans .docker-build/ pour ne pas
// mélanger ses chemins (/workspace/…) avec ceux d'Android Studio sur le poste.
if (providers.environmentVariable("PAWRISE_DOCKER_BUILD").isPresent) {
    allprojects {
        layout.buildDirectory.set(rootDir.resolve(".docker-build${path.replace(':', '/')}"))
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Tests de contrat (*ContractTest) : -Ppawrise.contractUrl=http://localhost:4010/ les active
// contre le mock Prism (api/openapi.yaml). Sans la propriété, ils sont ignorés.
subprojects {
    tasks.withType<Test>().configureEach {
        val contractUrl = providers.gradleProperty("pawrise.contractUrl")
        inputs.property("pawrise.contractUrl", contractUrl.orElse(""))
        contractUrl.orNull?.let { systemProperty("pawrise.contractUrl", it) }
    }
}
