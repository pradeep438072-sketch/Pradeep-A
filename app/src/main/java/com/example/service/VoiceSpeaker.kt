package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceSpeaker private constructor(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceSpeaker", "Language is not supported")
            } else {
                isInitialized = true
                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        } else {
            Log.e("VoiceSpeaker", "TTS Initialization failed")
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (isInitialized && tts != null) {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, "medivoice_tts_${System.currentTimeMillis()}")
        } else {
            pendingText = text
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        @Volatile
        private var INSTANCE: VoiceSpeaker? = null

        fun getInstance(context: Context): VoiceSpeaker {
            return INSTANCE ?: synchronized(this) {
                val instance = VoiceSpeaker(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
