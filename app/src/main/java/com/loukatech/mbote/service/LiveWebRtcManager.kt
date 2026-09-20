package com.loukatech.mbote.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*

/**
 * Native WebRTC transport for MBoté Live.
 * Broadcaster captures camera + microphone and creates one PeerConnection per viewer.
 * Viewers receive the remote MediaStream through the same signaling channel.
 */
class LiveWebRtcManager(
    context: Context,
    private val streamId: String,
    private val broadcaster: Boolean,
    private val onLocalVideoTrack: (VideoTrack) -> Unit = {},
    private val onRemoteVideoTrack: (VideoTrack) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val egl = EglBase.create()
    private val factory: PeerConnectionFactory
    private val peers = mutableMapOf<String, PeerConnection>()
    private var capturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideo: VideoTrack? = null
    private var localAudio: AudioTrack? = null

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
        scope.launch { MboteSocketManager.liveSignals.collectLatest(::handleSignal) }
    }

    fun eglContext(): EglBase.Context = egl.eglBaseContext

    private fun startCapture() {
        val enumerator = Camera2Enumerator(appContext)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull() ?: return
        capturer = enumerator.createCapturer(name, null)
        videoSource = factory.createVideoSource(false)
        val helper = SurfaceTextureHelper.create("MBoteLiveCapture", egl.eglBaseContext)
        capturer?.initialize(helper, appContext, videoSource!!.capturerObserver)
        capturer?.startCapture(720, 1280, 30)
        localVideo = factory.createVideoTrack("MBOTE_LIVE_VIDEO", videoSource).also(onLocalVideoTrack)
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudio = factory.createAudioTrack("MBOTE_LIVE_AUDIO", audioSource)
    }

    private fun newPeer(peerId: String): PeerConnection {
        return peers.getOrPut(peerId) {
            val ice = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
            factory.createPeerConnection(ice, object : PeerConnection.Observer {
                override fun onIceCandidate(c: IceCandidate) {
                    MboteSocketManager.sendLiveSignal(streamId, "ICE", peerId, candidate=c.sdp, sdpMid=c.sdpMid, sdpMLineIndex=c.sdpMLineIndex)
                }
                override fun onAddStream(stream: MediaStream) { stream.videoTracks.firstOrNull()?.let(onRemoteVideoTrack) }
                override fun onTrack(transceiver: RtpTransceiver?) {
                    (transceiver?.receiver?.track() as? VideoTrack)?.let(onRemoteVideoTrack)
                }
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            })!!.also { pc ->
                if (broadcaster) {
                    localVideo?.let { pc.addTrack(it, listOf("mbote-live")) }
                    localAudio?.let { pc.addTrack(it, listOf("mbote-live")) }
                }
            }
        }
    }

    private fun sdpObserver(onCreated: (SessionDescription) -> Unit = {}): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) = onCreated(sdp)
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }

    /** Viewer announces readiness; broadcaster answers by creating an offer for that viewer. */
    fun requestStream() {
        if (!broadcaster) MboteSocketManager.sendLiveSignal(streamId, "OFFER", sdp="REQUEST_STREAM")
    }

    private fun handleSignal(data: JSONObject) {
        if (data.optString("streamId") != streamId) return
        val from = data.optString("fromUserId")
        if (from.isBlank()) return
        when (data.optString("signalType")) {
            "OFFER" -> {
                val sdp = data.optString("sdp")
                if (broadcaster && sdp == "REQUEST_STREAM") {
                    val pc = newPeer(from)
                    pc.createOffer(sdpObserver { offer ->
                        pc.setLocalDescription(sdpObserver(), offer)
                        MboteSocketManager.sendLiveSignal(streamId, "OFFER", from, sdp=offer.description)
                    }, MediaConstraints())
                } else if (!broadcaster && sdp.isNotBlank()) {
                    val pc = newPeer(from)
                    pc.setRemoteDescription(sdpObserver(), SessionDescription(SessionDescription.Type.OFFER, sdp))
                    pc.createAnswer(sdpObserver { answer ->
                        pc.setLocalDescription(sdpObserver(), answer)
                        MboteSocketManager.sendLiveSignal(streamId, "ANSWER", from, sdp=answer.description)
                    }, MediaConstraints())
                }
            }
            "ANSWER" -> {
                if (broadcaster) peers[from]?.setRemoteDescription(
                    sdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp"))
                )
            }
            "ICE" -> {
                val pc = newPeer(from)
                pc.addIceCandidate(IceCandidate(data.optString("sdpMid"), data.optInt("sdpMLineIndex"), data.optString("candidate")))
            }
        }
    }

    fun switchCamera() { capturer?.switchCamera(null) }

    fun close() {
        runCatching { capturer?.stopCapture() }
        capturer?.dispose()
        peers.values.forEach { it.close(); it.dispose() }
        peers.clear()
        localVideo?.dispose(); localAudio?.dispose()
        videoSource?.dispose(); audioSource?.dispose()
        factory.dispose(); egl.release()
        MboteSocketManager.disconnectLiveWebSocket()
    }
}
