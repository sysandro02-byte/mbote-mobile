import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val envProps = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    FileInputStream(envFile).use { stream ->
        envProps.load(stream)
    }
}

fun sanitizedEnvValue(name: String, fallback: String = ""): String =
    (envProps.getProperty(name) ?: System.getenv(name) ?: fallback).trim()

fun javaStringLiteral(value: String): String =
    "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n") + "\""

val configuredMboteApiBaseUrl = envProps.getProperty("MBOTE_API_BASE_URL")
    ?: System.getenv("MBOTE_API_BASE_URL")
    ?: "https://mbote-backend.onrender.com/v1"
val mboteApiBaseUrl = configuredMboteApiBaseUrl.trim().trimEnd('/').let { url ->
    require(url.startsWith("https://") || url.startsWith("http://")) {
        "MBOTE_API_BASE_URL doit être une URL HTTP(S) valide"
    }
    when {
        url.endsWith("/v1") -> url
        url.endsWith("/api/v1") -> url
        else -> "$url/v1"
    }
}
val viteSocketUrl = sanitizedEnvValue("VITE_SOCKET_URL", "https://mbote-backend.onrender.com")
val viteSupabaseUrl = sanitizedEnvValue("VITE_SUPABASE_URL")
val viteSupabaseAnonKey = sanitizedEnvValue("VITE_SUPABASE_ANON_KEY")
val googleClientId = sanitizedEnvValue("GOOGLE_CLIENT_ID")
val githubClientId = sanitizedEnvValue("GITHUB_CLIENT_ID")

val qaKeystorePath = System.getenv("MBOTE_QA_KEYSTORE_PATH")
val qaKeystorePassword = System.getenv("MBOTE_QA_KEYSTORE_PASSWORD")
val qaKeyAlias = System.getenv("MBOTE_QA_KEY_ALIAS")
val qaKeyPassword = System.getenv("MBOTE_QA_KEY_PASSWORD")
val qaSigningConfigured = listOf(
    qaKeystorePath,
    qaKeystorePassword,
    qaKeyAlias,
    qaKeyPassword,
).all { !it.isNullOrBlank() }

val releaseKeystorePath = System.getenv("MBOTE_RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("MBOTE_RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("MBOTE_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("MBOTE_RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val configuredVersionCode = System.getenv("MBOTE_VERSION_CODE")?.toIntOrNull() ?: 1
val configuredVersionName = System.getenv("MBOTE_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"

android {
    namespace = "com.loukatech.mbote"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.mbote.krtwvx"
        minSdk = 26
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = configuredVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "VITE_SOCKET_URL", javaStringLiteral(viteSocketUrl))
        buildConfigField("String", "MBOTE_API_BASE_URL", javaStringLiteral(mboteApiBaseUrl))
        buildConfigField("String", "VITE_SUPABASE_URL", javaStringLiteral(viteSupabaseUrl))
        buildConfigField("String", "VITE_SUPABASE_ANON_KEY", javaStringLiteral(viteSupabaseAnonKey))
        buildConfigField("String", "GOOGLE_CLIENT_ID", javaStringLiteral(googleClientId))
        buildConfigField("String", "GITHUB_CLIENT_ID", javaStringLiteral(githubClientId))
    }

    signingConfigs {
        if (qaSigningConfigured) {
            create("qa") {
                storeFile = file(qaKeystorePath!!)
                storePassword = qaKeystorePassword
                keyAlias = qaKeyAlias
                keyPassword = qaKeyPassword
            }
        }
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
            signingConfig = if (qaSigningConfigured) {
                signingConfigs.getByName("qa")
            } else {
                signingConfigs.getByName("debug")
            }
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.firebase.messaging)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.okhttp)
    implementation(libs.webrtc)
    implementation(libs.androidx.biometric)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
