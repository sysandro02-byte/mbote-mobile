package com.loukatech.mbote.service

import android.content.Context
import com.loukatech.mbote.service.api.IceServerDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*

/**
 * Native WebRTC transport for MBoté Live.
 * Broadcaster captures camera + microphone and creates one PeerConnection per viewer.
 * Viewers receive the remote MediaStream through the authenticated signaling channel.
 */
class LiveWebRtcManager(
    context: Context,
    private val streamId: String,
    private val broadcaster: Boolean,
    private val iceServerConfig: List<IceServerDto> = emptyList(),
    private val onLocalVideoTrack: (VideoTrack) -> Unit = {},
    private val onRemoteVideoTrack: (VideoTrack) -> Unit = {},
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

        if (broadcaster) startCapture()
        MboteSocketManager.connectLiveWebSocket(streamId)

        scope.launch {
            MboteSocketManager.liveSignals.collectLatest(::handleSignal)
        }

        if (!broadcaster) {
            scope.launch {
                MboteSocketManager.liveSocketIdentity
                    .filterNotNull()
                    .take(1)
                    .collect { requestStream() }
            }
        }
    }

    fun eglContext(): EglBase.Context = egl.eglBaseContext

    private fun startCapture() {
        val enumerator = Camera2Enumerator(appContext)
        val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return

        capturer = enumerator.createCapturer(deviceName, null)
        videoSource = factory.createVideoSource(false)
        textureHelper = SurfaceTextureHelper.create("MBoteLiveCapture", egl.eglBaseContext)
        capturer?.initialize(textureHelper, appContext, videoSource!!.capturerObserver)
        capturer?.startCapture(720, 1280, 30)

        localVideo = factory.createVideoTrack("MBOTE_LIVE_VIDEO", videoSource).also(onLocalVideoTrack)
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudio = factory.createAudioTrack("MBOTE_LIVE_AUDIO", audioSource).apply {
            setEnabled(true)
        }
    }

    private fun iceServers(): List<PeerConnection.IceServer> =
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

    private fun newPeer(peerId: String): PeerConnection =
        peers.getOrPut(peerId) {
            factory.createPeerConnection(iceServers(), object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    MboteSocketManager.sendLiveSignal(
                        streamId = streamId,
                        signalType = "ICE",
                        targetUserId = peerId,
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                }

                override fun onAddStream(stream: MediaStream?) {
                    stream?.videoTracks?.firstOrNull()?.let(onRemoteVideoTrack)
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    (transceiver?.receiver?.track() as? VideoTrack)?.let(onRemoteVideoTrack)
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
                if (broadcaster) {
                    localVideo?.let { connection.addTrack(it, listOf("mbote-live")) }
                    localAudio?.let { connection.addTrack(it, listOf("mbote-live")) }
                }
            }
        }

    private fun simpleSdpObserver(
        onCreated: (SessionDescription) -> Unit = {},
        onSet: () -> Unit = {},
    ): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) = onCreated(description)
        override fun onSetSuccess() = onSet()
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }

    private fun remoteSdpObserver(peerId: String, onReady: () -> Unit = {}): SdpObserver =
        object : SdpObserver {
            override fun onSetSuccess() {
                remoteDescriptionReady.add(peerId)
                pendingIce.remove(peerId)?.forEach { newPeer(peerId).addIceCandidate(it) }
                onReady()
            }

            override fun onCreateSuccess(description: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }

    /** Viewer announces readiness; broadcaster answers by creating an offer for that viewer. */
    fun requestStream() {
        if (!broadcaster && !closed) {
            MboteSocketManager.sendLiveSignal(streamId, "OFFER", sdp = "REQUEST_STREAM")
        }
    }

    private fun handleSignal(data: JSONObject) {
        if (closed || data.optString("streamId") != streamId) return

        val from = data.optString("fromUserId")
        if (from.isBlank()) return

        when (data.optString("signalType").uppercase()) {
            "OFFER" -> {
                val sdp = data.optString("sdp")
                if (broadcaster && sdp == "REQUEST_STREAM") {
                    val connection = newPeer(from)
                    connection.createOffer(
                        simpleSdpObserver { offer ->
                            connection.setLocalDescription(simpleSdpObserver(), offer)
                            MboteSocketManager.sendLiveSignal(
                                streamId = streamId,
                                signalType = "OFFER",
                                targetUserId = from,
                                sdp = offer.description
                            )
                        },
                        MediaConstraints()
                    )
                } else if (!broadcaster && sdp.isNotBlank() && sdp != "REQUEST_STREAM") {
                    val connection = newPeer(from)
                    connection.setRemoteDescription(
                        remoteSdpObserver(from) {
                            connection.createAnswer(
                                simpleSdpObserver { answer ->
                                    connection.setLocalDescription(simpleSdpObserver(), answer)
                                    MboteSocketManager.sendLiveSignal(
                                        streamId = streamId,
                                        signalType = "ANSWER",
                                        targetUserId = from,
                                        sdp = answer.description
                                    )
                                },
                                MediaConstraints()
                            )
                        },
                        SessionDescription(SessionDescription.Type.OFFER, sdp)
                    )
                }
            }

            "ANSWER" -> {
                if (!broadcaster) return
                val sdp = data.optString("sdp")
                if (sdp.isBlank()) return
                val connection = newPeer(from)
                connection.setRemoteDescription(
                    remoteSdpObserver(from),
                    SessionDescription(SessionDescription.Type.ANSWER, sdp)
                )
            }

            "ICE" -> {
                val candidateText = data.optString("candidate")
                if (candidateText.isBlank()) return
                val candidate = IceCandidate(
                    data.optString("sdpMid").ifBlank { null },
                    data.optInt("sdpMLineIndex", 0),
                    candidateText
                )
                val connection = newPeer(from)
                if (remoteDescriptionReady.contains(from)) {
                    connection.addIceCandidate(candidate)
                } else {
                    pendingIce.getOrPut(from) { mutableListOf() }.add(candidate)
                }
            }
        }
    }

    fun switchCamera() {
        if (!closed) capturer?.switchCamera(null)
    }

    fun close() {
        if (closed) return
        closed = true

        MboteSocketManager.disconnectLiveWebSocket()
        scope.cancel()

        runCatching { capturer?.stopCapture() }
        capturer?.dispose()
        textureHelper?.dispose()

        peers.values.forEach { connection ->
            runCatching { connection.close() }
            connection.dispose()
        }
        peers.clear()
        pendingIce.clear()
        remoteDescriptionReady.clear()

        localVideo?.dispose()
        localAudio?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        factory.dispose()
        egl.release()
    }
}
