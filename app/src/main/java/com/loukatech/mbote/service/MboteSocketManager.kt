package com.loukatech.mbote.service

import android.util.Log
import com.loukatech.mbote.BuildConfig
import com.loukatech.mbote.service.api.MboteBackendConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.net.URI
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

enum class SocketConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

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
    val senderId: String = "",
    val senderName: String = "Utilisateur MBoté",
    val senderAvatar: String? = null,
    val text: String,
    val timestamp: String = "À l'instant",
    val isMedia: Boolean = false,
    val mediaUrl: String? = null
)

@Serializable
data class LiveStreamSocketEvent(
    val type: String,
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

/** Socket.IO client aligned with the canonical MBoté server and JWT session. */
object MboteSocketManager {
    private const val TAG = "MboteSocketManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var currentToken: String? = null
    private var liveWebSocket: WebSocket? = null
    private val liveHttpClient = OkHttpClient()
    private val _liveSignals = MutableSharedFlow<JSONObject>(extraBufferCapacity = 128)
    val liveSignals: SharedFlow<JSONObject> = _liveSignals.asSharedFlow()
    private val _liveSocketIdentity = MutableStateFlow<String?>(null)
    val liveSocketIdentity: StateFlow<String?> = _liveSocketIdentity.asStateFlow()

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private val _socketUrl = MutableStateFlow(BuildConfig.VITE_SOCKET_URL.trimEnd('/'))
    val socketUrl: StateFlow<String> = _socketUrl.asStateFlow()
    private val _typingStateMap = MutableStateFlow<Map<String, PartnerTypingState>>(emptyMap())
    val typingStateMap: StateFlow<Map<String, PartnerTypingState>> = _typingStateMap.asStateFlow()
    private val _incomingMessages = MutableSharedFlow<SocketChatMessage>(extraBufferCapacity = 128)
    val incomingMessages: SharedFlow<SocketChatMessage> = _incomingMessages.asSharedFlow()
    private val _liveStreamEvents = MutableSharedFlow<LiveStreamSocketEvent>(extraBufferCapacity = 128)
    val liveStreamEvents: SharedFlow<LiveStreamSocketEvent> = _liveStreamEvents.asSharedFlow()
    private val _liveViewerCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val liveViewerCounts: StateFlow<Map<String, Int>> = _liveViewerCounts.asStateFlow()
    private val autoResetJobs = mutableMapOf<String, Job>()

    fun connect(customUrl: String? = null, forceResetAttempts: Boolean = false) {
        val token = MboteBackendConfig.authToken?.trim().orEmpty()
        if (token.isBlank()) {
            Log.d(TAG, "Socket.IO connection skipped: no authenticated session")
            disconnect()
            return
        }
        val target = (customUrl?.takeIf { it.isNotBlank() } ?: _socketUrl.value).trimEnd('/')
        _socketUrl.value = target
        if (socket?.connected() == true && currentToken == token && !forceResetAttempts) return
        disconnect()
        currentToken = token
        _connectionState.value = SocketConnectionState.CONNECTING
        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket", "polling"))
                .setReconnection(true)
                .setReconnectionAttempts(8)
                .setReconnectionDelay(1500)
                .setTimeout(10000)
                .build()
            socket = IO.socket(URI.create(target), options).also { client ->
                client.on(Socket.EVENT_CONNECT) {
                    _connectionState.value = SocketConnectionState.CONNECTED
                    _isConnected.value = true
                    Log.i(TAG, "Socket.IO connected to $target")
                }
                client.on(Socket.EVENT_DISCONNECT) {
                    _connectionState.value = SocketConnectionState.DISCONNECTED
                    _isConnected.value = false
                }
                client.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    _connectionState.value = SocketConnectionState.RECONNECTING
                    _isConnected.value = false
                    Log.w(TAG, "Socket.IO connection error: ${args.firstOrNull()}")
                }
                client.on("receive_message") { args -> handleMessage(args.firstOrNull()) }
                client.on("message:new") { args -> handleMessage(args.firstOrNull()) }
                client.on("chat:typing") { args -> handleTyping(args.firstOrNull()) }
                client.on("live:event") { args -> handleLiveEvent(args.firstOrNull()) }
                client.on("user_typing") { args -> handleTyping(args.firstOrNull(), true) }
                client.on("user_stop_typing") { args -> handleTyping(args.firstOrNull(), false) }
                client.connect()
            }
        } catch (error: Exception) {
            _connectionState.value = SocketConnectionState.DISCONNECTED
            _isConnected.value = false
            Log.w(TAG, "Unable to initialize Socket.IO: ${error.message}")
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket?.close()
        socket = null
        currentToken = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
        _isConnected.value = false
    }

    private fun handleMessage(raw: Any?) {
        val data = raw as? JSONObject ?: return
        val chatId = data.optString("chat_id", data.optString("chatId"))
        if (chatId.isBlank()) return
        _incomingMessages.tryEmit(
            SocketChatMessage(
                id = data.optString("id", System.currentTimeMillis().toString()),
                chatId = chatId,
                senderId = data.optString("sender_id", data.optString("senderId")),
                senderName = data.optString("sender_name", data.optString("senderName", "Utilisateur MBoté")),
                senderAvatar = data.optString("sender_avatar", data.optString("senderAvatar")).ifBlank { null },
                text = data.optString("content", data.optString("text")),
                mediaUrl = data.optString("media_url", data.optString("mediaUrl")).ifBlank { null }
            )
        )
    }

    private fun handleTyping(raw: Any?, forcedState: Boolean? = null) {
        val data = raw as? JSONObject ?: return
        val chatId = data.optString("chatId", data.optString("chat_id"))
        if (chatId.isBlank()) return
        onRemotePartnerTypingReceived(
            chatId,
            data.optString("userName", "Interlocuteur"),
            forcedState ?: data.optBoolean("isTyping", true)
        )
    }

    fun sendRawEvent(jsonPayload: String): Boolean {
        Log.w(TAG, "Raw WebSocket payload rejected; use a typed Socket.IO event")
        return false
    }

    fun sendChatMessage(chatId: String, senderName: String, text: String, mediaUrl: String? = null) {
        val payload = JSONObject()
            .put("chatId", chatId)
            .put("content", text)
            .put("type", if (mediaUrl == null) "text" else "media")
        if (mediaUrl != null) payload.put("mediaUrl", mediaUrl)
        socket?.emit("send_message", payload)
    }

    fun sendUserTypingState(chatId: String, isTyping: Boolean, userName: String = "Vous") {
        socket?.emit(if (isTyping) "typing" else "stop_typing", JSONObject().put("chatId", chatId).put("userName", userName))
    }

    fun onRemotePartnerTypingReceived(chatId: String, userName: String, isTyping: Boolean) {
        if (!isTyping) {
            autoResetJobs.remove(chatId)?.cancel()
            _typingStateMap.update { it - chatId }
            return
        }
        _typingStateMap.update { it + (chatId to PartnerTypingState(chatId, userName, true)) }
        autoResetJobs.remove(chatId)?.cancel()
        autoResetJobs[chatId] = scope.launch {
            delay(4500)
            _typingStateMap.update { it - chatId }
        }
    }

    fun clearTyping(chatId: String) = onRemotePartnerTypingReceived(chatId, "", false)

    private fun handleLiveEvent(raw: Any?) {
        val data = raw as? JSONObject ?: return
        val event = LiveStreamSocketEvent(
            type = data.optString("type"),
            streamId = data.optString("streamId"),
            senderName = data.optString("senderName", "Spectateur"),
            payloadText = data.optString("payloadText").ifBlank { null },
            emoji = data.optString("emoji").ifBlank { null },
            giftId = data.optString("giftId").ifBlank { null },
            giftName = data.optString("giftName").ifBlank { null },
            giftEmoji = data.optString("giftEmoji").ifBlank { null },
            giftValueFcfa = data.optLong("giftValueFcfa", 0),
            viewerCount = data.optInt("viewerCount", 0),
            status = data.optString("status").ifBlank { null },
            badgeType = data.optString("badgeType").ifBlank { null },
            timestamp = data.optLong("timestamp", System.currentTimeMillis())
        )
        if (event.type == "LIVE_VIEWER_COUNT" && event.streamId.isNotBlank()) {
            _liveViewerCounts.update { it + (event.streamId to event.viewerCount) }
        }
        _liveStreamEvents.tryEmit(event)
    }

    fun connectLiveWebSocket(streamId: String) {
        val token = MboteBackendConfig.authToken?.trim().orEmpty()
        if (token.isBlank()) return
        liveWebSocket?.close(1000, "reconnect")
        val base = BuildConfig.VITE_SOCKET_URL.trimEnd('/').replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        val request = Request.Builder().url("$base/ws").build()
        liveWebSocket = liveHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("type","AUTH").put("token",token).toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val data = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (data.optString("type")) {
                    "AUTH_OK" -> { _liveSocketIdentity.value = data.optString("userId").ifBlank { null }; webSocket.send(JSONObject().put("type","LIVE_JOIN").put("streamId",streamId).toString()) }
                    "LIVE_SIGNAL" -> _liveSignals.tryEmit(data)
                    else -> handleLiveEvent(data)
                }
            }
        })
    }

    fun sendLiveSignal(streamId: String, signalType: String, targetUserId: String? = null, sdp: String? = null, candidate: String? = null, sdpMid: String? = null, sdpMLineIndex: Int? = null) {
        val payload = JSONObject().put("type","LIVE_SIGNAL").put("streamId",streamId).put("signalType",signalType)
        targetUserId?.let { payload.put("targetUserId",it) }
        sdp?.let { payload.put("sdp",it) }
        candidate?.let { payload.put("candidate",it) }
        sdpMid?.let { payload.put("sdpMid",it) }
        sdpMLineIndex?.let { payload.put("sdpMLineIndex",it) }
        liveWebSocket?.send(payload.toString())
    }

    fun disconnectLiveWebSocket() {
        liveWebSocket?.close(1000, "Live terminé")
        liveWebSocket = null
        _liveSocketIdentity.value = null
    }

    fun joinLive(streamId: String) {
        socket?.emit("live:join", JSONObject().put("streamId", streamId))
    }

    fun leaveLive(streamId: String) {
        socket?.emit("live:leave", JSONObject().put("streamId", streamId))
    }

    fun sendLiveComment(streamId: String, senderName: String, text: String, badgeType: String? = null) {
        socket?.emit("live:comment", JSONObject().put("streamId", streamId).put("text", text).put("badgeType", badgeType))
    }

    fun sendLiveReaction(streamId: String, senderName: String, emoji: String) {
        socket?.emit("live:reaction", JSONObject().put("streamId", streamId).put("emoji", emoji))
    }

    fun sendLiveGift(streamId: String, senderName: String, giftId: String, giftName: String, emoji: String, valueFcfa: Long) {
        socket?.emit("live:gift", JSONObject().put("streamId", streamId).put("giftId", giftId).put("giftName", giftName).put("emoji", emoji).put("valueFcfa", valueFcfa))
    }

    fun sendLiveBroadcastStatus(streamId: String, status: String) {
        socket?.emit("live:status", JSONObject().put("streamId", streamId).put("status", status))
    }
}
