plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Screens only. Navigation is owned by :app, so every screen here takes its transitions as
// callbacks and never knows where it sits in the graph.
kotlin {
    android {
        namespace = "app.larova.feature.settings"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:domain"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
