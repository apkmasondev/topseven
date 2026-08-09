package com.topseven.fakty.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("pl", "PL")
            val result = tts?.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Język polski nie jest wspierany przez silnik TTS.")
                _isReady.value = false
            } else {
                _isReady.value = true
                Log.d("TtsManager", "Silnik TTS zainicjowany pomyślnie.")

                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                    }

                    @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                    }
                })
            }
        } else {
            Log.e("TtsManager", "Inicjalizacja TTS nie powiodła się.")
            _isReady.value = false
        }
    }

    fun speak(text: String) {
        if (_isReady.value && tts != null) {
            if (_isPlaying.value) {
                stop()
            } else {
                val cleanText = sanitizeTextForSpeech(text)
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
            }
        }
    }

    fun stop() {
        if (_isPlaying.value) {
            tts?.stop()
            _isPlaying.value = false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _isPlaying.value = false
    }

    private fun sanitizeTextForSpeech(text: String): String {
        return text.replace(Regex("\\[.*?\\]"), "")
                   .replace(Regex("\\(.*?\\)"), "")
    }
}
