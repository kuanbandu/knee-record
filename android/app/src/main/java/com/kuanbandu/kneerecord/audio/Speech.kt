package com.kuanbandu.kneerecord.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 报数用的语音。初始化是异步的，没就绪就安静跳过，不打断训练 */
class Speech(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun say(text: String) {
        if (!ready) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "knee-cue") }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
