package com.loukatech.mbote

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.loukatech.mbote.service.MboteNotificationManager
import com.loukatech.mbote.service.api.MboteApiService
import com.loukatech.mbote.service.api.MboteBackendConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MboteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        val applicationId = BuildConfig.FIREBASE_ANDROID_APPLICATION_ID.trim()
        val apiKey = BuildConfig.FIREBASE_ANDROID_API_KEY.trim()
        val projectId = BuildConfig.FIREBASE_ANDROID_PROJECT_ID.trim()
        val senderId = BuildConfig.FIREBASE_ANDROID_SENDER_ID.trim()
        if (applicationId.isBlank() || apiKey.isBlank() || projectId.isBlank() || senderId.isBlank()) {
            Log.i("MBoteFirebase", "Firebase client configuration is not available in this build")
            return
        }
        runCatching {
            val options = FirebaseOptions.Builder()
                .setApplicationId(applicationId)
                .setApiKey(apiKey)
                .setProjectId(projectId)
                .setGcmSenderId(senderId)
                .build()
            FirebaseApp.initializeApp(this, options)
        }.onFailure {
            Log.e("MBoteFirebase", "Firebase initialization failed", it)
        }
    }
}

object MbotePushTokenSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncIfAuthenticated() {
        if (MboteBackendConfig.authToken.isNullOrBlank()) return
        if (runCatching { FirebaseApp.getInstance() }.isFailure) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) return@addOnSuccessListener
                MboteNotificationManager.updateFcmToken(token)
                scope.launch {
                    MboteApiService().registerPushToken(token)
                        .onFailure { Log.w("MBoteFirebase", "Push token sync failed: ${it.message}") }
                }
            }
            .addOnFailureListener { error ->
                Log.w("MBoteFirebase", "FCM token unavailable", error)
            }
    }
}
