package com.regannian.legend.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * D4：早市小调循环 BGM（程序合成，无需额外音源文件）。
 */
class BgmPlayer {

    @Volatile
    var muted: Boolean = false
        set(value) {
            field = value
            if (value) pause() else play()
        }

    private var track: AudioTrack? = null

    fun play() {
        if (muted) {
            pause()
            return
        }
        val existing = track
        if (existing != null) {
            if (existing.playState != AudioTrack.PLAYSTATE_PLAYING) {
                existing.play()
            }
            return
        }
        val pcm = buildMelodyPcm()
        val frames = pcm.size / 2
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(pcm.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        created.write(pcm, 0, pcm.size)
        created.setLoopPoints(0, frames, -1)
        created.setVolume(0.28f)
        created.play()
        track = created
    }

    fun pause() {
        track?.pause()
    }

    fun release() {
        track?.stop()
        track?.release()
        track = null
    }

    companion object {
        private const val SAMPLE_RATE = 22_050

        /**  pentatonic-ish 早市小调，约 2.4s。 */
        private fun buildMelodyPcm(): ByteArray {
            val notes = listOf(
                523.25 to 0.18, // C5
                659.25 to 0.18, // E5
                783.99 to 0.24, // G5
                659.25 to 0.18, // E5
                880.00 to 0.18, // A5
                783.99 to 0.24, // G5
                659.25 to 0.18, // E5
                523.25 to 0.30, // C5
                392.00 to 0.24, // G4
                523.25 to 0.36, // C5
            )
            val samples = ArrayList<Short>(SAMPLE_RATE * 3)
            notes.forEach { (freq, sec) ->
                val n = (sec * SAMPLE_RATE).toInt()
                val attack = (0.02 * SAMPLE_RATE).toInt().coerceAtMost(n / 4)
                val release = (0.04 * SAMPLE_RATE).toInt().coerceAtMost(n / 3)
                for (i in 0 until n) {
                    val env = when {
                        i < attack -> i.toFloat() / attack
                        i > n - release -> (n - i).toFloat() / release
                        else -> 1f
                    }
                    val s = sin(2.0 * PI * freq * i / SAMPLE_RATE)
                    samples.add((s * env * 0.35 * Short.MAX_VALUE).toInt().toShort())
                }
            }
            val bytes = ByteArray(samples.size * 2)
            samples.forEachIndexed { i, v ->
                bytes[i * 2] = (v.toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
            }
            return bytes
        }
    }
}
