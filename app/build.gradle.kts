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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
    implementation(libs.jetbrains.navigation.compose)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    testImplementation(libs.kotlin.test)
}
