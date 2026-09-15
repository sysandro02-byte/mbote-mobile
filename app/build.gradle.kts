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
val mboteApiBaseUrl = envProps.getProperty("MBOTE_API_BASE_URL")
    ?: System.getenv("MBOTE_API_BASE_URL")
    ?: "https://mbote-backend.onrender.com/v1"
val viteSocketUrl = envProps.getProperty("VITE_SOCKET_URL")
    ?: System.getenv("VITE_SOCKET_URL")
    ?: "https://mbote-backend.onrender.com"
val viteSupabaseUrl = envProps.getProperty("VITE_SUPABASE_URL")
    ?: System.getenv("VITE_SUPABASE_URL")
    ?: ""
val viteSupabaseAnonKey = envProps.getProperty("VITE_SUPABASE_ANON_KEY")
    ?: System.getenv("VITE_SUPABASE_ANON_KEY")
    ?: ""

val googleClientId = envProps.getProperty("GOOGLE_CLIENT_ID")
    ?: System.getenv("GOOGLE_CLIENT_ID")
    ?: ""
val githubClientId = envProps.getProperty("GITHUB_CLIENT_ID")
    ?: System.getenv("GITHUB_CLIENT_ID")
    ?: ""

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

android {
    namespace = "com.loukatech.mbote"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.mbote.krtwvx"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "VITE_SOCKET_URL", "\"$viteSocketUrl\"")
        buildConfigField("String", "MBOTE_API_BASE_URL", "\"$mboteApiBaseUrl\"")
        buildConfigField("String", "VITE_SUPABASE_URL", "\"$viteSupabaseUrl\"")
        buildConfigField("String", "VITE_SUPABASE_ANON_KEY", "\"$viteSupabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$githubClientId\"")
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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")
    }
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
