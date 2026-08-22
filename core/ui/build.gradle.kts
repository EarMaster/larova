plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "app.larova.core.ui"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            // Strings live here, not in :app — the screens are in commonMain of this module and
            // of :feature:*, which cannot see an Android R class. Same XML, same rules.
            api(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.larova.core.ui.resources"
    generateResClass = auto
}
