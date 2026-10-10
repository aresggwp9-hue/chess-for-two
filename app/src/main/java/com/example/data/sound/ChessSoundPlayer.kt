package com.example.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

/**
 * Синтезатор звуков перемещения фигур и тактильная отдача.
 * Работает полностью автономно без внешних аудиофайлов.
 */
class ChessSoundPlayer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Предварительно сгенерированные PCM буферы звуков
    private val moveSoundBytes: ShortArray by lazy { generateClickPcm(isCapture = false) }
    private val captureSoundBytes: ShortArray by lazy { generateClickPcm(isCapture = true) }

    fun playMoveFeedback(isCapture: Boolean, soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) {
            scope.launch {
                playPcmSound(if (isCapture) captureSoundBytes else moveSoundBytes)
            }
        }
        if (vibrationEnabled) {
            triggerVibration(isCapture)
        }
    }

    private fun triggerVibration(isCapture: Boolean) {
        vibrator?.let { v ->
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (isCapture) 45L else 25L
                val amplitude = if (isCapture) 220 else 140
                v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(if (isCapture) 40L else 20L)
            }
        }
    }

    private fun playPcmSound(pcmData: ShortArray) {
        try {
            val sampleRate = 44100
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
            // Освобождаем ресурс после воспроизведения
            scope.launch {
                kotlinx.coroutines.delay(200)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Игнорируем сбои аудиоустройств
        }
    }

    /**
     * Синтезирует короткий мягкий деревянный щелчок доски (35-50 мс).
     */
    private fun generateClickPcm(isCapture: Boolean): ShortArray {
        val sampleRate = 44100
        val durationMs = if (isCapture) 55 else 35
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val baseFreq = if (isCapture) 440.0 else 280.0
        val secondFreq = if (isCapture) 720.0 else 180.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Быстро затухающая экспонента
            val decay = exp(-t * 90.0)
            val wave = (sin(2.0 * Math.PI * baseFreq * t) * 0.7 +
                    sin(2.0 * Math.PI * secondFreq * t) * 0.3)
            val sample = (wave * decay * Short.MAX_VALUE * 0.65).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
