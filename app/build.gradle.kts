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
    ?: "https://mbote-backend.onrender.com/api"
val viteSocketUrl = envProps.getProperty("VITE_SOCKET_URL")
    ?: System.getenv("VITE_SOCKET_URL")
    ?: "https://mbote-backend.onrender.com"
val viteSupabaseUrl = envProps.getProperty("VITE_SUPABASE_URL")
    ?: System.getenv("VITE_SUPABASE_URL")
    ?: "https://mbote-app.supabase.co"
val viteSupabaseAnonKey = envProps.getProperty("VITE_SUPABASE_ANON_KEY")
    ?: System.getenv("VITE_SUPABASE_ANON_KEY")
    ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1ib3RlLWFwcCIsInJvbGUiOiJhbW9uIiwiaWF0IjoxNzE2MDAwMDAwLCJleHAiOjIwMzE1NzYwMDB9.MboteSupabaseSecretKeyPlaceholder"

val googleClientId = envProps.getProperty("GOOGLE_CLIENT_ID")
    ?: System.getenv("GOOGLE_CLIENT_ID")
    ?: "108392019482-mbote-google-oauth.apps.googleusercontent.com"
val githubClientId = envProps.getProperty("GITHUB_CLIENT_ID")
    ?: System.getenv("GITHUB_CLIENT_ID")
    ?: "Iv1.mbote_github_oauth_client_id"
val brevoApiKey = envProps.getProperty("BREVO_API_KEY")
    ?: System.getenv("BREVO_API_KEY")
    ?: "xkeysib-brevo-api-key-placeholder"
val brevoApiUrl = envProps.getProperty("BREVO_API_URL")
    ?: System.getenv("BREVO_API_URL")
    ?: "https://api.brevo.com/v3/smtp/email"
val brevoSenderEmail = envProps.getProperty("BREVO_SENDER_EMAIL")
    ?: System.getenv("BREVO_SENDER_EMAIL")
    ?: "noreply@loukatech.com"
val brevoSenderName = envProps.getProperty("BREVO_SENDER_NAME")
    ?: System.getenv("BREVO_SENDER_NAME")
    ?: "MBoté Sécurité"
val geminiApiKey = envProps.getProperty("GEMINI_API_KEY")
    ?: System.getenv("GEMINI_API_KEY")
    ?: "gemini-api-key-placeholder"

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
        buildConfigField("String", "BREVO_API_KEY", "\"$brevoApiKey\"")
        buildConfigField("String", "BREVO_API_URL", "\"$brevoApiUrl\"")
        buildConfigField("String", "BREVO_SENDER_EMAIL", "\"$brevoSenderEmail\"")
        buildConfigField("String", "BREVO_SENDER_NAME", "\"$brevoSenderName\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
