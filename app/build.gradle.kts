import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // Navigation routes are @Serializable data objects, which is what makes them type-safe.
    alias(libs.plugins.kotlin.serialization)
    // Screenshot goldens and the store images. Contributes `recordRoborazziDebug` and
    // `verifyRoborazziDebug`; the tests themselves are ordinary JVM unit tests either way.
    alias(libs.plugins.roborazzi)
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
        versionCode = 6
        versionName = "0.3.2"
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
    val keystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull?.takeIf { it.isNotBlank() }
    val keystore = keystorePath?.let(::keystoreFile)

    // Set but not there is a mistake, and a different one from not set at all. Unset means a local
    // build by somebody without the key, which is fine and silent; set to a path with no file at it
    // means CI decoded the secret somewhere else, or somebody mistyped, and the only useful moment
    // to say so is now rather than after twenty minutes of R8.
    if (keystorePath != null && keystore == null) {
        error(
            "KEYSTORE_PATH is set to '$keystorePath' but there is no file there. Relative paths " +
                "resolve against the repository root ($rootDir), not this module. Unset it to " +
                "build unsigned — see docs/release-setup.md §1.",
        )
    }

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

    testOptions {
        unitTests {
            // Robolectric renders the real screens, so the unit tests need the real resources.
            // For Larova that means the *assets*: Compose Multiplatform compiles `strings.xml`
            // into `assets/composeResources/`, not into `res/`, so a screenshot taken without
            // this is a picture of the layout with no words in it.
            isIncludeAndroidResources = true
            all {
                // A full screen at 420dpi does not render in the 512 MB default heap.
                it.maxHeapSize = "2g"
            }
        }
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
        ?.let(::keystoreFile) != null

/**
 * The keystore named by `KEYSTORE_PATH`, or null if there is no file there.
 *
 * Relative to the **repository root**, which is the whole reason this function exists. `file(...)`
 * in a module's build script resolves against that module — `app/` — so `KEYSTORE_PATH=keystore.p12`
 * pointed at `app/keystore.p12` while CI had written the decoded key beside the settings file. The
 * build then signed nothing, said so in one line of a two-hundred-line log, and failed four minutes
 * later on an artifact name. `rootDir.resolve` takes an absolute path as it is and a relative one
 * from the root, which is what every caller means either way.
 */
fun keystoreFile(path: String): java.io.File? = rootDir.resolve(path).takeIf { it.isFile }


// ------------------------------------------------------------------ store and website images
//
// `StoreAssetTest` writes the listing screenshots straight into the fastlane layout, but it cannot
// finish the job: Roborazzi writes RGBA, Play refuses any image with an alpha channel, and neither
// `javax.imageio` nor `java.awt` is on an Android unit test's classpath. Both remaining steps
// therefore happen here, where the full JDK is.
//
// Paths are resolved inside the configuration block and captured as plain `File`s. They cannot be
// script-level `val`s: reading one from `doLast` captures the build script object itself, which the
// configuration cache refuses to serialise.
val finishStoreAssets = tasks.register("finishStoreAssets") {
    group = "publishing"
    description = "Converts the Play Store screenshots to 24-bit PNG and copies them to the " +
        "Pages site."

    // Every locale, not just the source one. `StoreAssetTest` writes one directory per locale it
    // has content for, and a locale added later must not need this file edited as well.
    val storeMetadata = rootProject.layout.projectDirectory
        .dir("fastlane/metadata/android").asFile
    val pagesScreenshots = rootProject.layout.projectDirectory
        .dir("docs/pages/assets/screenshots").asFile

    doLast {
        val localeImageDirs = storeMetadata.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.resolve("images") }
            ?.filter { it.isDirectory }
            .orEmpty()

        if (localeImageDirs.isEmpty()) {
            logger.lifecycle("No store images under $storeMetadata — nothing to finish.")
            return@doLast
        }

        var converted = 0
        localeImageDirs.asSequence()
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "png" }
            .forEach { file ->
                val source = ImageIO.read(file) ?: return@forEach
                // Composited onto the picture's own top-left pixel rather than a fixed colour.
                // Every screen here is drawn on an opaque Surface so nothing is actually
                // transparent and the fill never shows — but the night-mode screenshot has a
                // near-black background and the others a warm off-white, so a single hardcoded
                // colour would be the wrong one half the time the day that stops being true.
                val opaque = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
                opaque.createGraphics().apply {
                    // Imported at the top of the file on purpose: in a Kotlin DSL script a bare
                    // `java.awt.Color` parses as a property on the Java plugin extension, which is
                    // what `java` means here, and fails with "unresolved reference: awt".
                    color = Color(source.getRGB(0, 0), true)
                    fillRect(0, 0, source.width, source.height)
                    drawImage(source, 0, 0, null)
                    dispose()
                }
                ImageIO.write(opaque, "png", file)
                converted++
            }

        // The website is served from docs/pages only and cannot link into the fastlane tree, so it
        // needs its own copy. Same run, same renderer: the listing and the landing page can never
        // show a different version of the app from each other, or from the goldens.
        //
        // The English set alone. The site is written in English, so a German screenshot in the
        // middle of an English page would read as a mistake rather than as a translation.
        var copied = 0
        val phoneSet = storeMetadata.resolve("en-US/images/phoneScreenshots")
        if (phoneSet.isDirectory) {
            pagesScreenshots.mkdirs()
            phoneSet.listFiles { file -> file.extension == "png" }?.forEach { file ->
                file.copyTo(pagesScreenshots.resolve(file.name), overwrite = true)
                copied++
            }
        }

        logger.lifecycle("Converted $converted store screenshot(s); copied $copied to the site.")
    }
}

// Recording is the only thing that writes those files, so it is the only thing that has to finish
// the job. A record run that captured no store assets simply finds nothing to do.
tasks.matching { it.name == "recordRoborazziDebug" }.configureEach { finalizedBy(finishStoreAssets) }

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

    // Screenshot tests. They are plain unit tests: `captureRoboImage` is inert unless Roborazzi's
    // own tasks set its system property, so `./gradlew test` only checks that every screen still
    // composes — worth having on its own — and neither records nor compares a golden.
    //
    // `testDebug` rather than `test`, and `debugImplementation` for the manifest: the activity
    // `createAndroidComposeRule` launches is contributed to the *debug* merged manifest by
    // ui-test-manifest. Scoped this way, `testReleaseUnitTest` is empty rather than broken.
    debugImplementation(libs.compose.ui.test.manifest)
    testDebugImplementation(libs.junit)
    testDebugImplementation(libs.robolectric)
    testDebugImplementation(libs.roborazzi)
    testDebugImplementation(libs.roborazzi.compose)
    testDebugImplementation(libs.compose.ui.test.junit4)
    testDebugImplementation(libs.kotlinx.coroutines.test)
}
