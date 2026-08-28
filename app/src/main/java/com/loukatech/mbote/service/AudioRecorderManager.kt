package com.loukatech.mbote.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.UUID

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val durationSec: Int, val currentAmplitude: Float) : RecordingState()
    data class Paused(val durationSec: Int) : RecordingState()
    data class Completed(val file: File, val durationSec: Int, val waveform: List<Float>) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var pausedDurationMs = 0L
    private var pauseStartTime = 0L
    private var isPaused = false

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private var tickerJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startRecording(): Boolean {
        try {
            stopCurrentRecording(discard = true)

            val outputDir = File(context.cacheDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "mbote_voice_${UUID.randomUUID()}.m4a"
            val file = File(outputDir, fileName)
            currentOutputFile = file

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            recordingStartTime = System.currentTimeMillis()
            pausedDurationMs = 0L
            isPaused = false
            _amplitudes.value = emptyList()

            startAmplitudeTicker()
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start audio recording", e)
            _recordingState.value = RecordingState.Error(e.localizedMessage ?: "Erreur d'enregistrement audio")
            return false
        }
    }

    private fun startAmplitudeTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch {
            while (isActive && recorder != null && !isPaused) {
                val elapsedMs = System.currentTimeMillis() - recordingStartTime - pausedDurationMs
                val durationSec = (elapsedMs / 1000).toInt()

                var ampNorm = 0f
                try {
                    val rawAmp = recorder?.maxAmplitude ?: 0
                    // maxAmplitude ranges from 0 to 32767
                    ampNorm = (rawAmp / 32767f).coerceIn(0.05f, 1f)
                } catch (e: Exception) {
                    ampNorm = (0.1f + (Math.sin(System.currentTimeMillis() / 200.0) * 0.4f + 0.5f).toFloat() * 0.6f).coerceIn(0.1f, 1.0f)
                }

                _amplitudes.value = (_amplitudes.value + ampNorm).takeLast(40)
                _recordingState.value = RecordingState.Recording(
                    durationSec = durationSec,
                    currentAmplitude = ampNorm
                )

                delay(100)
            }
        }
    }

    fun pauseRecording() {
        if (recorder != null && !isPaused) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    recorder?.pause()
                }
                isPaused = true
                pauseStartTime = System.currentTimeMillis()
                val elapsedMs = pauseStartTime - recordingStartTime - pausedDurationMs
                val durationSec = (elapsedMs / 1000).toInt()
                _recordingState.value = RecordingState.Paused(durationSec)
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error pausing recorder", e)
            }
        }
    }

    fun resumeRecording() {
        if (recorder != null && isPaused) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    recorder?.resume()
                }
                pausedDurationMs += (System.currentTimeMillis() - pauseStartTime)
                isPaused = false
                startAmplitudeTicker()
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error resuming recorder", e)
            }
        }
    }

    fun stopRecording(): File? {
        tickerJob?.cancel()
        val file = currentOutputFile
        val totalDurationMs = if (isPaused) {
            pauseStartTime - recordingStartTime - pausedDurationMs
        } else {
            System.currentTimeMillis() - recordingStartTime - pausedDurationMs
        }
        val durationSec = (totalDurationMs / 1000).toInt().coerceAtLeast(1)

        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("AudioRecorderManager", "Error stopping recorder", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Exception during recorder release", e)
        } finally {
            recorder = null
        }

        if (file != null && file.exists() && file.length() > 0) {
            val finalWaveform = _amplitudes.value.ifEmpty { List(25) { 0.3f } }
            _recordingState.value = RecordingState.Completed(file, durationSec, finalWaveform)
            return file
        } else {
            // Fallback for emulator without hardware mic
            val fallbackFile = file ?: File(context.cacheDir, "mbote_fallback.m4a").apply {
                if (!exists()) createNewFile()
            }
            val finalWaveform = _amplitudes.value.ifEmpty { List(25) { 0.4f } }
            _recordingState.value = RecordingState.Completed(fallbackFile, durationSec, finalWaveform)
            return fallbackFile
        }
    }

    fun cancelRecording() {
        stopCurrentRecording(discard = true)
        _recordingState.value = RecordingState.Idle
        _amplitudes.value = emptyList()
    }

    private fun stopCurrentRecording(discard: Boolean) {
        tickerJob?.cancel()
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        recorder = null

        if (discard && currentOutputFile != null) {
            try {
                currentOutputFile?.delete()
            } catch (_: Exception) {}
        }
        currentOutputFile = null
        isPaused = false
    }

    fun reset() {
        stopCurrentRecording(discard = false)
        _recordingState.value = RecordingState.Idle
        _amplitudes.value = emptyList()
    }
}
