package com.loukatech.mbote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Compose wrapper around WebRTC SurfaceViewRenderer.
 * The sink is always detached and released when the track leaves composition.
 */
@Composable
fun RtcVideoSurface(
    track: VideoTrack,
    eglContext: EglBase.Context,
    mirror: Boolean,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
) {
    val context = LocalContext.current
    val renderer = remember(track, eglContext, mirror, overlay) {
        SurfaceViewRenderer(context).apply {
            init(eglContext, null)
            setMirror(mirror)
            setEnableHardwareScaler(true)
            setZOrderMediaOverlay(overlay)
        }
    }

    DisposableEffect(renderer, track) {
        track.addSink(renderer)
        onDispose {
            runCatching { track.removeSink(renderer) }
            runCatching { renderer.release() }
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier,
    )
}
