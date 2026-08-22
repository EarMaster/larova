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
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.androidx.datastore.preferences.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Schema JSON is committed. The export container and the database are the same compatibility
// story (docs/technical-notes.md §6): a migration nobody can reconstruct is a family's data lost.
room {
    schemaDirectory("$projectDir/schemas")
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
