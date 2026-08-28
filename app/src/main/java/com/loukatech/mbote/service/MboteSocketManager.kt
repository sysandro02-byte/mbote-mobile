package com.loukatech.mbote.service

import android.util.Log
import com.loukatech.mbote.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class SocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class PartnerTypingState(
    val chatId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SocketChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val chatId: String,
    val senderId: String = "user",
    val senderName: String,
    val senderAvatar: String? = null,
    val text: String,
    val timestamp: String = "À l'instant",
    val isMedia: Boolean = false,
    val mediaUrl: String? = null
)

@Serializable
data class LiveStreamSocketEvent(
    val type: String, // "LIVE_COMMENT", "LIVE_REACTION", "LIVE_GIFT", "LIVE_VIEWER_COUNT", "LIVE_BROADCAST_STATUS"
    val streamId: String = "default_live",
    val senderName: String = "Spectateur",
    val payloadText: String? = null,
    val emoji: String? = null,
    val giftId: String? = null,
    val giftName: String? = null,
    val giftEmoji: String? = null,
    val giftValueFcfa: Long = 0,
    val viewerCount: Int = 0,
    val status: String? = null,
    val badgeType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Production-ready Real-Time WebSocket Client & Presence Manager for MBoté.
 * Connects to VITE_SOCKET_URL from environment with automatic reconnection,
 * heartbeat ping/pong lifecycle, real-time messaging, and live-stream event streams.
 */
object MboteSocketManager {
    private const val TAG = "MboteSocketManager"
    private const val MAX_RECONNECT_ATTEMPTS = 3
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isManuallyDisconnected = false
    private var reconnectAttempt = 0
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _socketUrl = MutableStateFlow(
        if (BuildConfig.VITE_SOCKET_URL.isNotBlank()) BuildConfig.VITE_SOCKET_URL
        else "wss://mbote-socket.loukatech.com/ws"
    )
    val socketUrl: StateFlow<String> = _socketUrl.asStateFlow()

    // Map of chatId -> PartnerTypingState
    private val _typingStateMap = MutableStateFlow<Map<String, PartnerTypingState>>(emptyMap())
    val typingStateMap: StateFlow<Map<String, PartnerTypingState>> = _typingStateMap.asStateFlow()

    // Real-time Chat Messages SharedFlow
    private val _incomingMessages = MutableSharedFlow<SocketChatMessage>(extraBufferCapacity = 128)
    val incomingMessages: SharedFlow<SocketChatMessage> = _incomingMessages.asSharedFlow()

    // Real-time Live Stream Events SharedFlow (Comments, Reactions, Gifts, Viewer Count, Status)
    private val _liveStreamEvents = MutableSharedFlow<LiveStreamSocketEvent>(extraBufferCapacity = 128)
    val liveStreamEvents: SharedFlow<LiveStreamSocketEvent> = _liveStreamEvents.asSharedFlow()

    // Live Viewer Count Map
    private val _liveViewerCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val liveViewerCounts: StateFlow<Map<String, Int>> = _liveViewerCounts.asStateFlow()

    private val autoResetJobs = mutableMapOf<String, Job>()
    private var myTypingDebounceJob: Job? = null

    init {
        Log.d(TAG, "MboteSocketManager initialized with target socket URL: ${_socketUrl.value}")
        connect()
    }

    /**
     * Establishes a reliable WebSocket connection to VITE_SOCKET_URL
     */
    fun connect(customUrl: String? = null, forceResetAttempts: Boolean = false) {
        if (customUrl != null && customUrl.isNotBlank()) {
            _socketUrl.value = customUrl
            reconnectAttempt = 0
        }
        if (forceResetAttempts) {
            reconnectAttempt = 0
        }

        val targetUrl = _socketUrl.value

        if (webSocket != null && _connectionState.value == SocketConnectionState.CONNECTED) {
            Log.d(TAG, "Socket already connected to $targetUrl")
            return
        }

        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "WebSocket auto-reconnect skipped because max attempts ($MAX_RECONNECT_ATTEMPTS) reached. Running in local event bus fallback mode.")
            _connectionState.value = SocketConnectionState.DISCONNECTED
            _isConnected.value = false
            return
        }

        isManuallyDisconnected = false
        _connectionState.value = if (reconnectAttempt > 0) SocketConnectionState.RECONNECTING else SocketConnectionState.CONNECTING

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("User-Agent", "MBote-Android-Socket/1.0")
                .build()

            webSocket = client.newWebSocket(request, createWebSocketListener())
            Log.d(TAG, "Initiated WebSocket connection attempt #${reconnectAttempt + 1} to $targetUrl")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create WebSocket request to $targetUrl: ${e.message}")
            handleDisconnect(isPermanentFailure = true)
        }
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket connected successfully to ${_socketUrl.value}")
            _connectionState.value = SocketConnectionState.CONNECTED
            _isConnected.value = true
            reconnectAttempt = 0
            startHeartbeat()

            // Send initial connection payload
            sendRawEvent("""{"type":"handshake","client":"MBote-Android","timestamp":${System.currentTimeMillis()}}""")
        }

        override fun onMessage(ws: WebSocket, text: String) {
            Log.d(TAG, "WebSocket message received: $text")
            handleIncomingPacket(text)
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code / $reason")
            ws.close(1000, null)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            handleDisconnect()
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            val isNotFoundOrHttpError = response?.code == 404 || t.message?.contains("404") == true
            if (isNotFoundOrHttpError) {
                Log.w(TAG, "WebSocket endpoint at ${_socketUrl.value} returned HTTP 404 Not Found. Falling back to local internal event bus.")
                handleDisconnect(isPermanentFailure = true)
            } else {
                Log.w(TAG, "WebSocket connection failed (${t.message}). Attempt ${reconnectAttempt + 1} of $MAX_RECONNECT_ATTEMPTS")
                handleDisconnect(isPermanentFailure = reconnectAttempt >= MAX_RECONNECT_ATTEMPTS)
            }
        }
    }

    private fun handleDisconnect(isPermanentFailure: Boolean = false) {
        stopHeartbeat()
        _connectionState.value = SocketConnectionState.DISCONNECTED
        _isConnected.value = false
        webSocket = null

        if (!isManuallyDisconnected && !isPermanentFailure && reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect()
        } else {
            reconnectJob?.cancel()
            Log.i(TAG, "WebSocket listener disconnected. Operating seamlessly in offline/local event bus mode.")
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        _connectionState.value = SocketConnectionState.RECONNECTING
        reconnectJob = scope.launch {
            reconnectAttempt++
            val delayMs = (1000L * (1 shl reconnectAttempt.coerceAtMost(4))).coerceAtMost(15000L)
            Log.d(TAG, "Scheduling WebSocket reconnect attempt #$reconnectAttempt in ${delayMs}ms")
            delay(delayMs)
            connect()
        }
    }

    fun disconnect() {
        isManuallyDisconnected = true
        reconnectJob?.cancel()
        stopHeartbeat()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
        _isConnected.value = false
        Log.d(TAG, "WebSocket manually disconnected.")
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == SocketConnectionState.CONNECTED) {
                delay(20000)
                sendRawEvent("""{"type":"ping","timestamp":${System.currentTimeMillis()}}""")
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun sendRawEvent(jsonPayload: String): Boolean {
        val ws = webSocket
        return if (ws != null && _connectionState.value == SocketConnectionState.CONNECTED) {
            ws.send(jsonPayload)
        } else {
            Log.w(TAG, "Cannot send event - socket not connected")
            false
        }
    }

    private fun handleIncomingPacket(rawJson: String) {
        scope.launch {
            try {
                val jsonElement = json.parseToJsonElement(rawJson)
                if (jsonElement is JsonObject) {
                    val type = jsonElement["type"]?.jsonPrimitive?.content ?: ""
                    when (type) {
                        "pong" -> {
                            Log.d(TAG, "Socket Heartbeat Pong received.")
                        }
                        "CHAT_MESSAGE" -> {
                            val msg = json.decodeFromJsonElement<SocketChatMessage>(jsonElement)
                            _incomingMessages.emit(msg)
                        }
                        "USER_TYPING" -> {
                            val chatId = jsonElement["chatId"]?.jsonPrimitive?.content ?: ""
                            val userName = jsonElement["userName"]?.jsonPrimitive?.content ?: "Interlocuteur"
                            val isTyping = jsonElement["isTyping"]?.jsonPrimitive?.booleanOrNull ?: false
                            onRemotePartnerTypingReceived(chatId, userName, isTyping)
                        }
                        "LIVE_COMMENT", "LIVE_REACTION", "LIVE_GIFT", "LIVE_VIEWER_COUNT", "LIVE_BROADCAST_STATUS" -> {
                            val event = json.decodeFromJsonElement<LiveStreamSocketEvent>(jsonElement)
                            if (event.type == "LIVE_VIEWER_COUNT") {
                                _liveViewerCounts.update { current ->
                                    current + (event.streamId to event.viewerCount)
                                }
                            }
                            _liveStreamEvents.emit(event)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing incoming WebSocket packet: ${e.message}")
            }
        }
    }

    // ================= Real-Time Messaging Emit Methods =================

    fun sendChatMessage(chatId: String, senderName: String, text: String, mediaUrl: String? = null) {
        val message = SocketChatMessage(
            chatId = chatId,
            senderName = senderName,
            text = text,
            isMedia = mediaUrl != null,
            mediaUrl = mediaUrl
        )
        val payload = json.encodeToString(message)
        sendRawEvent("""{"type":"CHAT_MESSAGE","payload":$payload}""")
    }

    fun sendUserTypingState(chatId: String, isTyping: Boolean, userName: String = "Vous") {
        myTypingDebounceJob?.cancel()
        val payload = """{"type":"USER_TYPING","chatId":"$chatId","userName":"$userName","isTyping":$isTyping}"""
        sendRawEvent(payload)
        Log.d(TAG, "Socket event emitted -> USER_TYPING: chatId=$chatId, isTyping=$isTyping")
    }

    fun onRemotePartnerTypingReceived(chatId: String, userName: String, isTyping: Boolean) {
        scope.launch {
            if (isTyping) {
                _typingStateMap.update { current ->
                    current + (chatId to PartnerTypingState(chatId, userName, true))
                }

                autoResetJobs[chatId]?.cancel()
                autoResetJobs[chatId] = launch {
                    delay(4500)
                    _typingStateMap.update { current ->
                        current.filterKeys { it != chatId }
                    }
                }
            } else {
                autoResetJobs[chatId]?.cancel()
                _typingStateMap.update { current ->
                    current.filterKeys { it != chatId }
                }
            }
        }
    }

    fun simulatePartnerTyping(chatId: String, partnerName: String, durationMs: Long = 3000L) {
        scope.launch {
            delay(400)
            onRemotePartnerTypingReceived(chatId, partnerName, true)
            delay(durationMs)
            onRemotePartnerTypingReceived(chatId, partnerName, false)
        }
    }

    fun clearTyping(chatId: String) {
        autoResetJobs[chatId]?.cancel()
        _typingStateMap.update { current ->
            current.filterKeys { it != chatId }
        }
    }

    // ================= Live-Stream Real-Time Emit Methods =================

    fun sendLiveComment(streamId: String, senderName: String, text: String, badgeType: String? = null) {
        val event = LiveStreamSocketEvent(
            type = "LIVE_COMMENT",
            streamId = streamId,
            senderName = senderName,
            payloadText = text,
            badgeType = badgeType
        )
        val payload = json.encodeToString(event)
        sendRawEvent(payload)
        scope.launch { _liveStreamEvents.emit(event) }
    }

    fun sendLiveReaction(streamId: String, senderName: String, emoji: String) {
        val event = LiveStreamSocketEvent(
            type = "LIVE_REACTION",
            streamId = streamId,
            senderName = senderName,
            emoji = emoji
        )
        val payload = json.encodeToString(event)
        sendRawEvent(payload)
        scope.launch { _liveStreamEvents.emit(event) }
    }

    fun sendLiveGift(
        streamId: String,
        senderName: String,
        giftId: String,
        giftName: String,
        emoji: String,
        valueFcfa: Long
    ) {
        val event = LiveStreamSocketEvent(
            type = "LIVE_GIFT",
            streamId = streamId,
            senderName = senderName,
            giftId = giftId,
            giftName = giftName,
            giftEmoji = emoji,
            giftValueFcfa = valueFcfa
        )
        val payload = json.encodeToString(event)
        sendRawEvent(payload)
        scope.launch { _liveStreamEvents.emit(event) }
    }

    fun sendLiveBroadcastStatus(streamId: String, status: String) {
        val event = LiveStreamSocketEvent(
            type = "LIVE_BROADCAST_STATUS",
            streamId = streamId,
            status = status
        )
        val payload = json.encodeToString(event)
        sendRawEvent(payload)
        scope.launch { _liveStreamEvents.emit(event) }
    }
}
