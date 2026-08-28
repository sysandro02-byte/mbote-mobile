package com.loukatech.mbote.service

import android.content.Context
import android.util.Log
import com.loukatech.mbote.BuildConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * WebSocketManager utility class to handle connection lifecycle, automatic reconnection logic,
 * and error logging using VITE_SOCKET_URL from BuildConfig.
 */
object WebSocketManager {
    private const val TAG = "WebSocketManager"

    val socketUrl: String
        get() = BuildConfig.VITE_SOCKET_URL

    val connectionState: StateFlow<SocketConnectionState>
        get() = MboteSocketManager.connectionState

    val isConnected: StateFlow<Boolean>
        get() = MboteSocketManager.isConnected

    /**
     * Initializes the WebSocket connection lifecycle using BuildConfig.VITE_SOCKET_URL.
     */
    fun initialize(context: Context) {
        Log.i(TAG, "Initializing WebSocketManager with URL: $socketUrl")
        connect()
    }

    /**
     * Connects to the WebSocket endpoint using BuildConfig.VITE_SOCKET_URL.
     */
    fun connect() {
        Log.d(TAG, "Connecting WebSocket via VITE_SOCKET_URL ($socketUrl)...")
        MboteSocketManager.connect(customUrl = socketUrl, forceResetAttempts = true)
    }

    /**
     * Gracefully disconnects the WebSocket.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket connection...")
        MboteSocketManager.disconnect()
    }

    /**
     * Sends a chat message via WebSocket with automatic retry logging.
     */
    fun sendChatMessage(chatId: String, text: String, senderName: String = "Moi") {
        Log.d(TAG, "Sending message to $chatId via WebSocketManager: $text")
        MboteSocketManager.sendChatMessage(chatId = chatId, text = text, senderName = senderName)
    }

    /**
     * Logs WebSocket system errors or state transitions.
     */
    fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, "WebSocket error: $message", throwable)
        } else {
            Log.w(TAG, "WebSocket error: $message")
        }
    }
}
