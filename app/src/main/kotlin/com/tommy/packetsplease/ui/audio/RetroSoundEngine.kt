package com.tommy.packetsplease.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object RetroSoundEngine {

  private val executor = Executors.newSingleThreadExecutor()
  @Volatile
  var muted: Boolean = false

  fun playStampSound(isQuarantine: Boolean = false) {
    if (muted) return
    playPcm { sampleRate ->
      val durationMs = 110
      val count = sampleRate * durationMs / 1000
      ShortArray(count) { i ->
        val t = i.toDouble() / sampleRate
        val env = exp(-t * 28.0)
        val thud = sin(2.0 * Math.PI * (if (isQuarantine) 190.0 else 130.0) * t) * 0.7
        val click = (Random.nextDouble() * 2.0 - 1.0) * exp(-t * 95.0) * 0.45
        ((thud + click) * env * 14000).toInt().coerceIn(-32767, 32767).toShort()
      }
    }
  }

  fun playPassChime() {
    if (muted) return
    playNotes(listOf(523.25 to 55, 659.25 to 75))
  }

  fun playBlockTone() {
    if (muted) return
    playNotes(listOf(220.0 to 70, 164.81 to 90))
  }

  fun playAlertWarning() {
    if (muted) return
    playNotes(listOf(880.0 to 65, 440.0 to 75, 880.0 to 95))
  }

  fun playInspectMatch(foundDiscrepancy: Boolean) {
    if (muted) return
    if (foundDiscrepancy) {
      playNotes(listOf(587.33 to 55, 880.0 to 90))
    } else {
      playNotes(listOf(392.0 to 55))
    }
  }

  fun playStartupJingle() {
    if (muted) return
    playNotes(listOf(329.63 to 70, 440.0 to 70, 523.25 to 80, 659.25 to 120))
  }

  private fun playNotes(notes: List<Pair<Double, Int>>) {
    playPcm { sampleRate ->
      val segments = notes.map { (freq, ms) ->
        val len = sampleRate * ms / 1000
        ShortArray(len) { i ->
          val t = i.toDouble() / sampleRate
          val env = (1.0 - i.toDouble() / len).coerceIn(0.0, 1.0)
          val sq = if (sin(2.0 * Math.PI * freq * t) >= 0) 0.5 else -0.5
          (sq * env * 9000).toInt().toShort()
        }
      }
      val total = segments.sumOf { it.size }
      val out = ShortArray(total)
      var offset = 0
      for (seg in segments) {
        seg.copyInto(out, offset)
        offset += seg.size
      }
      out
    }
  }

  private fun playPcm(generator: (sampleRate: Int) -> ShortArray) {
    executor.execute {
      runCatching {
        val sampleRate = 22050
        val samples = generator(sampleRate)
        if (samples.isEmpty()) return@execute
        val minBuf = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferBytes = maxOf(minBuf, samples.size * 2)
        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build(),
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build(),
          )
          .setBufferSizeInBytes(bufferBytes)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(samples, 0, samples.size)
        track.play()
        val durationMs = (samples.size * 1000L / sampleRate) + 40L
        Thread.sleep(durationMs)
        track.stop()
        track.release()
      }
    }
  }
}
