plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

// The only module that knows Google Play Billing exists.
//
// Kept apart from everything else for three reasons that all point the same way: the `foss` flavour
// must be able to leave it out entirely, the AGPL additional permission for linking a proprietary
// library stays scoped to one module rather than to the whole app (see LICENCE), and the iOS
// milestone gets a StoreKit sibling instead of an #if.
//
// Android-only on purpose: there is no commonMain here, because there is nothing platform-free left
// once the store is the subject. The domain interface it implements lives in :core:domain.
kotlin {
    android {
        namespace = "app.larova.core.billing"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        // No generated accessor exists for this one, so it is named. The verifier tests are plain
        // JVM tests: java.security is the real thing on the host, so no Robolectric is needed.
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)

            // The datatransport exclusions are the whole reason this is spelled out rather than a
            // one-liner. billing:9.1.0 depends on com.google.android.datatransport, whose manifest
            // declares INTERNET and ACCESS_NETWORK_STATE and whose job is uploading telemetry to
            // Google. AGENTS.md invariant 6 is "no analytics, no crash reporter, no network
            // dependency", so the classes are kept out of the APK rather than merely denied a
            // permission — see docs/technical-notes.md §7.
            //
            // play-services-location comes along for nothing this app does and is dropped with it.
            //
            // If the purchase flow ever fails with NoClassDefFoundError pointing at
            // com.google.android.datatransport, that is this block. Put the exclusion back one
            // artifact at a time with the reason written next to it, and re-check the merged
            // manifest; do not delete the block wholesale.
            implementation(libs.billing)
        }
    }
}

// The exclusions are applied here rather than on the dependency itself, because a KMP source-set
// `implementation(...)` of a version-catalog entry takes no configuration block.
//
// billing:9.1.0 depends on com.google.android.datatransport, whose manifest declares INTERNET and
// ACCESS_NETWORK_STATE and whose job is uploading telemetry to Google. AGENTS.md invariant 6 is
// "no analytics, no crash reporter, no network dependency", so the classes are kept out of the
// build rather than merely denied a permission. play-services-location comes along for nothing
// this app does and goes with it.
//
// If the purchase flow ever fails with NoClassDefFoundError naming
// com.google.android.datatransport, this block is why. Restore one artifact at a time with the
// reason written beside it and re-check the merged manifest; do not delete the block wholesale.
configurations.configureEach {
    exclude(group = "com.google.android.datatransport")
    exclude(group = "com.google.android.gms", module = "play-services-location")
}
