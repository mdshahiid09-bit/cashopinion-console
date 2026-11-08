import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

fun getAppVersionName(): String {
    val versionPropsFile = File(rootDir, "version.properties")
    if (versionPropsFile.exists()) {
        val props = Properties()
        props.load(versionPropsFile.inputStream())
        return props.getProperty("versionName", "2.0.0")
    }
    return "2.0.0"
}

// Fail-fast verification of app/google-services.json (Section 10.4)
tasks.register("verifyGoogleServices") {
    doLast {
        val gsFile = File(projectDir, "google-services.json")
        if (!gsFile.exists()) {
            throw GradleException("FATAL: app/google-services.json is missing! Please place the cash-opinion Firebase google-services.json in the app/ directory.")
        }
        val content = gsFile.readText()
        if (!content.contains("\"project_id\": \"cash-opinion\"") && !content.contains("\"project_id\":\"cash-opinion\"")) {
            throw GradleException("FATAL: app/google-services.json has wrong project_id! Expected 'cash-opinion'.")
        }
        if (!content.contains("\"package_name\": \"com.cashopinion.console\"") && !content.contains("\"package_name\":\"com.cashopinion.console\"")) {
            throw GradleException("FATAL: app/google-services.json does not contain package_name 'com.cashopinion.console'! Please register com.cashopinion.console in the Firebase Console and re-download google-services.json.")
        }
    }
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    dependsOn("verifyGoogleServices")
}

android {
    namespace = "com.cashopinion.console"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cashopinion.console"
        minSdk = 24
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "200").toIntOrNull() ?: 200
        versionName = getAppVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/app/console.keystore"
            val kFile = if (File(keystorePath).isAbsolute) File(keystorePath) else file("${rootDir}/${keystorePath}")
            val storePass = System.getenv("KEYSTORE_PASSWORD")
            val kAlias = System.getenv("KEY_ALIAS")
            val kPass = System.getenv("KEY_PASSWORD")

            storeFile = if (kFile.exists() && kFile.length() > 0) kFile else file(keystorePath)
            storePassword = storePass
            keyAlias = kAlias
            keyPassword = kPass ?: storePass
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Coil for images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // OkHttp for Render server calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
}
