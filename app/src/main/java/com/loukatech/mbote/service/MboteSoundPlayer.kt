package com.loukatech.mbote.service

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

object MboteSoundPlayer {
    private const val SAMPLE_RATE = 44100

    fun playSound(soundName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (soundName) {
                    "MBoté Crystal" -> playCrystal()
                    "MBoté Echo" -> playEcho()
                    "MBoté Sunset" -> playSunset()
                    "MBoté Rhythm" -> playRhythm()
                    "Marimba" -> playMarimba()
                    "Pop" -> playPop()
                }
            } catch (e: Exception) {
                Log.e("MboteSoundPlayer", "Error playing sound: $soundName", e)
            }
        }
    }

    private fun playTone(frequencies: List<Float>, durationMs: Int, envelope: (Float) -> Float) {
        try {
            val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
            val generatedSnd = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                var sampleVal = 0f
                for (f in frequencies) {
                    sampleVal += sin(2.0 * Math.PI * f * t).toFloat()
                }
                sampleVal /= frequencies.size
                
                val env = envelope(i.toFloat() / numSamples)
                generatedSnd[i] = (sampleVal * env * Short.MAX_VALUE).toInt().toShort()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                numSamples * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, numSamples)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 50L)
            audioTrack.release()
        } catch (e: Exception) {
            Log.e("MboteSoundPlayer", "Tone playback failed", e)
        }
    }

    private fun playCrystal() {
        playTone(listOf(1500f, 2200f, 3000f), 400) { progress ->
            exp(-progress * 6f)
        }
    }

    private fun playEcho() {
        for (i in 0..2) {
            val volume = 1.0f - (i * 0.35f)
            playTone(listOf(1300f), 120) { progress ->
                (1f - progress) * volume
            }
            Thread.sleep(90)
        }
    }

    private fun playSunset() {
        playTone(listOf(523.25f, 659.25f, 783.99f), 600) { progress ->
            if (progress < 0.15f) progress / 0.15f
            else 1f - (progress - 0.15f) / 0.85f
        }
    }

    private fun playRhythm() {
        playTone(listOf(750f), 80) { progress -> 1f - progress }
        Thread.sleep(90)
        playTone(listOf(1150f), 120) { progress -> 1f - progress }
    }

    private fun playMarimba() {
        playTone(listOf(587.33f, 1174.66f), 220) { progress ->
            exp(-progress * 8f)
        }
    }

    private fun playPop() {
        playTone(listOf(950f, 1350f), 140) { progress ->
            val env = 1f - progress
            env * env
        }
    }
}
