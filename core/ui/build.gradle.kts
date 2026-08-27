plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        // Without this, no string in the app has any text.
        //
        // `com.android.kotlin.multiplatform.library` ships with Android resources switched off, so
        // the variant has no assets for anything to be added to. The Compose plugin puts the
        // compiled strings there — and does it through `variant.sources.assets?.…`, so with assets
        // absent it silently does nothing: the files are generated under build/, the accessors
        // compile, the build passes, and `stringResource` throws on the first screen because the
        // APK contains no `composeResources/` at all.
        //
        // Nothing catches this but running the app or opening the APK. Check
        // `assets/composeResources/**/strings.commonMain.cvr` is in there before trusting a build.
        androidResources.enable = true
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
