package com.kuanbandu.kneerecord.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.net.Uri
import com.kuanbandu.kneerecord.model.MetronomeSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val RATE = 44100

/** 场景提示音：休息倒数短音 / 结束长音 */
enum class Cue { COUNTDOWN, FINISH }

/**
 * 节拍器。音是代码合成的，不依赖任何音频文件；
 * 也支持把用户选的音频文件当节拍音（SoundPool 解码后按拍播放）。
 */
class Metronome(private val context: Context) {

    private var pool: SoundPool? = null
    private var customId = 0
    private var customReady = false
    private val cache = HashMap<String, ShortArray>()

    /** 绑定自选音频。返回是否成功打开文件（解码完成是异步的，未就绪时会退回木鱼） */
    fun setCustom(uriString: String): Boolean {
        releaseCustom()
        if (uriString.isBlank()) return false
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        val fd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }
            .getOrNull() ?: return false
        val p = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        p.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                customId = sampleId
                customReady = true
            }
        }
        customId = p.load(fd, 0, -1, 1)
        runCatching { fd.close() }
        pool = p
        return true
    }

    fun hasCustom(): Boolean = customReady

    /** 打一拍。strong = 重音（每小节第一拍） */
    fun click(sound: MetronomeSound, strong: Boolean, volume: Float) {
        if (sound == MetronomeSound.CUSTOM && customReady) {
            runCatching { pool?.play(customId, volume, volume, 1, 0, 1f) }
            return
        }
        play(render(sound, strong), volume)
    }

    fun cue(kind: Cue, volume: Float) = play(renderCue(kind), volume)

    private fun play(pcm: ShortArray, volume: Float) {
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val fmt = AudioFormat.Builder()
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(fmt)
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.setVolume(volume.coerceIn(0f, 1f))
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack?) {
                        runCatching { t?.stop(); t?.release() }
                    }

                    override fun onPeriodicNotification(t: AudioTrack?) = Unit
                },
            )
            track.write(pcm, 0, pcm.size)
            track.play()
        }
    }

    // ------------------------------------------------------------ 合成

    private fun render(sound: MetronomeSound, strong: Boolean): ShortArray {
        val key = "${sound.key}_$strong"
        return cache.getOrPut(key) {
            when (sound) {
                MetronomeSound.WOOD -> wood(strong)
                MetronomeSound.TICK -> tick(strong)
                MetronomeSound.BELL -> bell(strong)
                MetronomeSound.CUSTOM -> wood(strong)
            }
        }
    }

    private fun renderCue(kind: Cue): ShortArray = cache.getOrPut("cue_$kind") {
        when (kind) {
            Cue.COUNTDOWN -> block(90) { t ->
                val d = t.toDouble()
                (sin(2 * PI * 1300 * d) * exp(-d / 0.030)).toFloat() * 0.85f
            }
            Cue.FINISH -> block(600) { t ->
                val d = t.toDouble()
                val f = 700.0 + 260.0 * (d / 0.6).coerceIn(0.0, 1.0)
                (sin(2 * PI * f * d) * exp(-d / 0.28)).toFloat() * 0.7f
            }
        }
    }

    private fun wood(strong: Boolean) = block(if (strong) 90 else 70) { t ->
        val d = t.toDouble()
        val f = if (strong) 1500.0 else 1050.0
        val tau = if (strong) 0.030 else 0.022
        val click = if (d < 0.004) (Random.nextFloat() * 2 - 1).toDouble() * 0.6 else 0.0
        (click + sin(2 * PI * f * d) * exp(-d / tau) * 0.9).toFloat()
    }

    private fun tick(strong: Boolean) = block(if (strong) 40 else 30) { t ->
        val d = t.toDouble()
        val f = if (strong) 3200.0 else 2400.0
        val tau = if (strong) 0.010 else 0.007
        val noise = (Random.nextFloat() * 2 - 1).toDouble() * exp(-d / 0.0025)
        (noise * 0.7 + sin(2 * PI * f * d) * exp(-d / tau) * 0.8).toFloat()
    }

    private fun bell(strong: Boolean) = block(if (strong) 520 else 380) { t ->
        val d = t.toDouble()
        val base = if (strong) 990.0 else 880.0
        val env = exp(-d / (if (strong) 0.22 else 0.16))
        val body = sin(2 * PI * base * d) +
            0.5 * sin(2 * PI * base * 2 * d) +
            0.25 * sin(2 * PI * base * 3 * d)
        (body * env * 0.5).toFloat()
    }

    /** 生成一段 PCM；(t 为秒) -> 振幅 -1..1 */
    private fun block(ms: Int, fn: (Float) -> Float): ShortArray {
        val n = RATE * ms / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val v = fn(i.toFloat() / RATE).coerceIn(-1f, 1f)
            out[i] = (v * 32767f).toInt().toShort()
        }
        return out
    }

    fun release() {
        releaseCustom()
        cache.clear()
    }

    private fun releaseCustom() {
        runCatching { pool?.release() }
        pool = null
        customId = 0
        customReady = false
    }
}
