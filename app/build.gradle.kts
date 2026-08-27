plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // Navigation routes are @Serializable data objects, which is what makes them type-safe.
    alias(libs.plugins.kotlin.serialization)
}

// The only Android-specific module, and the only one that is not multiplatform: there is no KMP
// equivalent of com.android.application. iOS (M5) gets its own entry point instead, which is why
// everything worth sharing lives in :core and :feature rather than here.
android {
    namespace = "app.larova"
    compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()

    defaultConfig {
        // Fixed for the lifetime of the app. `app.larova` is the reversed `larova.app`, so the
        // identifier is verifiably ours; it cannot change after the first Play release.
        applicationId = "app.larova"
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        targetSdk = providers.gradleProperty("larova.targetSdk").get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        // The two lines above are read by release.yml and google-play.yml with grep and sed, which
        // take the *first* match in this file. Keep them as plain assignments, and do not mention
        // either identifier in a comment above them.
    }

    // The upload key, from the environment — never from a file in the repository and never from
    // gradle.properties. `release.yml` decodes KEYSTORE_BASE64 into the runner's workspace and sets
    // these four; a local release build sets KEYSTORE_PATH to wherever the .p12 is kept. See
    // docs/release-setup.md §1.
    //
    // Absent, the release build is simply unsigned. That is deliberate and it is what the setup doc
    // asks for: `assembleRelease` on a fresh clone has to keep working for everyone who does not
    // hold the key, which is everyone but the maintainer. What must not happen is silence — an
    // unsigned AAB is refused by Play at the end of a ten-minute pipeline, so the build says so
    // while it is running.
    val keystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
    val keystore = keystorePath?.takeIf { it.isNotBlank() }?.let(::file)?.takeIf { it.isFile }

    signingConfigs {
        if (keystore != null) {
            create("upload") {
                storeFile = keystore
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
                // Named rather than inferred. keytool's modern default is PKCS12 and so is Java
                // 17's, but a build that signs or does not depending on the JDK it runs under is a
                // build that fails once, in CI, on a day somebody upgraded something else.
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        // Only for VERSION_NAME, which goes into every export manifest. Reading it from the build
        // rather than repeating the version in a constant that would drift.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/**
 * Says out loud when a release build is going out unsigned.
 *
 * Without this the only symptom is the artifact's name: AGP writes `app-release-unsigned.apk`
 * instead of `app-release.apk`, which fails `release.yml` at the step that renames it — a confusing
 * `mv: cannot stat` at the end of a pipeline that has already tagged the commit and pushed the tag.
 * An unsigned AAB is refused by Play for the same reason and just as late.
 */
tasks.register("reportSigning") {
    val signed = keystoreConfigured()
    doLast {
        if (signed) {
            logger.lifecycle("Release builds will be signed with the upload key from KEYSTORE_PATH.")
        } else {
            logger.warn(
                "Release builds will be UNSIGNED: KEYSTORE_PATH is unset or does not point at a " +
                    "file. Play refuses unsigned uploads — see docs/release-setup.md §1.",
            )
        }
    }
}

fun keystoreConfigured(): Boolean =
    providers.environmentVariable("KEYSTORE_PATH").orNull
        ?.takeIf { it.isNotBlank() }
        ?.let(::file)
        ?.isFile == true

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:platform"))

    implementation(project(":feature:home"))
    implementation(project(":feature:card"))
    implementation(project(":feature:help"))
    implementation(project(":feature:transfer"))
    implementation(project(":feature:settings"))

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    // ShortcutManagerCompat: the launcher shortcuts are built here, where the launcher icon and the
    // activity they point at both live.
    implementation(libs.androidx.core)
    // Named in the composition root, which owns the single instance for the preferences file.
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jetbrains.navigation.compose)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    testImplementation(libs.kotlin.test)
}
