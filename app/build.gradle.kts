import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "app.sprout.habits"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.sprout.habits"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "1.3.4"
        // Where the About screen's links point. Set once the GitHub repo exists.
        buildConfigField("String", "REPO_URL", "\"https://github.com/nisabmohd/sprout-habit-tracker\"")
        buildConfigField("String", "SPONSOR_URL", "\"https://github.com/sponsors/nisabmohd\"")
        buildConfigField("String", "PLAY_STORE_URL", "\"https://play.google.com/store/apps/details?id=app.sprout.habits\"")
    }

    flavorDimensions += "distribution"
    productFlavors {
        // Google sign-in + Drive appDataFolder backup.
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "PLAY_STORE", "true")
            // Set to true once Sprout is live on Google Play. It turns on "Rate on Play Store"
            // (About and the support prompt) and About's "Check for updates", which both open
            // the store listing. The other builds never link to Google Play.
            buildConfigField("boolean", "PLAY_LISTED", "false")
            buildConfigField("boolean", "GITHUB_UPDATES", "false")
        }
        // No Google or Play Services code at all, and no network (F-Droid updates it).
        create("foss") {
            dimension = "distribution"
            versionNameSuffix = "-foss"
            buildConfigField("boolean", "PLAY_STORE", "false")
            buildConfigField("boolean", "PLAY_LISTED", "false")
            buildConfigField("boolean", "GITHUB_UPDATES", "false")
        }
        // The APK on GitHub Releases: foss plus a check for new releases that downloads and installs them.
        create("github") {
            dimension = "distribution"
            versionNameSuffix = "-github"
            buildConfigField("boolean", "PLAY_STORE", "false")
            buildConfigField("boolean", "PLAY_LISTED", "false")
            buildConfigField("boolean", "GITHUB_UPDATES", "true")
        }
    }

    signingConfigs {
        // Set by CI (see docs/RELEASING.md), or locally by the gitignored keystore/keystore.properties.
        // Without either, release builds use the debug key so they can still be installed locally.
        val keystore = System.getenv("SPROUT_KEYSTORE_PATH")
        val local = rootProject.file("keystore/keystore.properties")
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("SPROUT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SPROUT_KEY_ALIAS")
                keyPassword = System.getenv("SPROUT_KEY_PASSWORD")
            }
        } else if (local.exists()) {
            val props = Properties().apply { local.inputStream().use { load(it) } }
            create("release") {
                storeFile = rootProject.file("keystore/" + props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    androidResources {
        // Only the languages the app ships; drops every other locale from AndroidX resources.
        // Keep in step with the values-* folders.
        localeFilters += listOf("en", "de", "es", "fr", "hi", "ja", "pt-rBR")
        // Lists those languages in Android 13+'s per-app language setting.
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/**/LICENSE.txt",
                "META-INF/*.version",
                "META-INF/{AL2.0,LGPL2.1}",
                "kotlin/**.kotlin_builtins",
                "kotlin-tooling-metadata.json",
                "DebugProbesKt.bin",
            )
        }
    }

    dependenciesInfo {
        // The signed dependency block is only readable by Google Play; F-Droid rejects it.
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

baselineProfile {
    // Generated on demand with ./gradlew :app:generateBaselineProfile, then committed.
    automaticGenerationDuringBuild = false
    // One profile for both flavors; the screens are the same.
    mergeIntoMain = true
    dexLayoutOptimization = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
}
