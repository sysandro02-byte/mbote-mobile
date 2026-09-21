package com.loukatech.mbote.service

import android.content.Context
import com.loukatech.mbote.service.api.IceServerDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*

/**
 * Bidirectional WebRTC mesh transport shared by 1:1 calls and MBoté meetings.
 *
 * Signaling only travels through the authenticated MBoté /ws gateway. Media
 * flows peer-to-peer when possible and through the configured TURN relay when
 * direct connectivity is unavailable.
 */
class RtcRoomManager(
    context: Context,
    private val roomCode: String,
    private val isVideoCall: Boolean,
    private val iceServerConfig: List<IceServerDto>,
    private val onLocalVideoTrack: (VideoTrack) -> Unit = {},
    private val onRemoteVideoTrack: (peerId: String, track: VideoTrack) -> Unit = { _, _ -> },
    private val onPeerLeft: (peerId: String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val egl = EglBase.create()
    private val factory: PeerConnectionFactory
    private val peers = mutableMapOf<String, PeerConnection>()
    private val pendingIce = mutableMapOf<String, MutableList<IceCandidate>>()
    private val remoteDescriptionReady = mutableSetOf<String>()

    private var capturer: CameraVideoCapturer? = null
    private var textureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideo: VideoTrack? = null
    private var localAudio: AudioTrack? = null
    @Volatile private var closed = false

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext).createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()

        startLocalMedia()
        MboteSocketManager.joinRtcRoom(roomCode)

        scope.launch {
            MboteSocketManager.rtcEvents.collectLatest { event ->
                if (event.optString("roomCode") == roomCode.uppercase()) handleRoomEvent(event)
            }
        }
        scope.launch {
            MboteSocketManager.rtcSignals.collectLatest { signal ->
                if (signal.optString("roomCode") == roomCode.uppercase()) handleSignal(signal)
            }
        }
    }

    fun eglContext(): EglBase.Context = egl.eglBaseContext

    private fun startLocalMedia() {
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudio = factory.createAudioTrack("MBOTE_RTC_AUDIO", audioSource).apply { setEnabled(true) }

        if (!isVideoCall) return
        val enumerator = Camera2Enumerator(appContext)
        val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return
        capturer = enumerator.createCapturer(deviceName, null)
        videoSource = factory.createVideoSource(false)
        textureHelper = SurfaceTextureHelper.create("MBoteRtcCapture", egl.eglBaseContext)
        capturer?.initialize(textureHelper, appContext, videoSource!!.capturerObserver)
        capturer?.startCapture(720, 1280, 30)
        localVideo = factory.createVideoTrack("MBOTE_RTC_VIDEO", videoSource).apply {
            setEnabled(true)
            onLocalVideoTrack(this)
        }
    }

    private fun rtcIceServers(): List<PeerConnection.IceServer> =
        iceServerConfig.flatMap { config ->
            config.urls.mapNotNull { rawUrl ->
                val url = rawUrl.trim()
                if (url.isBlank()) null else PeerConnection.IceServer.builder(url)
                    .apply {
                        if (config.username.isNotBlank()) setUsername(config.username)
                        if (config.credential.isNotBlank()) setPassword(config.credential)
                    }
                    .createIceServer()
            }
        }.ifEmpty {
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
        }

    private fun peer(peerId: String): PeerConnection =
        peers.getOrPut(peerId) {
            factory.createPeerConnection(rtcIceServers(), object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    MboteSocketManager.sendRtcSignal(
                        roomCode = roomCode,
                        targetUserId = peerId,
                        signalType = "ICE",
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    (transceiver?.receiver?.track() as? VideoTrack)?.let { onRemoteVideoTrack(peerId, it) }
                }

                override fun onAddStream(stream: MediaStream?) {
                    stream?.videoTracks?.firstOrNull()?.let { onRemoteVideoTrack(peerId, it) }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    if (newState == PeerConnection.PeerConnectionState.FAILED ||
                        newState == PeerConnection.PeerConnectionState.CLOSED
                    ) {
                        onPeerLeft(peerId)
                    }
                }

                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
            })!!.also { connection ->
                localAudio?.let { connection.addTrack(it, listOf("mbote-rtc")) }
                localVideo?.let { connection.addTrack(it, listOf("mbote-rtc")) }
            }
        }

    private fun handleRoomEvent(event: JSONObject) {
        when (event.optString("type")) {
            "RTC_PEERS" -> {
                val peersArray = event.optJSONArray("peers") ?: JSONArray()
                for (index in 0 until peersArray.length()) {
                    val peerId = peersArray.optJSONObject(index)?.optString("userId").orEmpty()
                    if (peerId.isNotBlank()) createOffer(peerId)
                }
            }
            "RTC_PEER_JOINED" -> {
                val peerId = event.optString("userId")
                if (peerId.isNotBlank()) peer(peerId)
            }
            "RTC_PEER_LEFT" -> {
                val peerId = event.optString("userId")
                if (peerId.isNotBlank()) removePeer(peerId)
            }
        }
    }

    private fun createOffer(peerId: String) {
        val connection = peer(peerId)
        connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                connection.setLocalDescription(simpleSdpObserver(), description)
                MboteSocketManager.sendRtcSignal(roomCode, peerId, "OFFER", sdp = description.description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    private fun handleSignal(data: JSONObject) {
        val peerId = data.optString("fromUserId")
        if (peerId.isBlank()) return
        val connection = peer(peerId)
        when (data.optString("signalType").uppercase()) {
            "OFFER" -> {
                val sdp = data.optString("sdp")
                if (sdp.isBlank()) return
                connection.setRemoteDescription(
                    remoteSdpObserver(peerId) {
                        connection.createAnswer(object : SdpObserver {
                            override fun onCreateSuccess(description: SessionDescription) {
                                connection.setLocalDescription(simpleSdpObserver(), description)
                                MboteSocketManager.sendRtcSignal(roomCode, peerId, "ANSWER", sdp = description.description)
                            }
                            override fun onSetSuccess() {}
                            override fun onCreateFailure(error: String?) {}
                            override fun onSetFailure(error: String?) {}
                        }, MediaConstraints())
                    },
                    SessionDescription(SessionDescription.Type.OFFER, sdp)
                )
            }
            "ANSWER" -> {
                val sdp = data.optString("sdp")
                if (sdp.isBlank()) return
                connection.setRemoteDescription(
                    remoteSdpObserver(peerId),
                    SessionDescription(SessionDescription.Type.ANSWER, sdp)
                )
            }
            "ICE" -> {
                val candidate = data.optString("candidate")
                if (candidate.isBlank()) return
                val ice = IceCandidate(
                    data.optString("sdpMid").ifBlank { null },
                    data.optInt("sdpMLineIndex", 0),
                    candidate
                )
                if (remoteDescriptionReady.contains(peerId)) {
                    connection.addIceCandidate(ice)
                } else {
                    pendingIce.getOrPut(peerId) { mutableListOf() }.add(ice)
                }
            }
        }
    }

    private fun remoteSdpObserver(peerId: String, onReady: () -> Unit = {}): SdpObserver =
        object : SdpObserver {
            override fun onSetSuccess() {
                remoteDescriptionReady.add(peerId)
                pendingIce.remove(peerId)?.forEach { peer(peerId).addIceCandidate(it) }
                onReady()
            }
            override fun onCreateSuccess(description: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }

    private fun simpleSdpObserver(): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        localAudio?.setEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideo?.setEnabled(enabled)
    }

    fun switchCamera() {
        capturer?.switchCamera(null)
    }

    private fun removePeer(peerId: String) {
        peers.remove(peerId)?.dispose()
        remoteDescriptionReady.remove(peerId)
        pendingIce.remove(peerId)
        onPeerLeft(peerId)
    }

    fun close() {
        if (closed) return
        closed = true
        MboteSocketManager.leaveRtcRoom(roomCode)
        scope.cancel()
        peers.values.forEach { it.dispose() }
        peers.clear()
        runCatching { capturer?.stopCapture() }
        capturer?.dispose()
        textureHelper?.dispose()
        localVideo?.dispose()
        localAudio?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        factory.dispose()
        egl.release()
    }
}
