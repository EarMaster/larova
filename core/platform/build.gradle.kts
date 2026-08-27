plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

// expect/actual only: paths, intents, pickers, zip, crypto. Everything the rest of the app must
// not know the platform of.
kotlin {
    android {
        namespace = "app.larova.core.platform"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            // For the content rules the platform side has to apply — what counts as a dialable
            // number, what counts as an openable address.
            implementation(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // Argon2id for the parent-view PIN, and for password-protected exports in M3.
            implementation(libs.argon2kt)
            // A picked photo carries its orientation in EXIF rather than in its pixels.
            implementation(libs.androidx.exifinterface)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
