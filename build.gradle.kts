// Larova · root build.
//
// Plugins are declared here without being applied, so every module can pick what it needs from
// one pinned set. Two things are configured for all projects rather than repeated ten times:
// Detekt, and a `test` alias in the multiplatform modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // Detekt's own parser, not the Kotlin compiler's, so a Kotlin version it has not seen
        // is a warning rather than a build failure.
        ignoreFailures = false
    }

    // `ci.yml` runs `./gradlew test`, and AGENTS.md documents `./gradlew :core:domain:test`.
    // Multiplatform modules have `allTests` and `testAndroid`, no `test` — without this alias
    // those tests would silently never run in CI, which is worse than not having them.
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        // Detekt looks in src/main/kotlin by default, which no multiplatform module has. Without
        // this the task reports NO-SOURCE and static analysis silently covers only :app.
        detekt {
            source.setFrom(files("src"))
        }

        tasks.register("test") {
            group = "verification"
            description = "Runs the host-side tests of this multiplatform module."
            dependsOn(tasks.matching { it.name == "allTests" || it.name == "testAndroid" })
        }
    }
}
