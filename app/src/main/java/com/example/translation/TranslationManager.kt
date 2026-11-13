package com.example.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val flag: String
)

object TranslationManager {

    val supportedLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage("fr", "Français", "🇫🇷"),
        SupportedLanguage("en", "Anglais", "🇬🇧"),
        SupportedLanguage("ar", "Arabe", "🇸🇦"),
        SupportedLanguage("es", "Espagnol", "🇪🇸"),
        SupportedLanguage("de", "Allemand", "🇩🇪"),
        SupportedLanguage("it", "Italien", "🇮🇹"),
        SupportedLanguage("pt", "Portugais", "🇵🇹"),
        SupportedLanguage("ru", "Russe", "🇷🇺"),
        SupportedLanguage("zh", "Chinois", "🇨🇳"),
        SupportedLanguage("ja", "Japonais", "🇯🇵")
    )

    fun getLanguage(code: String): SupportedLanguage {
        return supportedLanguages.find { it.code.equals(code, ignoreCase = true) }
            ?: SupportedLanguage(code, code.uppercase(), "🌐")
    }

    /**
     * Traduit un mot ou texte court d'une langue source vers une langue cible
     * en utilisant Google ML Kit Translate on-device.
     */
    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Le texte à traduire est vide."))
        }

        val sourceLang = TranslateLanguage.fromLanguageTag(sourceLangCode) ?: sourceLangCode
        val targetLang = TranslateLanguage.fromLanguageTag(targetLangCode) ?: targetLangCode

        if (sourceLang.equals(targetLang, ignoreCase = true)) {
            return@withContext Result.success(cleanText)
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)

        try {
            // Télécharge le modèle léger si nécessaire (connexion initiale requise une seule fois)
            val conditions = DownloadConditions.Builder().build()
            suspendCancellableCoroutine<Unit> { cont ->
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        if (cont.isActive) cont.resume(Unit)
                    }
                    .addOnFailureListener { exception ->
                        if (cont.isActive) cont.resumeWithException(exception)
                    }
            }

            // Traduction on-device
            val translated = suspendCancellableCoroutine<String> { cont ->
                translator.translate(cleanText)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result)
                    }
                    .addOnFailureListener { exception ->
                        if (cont.isActive) cont.resumeWithException(exception)
                    }
            }

            Result.success(translated)
        } catch (e: Exception) {
            val srcName = getLanguage(sourceLangCode).displayName
            val tgtName = getLanguage(targetLangCode).displayName
            val message = when {
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("download", ignoreCase = true) == true ->
                    "Téléchargement du pack $srcName → $tgtName requis. Activez internet pour la première utilisation."
                e.message?.contains("model", ignoreCase = true) == true ->
                    "Modèle de langue $srcName → $tgtName en cours de préparation ou non disponible."
                else ->
                    "Échec de la traduction ($srcName → $tgtName) : ${e.localizedMessage ?: "Erreur inconnue"}"
            }
            Result.failure(Exception(message, e))
        } finally {
            translator.close()
        }
    }
}
