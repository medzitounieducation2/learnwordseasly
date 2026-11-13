package com.example.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Map des rappels exécutés à la fin de prononciation d'un utteranceId
    private val completionCallbacks = mutableMapOf<String, () -> Unit>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null) {
                        val callback = completionCallbacks.remove(utteranceId)
                        if (callback != null) {
                            mainHandler.post { callback.invoke() }
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId != null) {
                        val callback = completionCallbacks.remove(utteranceId)
                        if (callback != null) {
                            mainHandler.post { callback.invoke() }
                        }
                    }
                }
            })
        }
    }

    /**
     * Définit la langue de synthèse vocale (ex: "fr", "en", "es", "ar").
     */
    fun setLanguage(languageCode: String) {
        if (!isReady) return
        val locale = when (languageCode.lowercase()) {
            "en" -> Locale.ENGLISH
            "fr" -> Locale.FRENCH
            "es" -> Locale.forLanguageTag("es")
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            "ar" -> Locale.forLanguageTag("ar")
            else -> Locale.FRENCH
        }
        tts?.language = locale
    }

    /**
     * Prononce un texte avec option de langue et de rappel à la fin.
     */
    fun speak(
        text: String,
        languageCode: String = "fr",
        utteranceId: String = "vocab_tts_${System.currentTimeMillis()}",
        onDone: (() -> Unit)? = null
    ) {
        if (isReady && text.isNotBlank()) {
            setLanguage(languageCode)
            if (onDone != null) {
                completionCallbacks[utteranceId] = onDone
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            onDone?.invoke()
        }
    }

    /**
     * Arrête la prononciation en cours et efface les rappels.
     */
    fun stop() {
        completionCallbacks.clear()
        tts?.stop()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
