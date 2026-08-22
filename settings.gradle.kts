// Larova · Gradle project layout.
//
// Module boundaries follow docs/technical-notes.md §3. They are drawn in full here even though
// several modules are still thin: the iOS milestone (M5) is meant to be platform adapters plus UI
// polish, and that is only true if platform-near code never leaks out of :core:platform.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "larova"

include(":app")

include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:platform")

include(":feature:home")
include(":feature:card")
include(":feature:help")
include(":feature:transfer")
include(":feature:settings")
