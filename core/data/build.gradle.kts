plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "app.larova.core.data"
        compileSdk = providers.gradleProperty("larova.compileSdk").get().toInt()
        minSdk = providers.gradleProperty("larova.minSdk").get().toInt()
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(project(":core:platform"))
            // api, not implementation: the composition root in :app names LarovaDatabase and
            // DataStore when wiring them, and LarovaDatabase's supertype is Room's.
            api(libs.androidx.room.runtime)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        // The migration test, and only the migration test. It needs a real SQLite file and Room's
        // own schema validator, neither of which exists in commonTest — and a migration is the one
        // thing in this module that a fake cannot prove anything about.
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.sqlite.bundled)
            // The JVM artifact explicitly: a host test runs on the desktop JVM, and the
            // Android variant of sqlite-bundled carries native libraries for Android ABIs
            // only, so the driver loads and then cannot find its own engine.
            implementation("androidx.sqlite:sqlite-bundled-jvm:2.7.0")
        }
    }
}

// Schema JSON is committed. The export container and the database are the same compatibility
// story (docs/technical-notes.md §6): a migration nobody can reconstruct is a family's data lost.
room {
    schemaDirectory("$projectDir/schemas")
}

// The migration test builds its "before" database from the committed schema JSON, so it has to be
// told where that is. A system property rather than a working-directory guess: the test's working
// directory is Gradle's business and has changed before.
tasks.withType<Test>().configureEach {
    systemProperty("larova.schemaDir", "$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

// KSP registers its generated directories as sources for every compilation of the module,
// including the host-test one where no processor runs. AGP's lint tasks then read those
// directories without declaring the dependency, and Gradle fails the build rather than risk an
// ordering bug. Wiring it explicitly is preferable to switching the validation off.
tasks.matching { it.name.startsWith("lintAnalyze") || it.name.endsWith("LintModel") }
    .configureEach {
        dependsOn(tasks.matching { it.name.startsWith("ksp") })
    }
