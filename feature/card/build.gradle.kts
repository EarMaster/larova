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
        namespace = "app.larova.feature.card"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:domain"))
            // Decoding a step picture is work for a background thread, not for the frame that is
            // trying to draw it.
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            // The player, and the view that draws it. Here rather than in :core:ui because a player
            // is not a design-system primitive: this is the only module with a screen that plays
            // anything, and iOS will bring AVPlayer beside it rather than through it.
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
