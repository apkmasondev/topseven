package com.topseven.fakty.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Opakowanie na systemowy silnik [TextToSpeech] czytający fakty po polsku.
 *
 * Cykl życia jest związany z [com.topseven.fakty.ui.screens.MainViewModel]: instancja przeżywa
 * zmiany konfiguracji (odtwarzanie nie urywa się przy np. zmianie motywu systemowego),
 * a [shutdown] wołany jest z `onCleared()`.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        // Konstruktor TextToSpeech nie rzuca wyjątku przy braku silnika w systemie - zgłasza
        // to dopiero przez status ERROR w onInit(). Zabezpieczamy się jednak także tutaj,
        // bo część nakładek producenckich potrafi rzucić wyjątkiem przy braku usługi TTS.
        tts = try {
            TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Nie udało się utworzyć silnika TTS.", e)
            null
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Inicjalizacja TTS nie powiodła się (status=$status).")
            _isReady.value = false
            return
        }

        val engine = tts
        if (engine == null) {
            _isReady.value = false
            return
        }

        val result = try {
            engine.setLanguage(POLISH)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd przy ustawianiu języka polskiego.", e)
            TextToSpeech.LANG_NOT_SUPPORTED
        }

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Język polski nie jest wspierany przez zainstalowany silnik TTS.")
            _isReady.value = false
            return
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _isPlaying.value = false
            }

            @Deprecated("Wymagane przez UtteranceProgressListener dla API < 21")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "Błąd odtwarzania TTS (errorCode=$errorCode).")
                _isPlaying.value = false
            }
        })

        _isReady.value = true
    }

    /**
     * Czyta podany tekst. Jeśli coś jest już odtwarzane, zatrzymuje odtwarzanie (zachowanie
     * przełącznika play/stop). Wywołanie jest bezpieczne także gdy silnik nie jest gotowy.
     */
    fun speak(text: String) {
        val engine = tts ?: return
        if (!_isReady.value) return

        if (_isPlaying.value) {
            stop()
            return
        }

        val cleanText = sanitizeTextForSpeech(text)
        if (cleanText.isBlank()) return

        // Ustawiamy stan optymistycznie: onStart() przychodzi z opóźnieniem, a bez tego
        // szybkie dwukrotne kliknięcie uruchamiało dwie wypowiedzi zamiast play/stop.
        _isPlaying.value = true
        val result = engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        if (result == TextToSpeech.ERROR) {
            Log.w(TAG, "Silnik TTS odrzucił żądanie odtwarzania.")
            _isPlaying.value = false
        }
    }

    fun stop() {
        val engine = tts ?: return
        if (_isPlaying.value) {
            engine.stop()
            _isPlaying.value = false
        }
    }

    /** Zwalnia silnik. Bezpieczne do wielokrotnego wywołania. */
    fun shutdown() {
        tts?.let { engine ->
            try {
                engine.setOnUtteranceProgressListener(null)
                engine.stop()
                engine.shutdown()
            } catch (e: Exception) {
                Log.w(TAG, "Błąd przy zwalnianiu silnika TTS.", e)
            }
        }
        tts = null
        _isReady.value = false
        _isPlaying.value = false
    }

    private fun sanitizeTextForSpeech(text: String): String {
        return text.replace(BRACKETS_REGEX, "")
            .replace(PARENTHESES_REGEX, "")
            .trim()
    }

    private companion object {
        const val TAG = "TtsManager"
        const val UTTERANCE_ID = "tts_utterance"
        val POLISH: Locale = Locale.forLanguageTag("pl-PL")
        val BRACKETS_REGEX = Regex("\\[.*?]")
        val PARENTHESES_REGEX = Regex("\\(.*?\\)")
    }
}
