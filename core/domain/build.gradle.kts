plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// Models and use cases. No platform dependencies at all — that is the point of the module, and
// what makes the iOS milestone adapters rather than a rewrite.
kotlin {
    android {
        namespace = "app.larova.core.domain"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
