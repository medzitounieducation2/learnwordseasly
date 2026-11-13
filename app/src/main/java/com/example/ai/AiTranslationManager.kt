package com.example.ai

import com.example.translation.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UsageExample(
    val textOrigine: String,
    val traduction: String,
    val countListen: Int = 0,
    val countRead: Int = 0
)

data class AiTranslationResult(
    val traduction: String,
    val exemples: List<UsageExample>
)

object AiTranslationManager {

    private const val API_KEY = "use-your-key"
    private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Analyse une chaîne JSON décrivant des exemples d'utilisation.
     * Supporte les formats `{"exemples": [...]}` et `[...]`.
     */
    fun parseExamplesJson(jsonStr: String): List<UsageExample> {
        val clean = jsonStr.trim()
        if (clean.isEmpty()) return emptyList()
        val list = mutableListOf<UsageExample>()
        try {
            if (clean.startsWith("{")) {
                val root = JSONObject(clean)
                val array = root.optJSONArray("exemples") ?: JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val orig = obj.optString("text_origine", obj.optString("text-origine", ""))
                    val trad = obj.optString("traduction", "")
                    val listen = obj.optInt("count_listen", 0)
                    val read = obj.optInt("count_read", 0)
                    if (orig.isNotBlank() || trad.isNotBlank()) {
                        list.add(UsageExample(orig, trad, listen, read))
                    }
                }
            } else if (clean.startsWith("[")) {
                val array = JSONArray(clean)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val orig = obj.optString("text_origine", obj.optString("text-origine", ""))
                    val trad = obj.optString("traduction", "")
                    val listen = obj.optInt("count_listen", 0)
                    val read = obj.optInt("count_read", 0)
                    if (orig.isNotBlank() || trad.isNotBlank()) {
                        list.add(UsageExample(orig, trad, listen, read))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Convertit une liste d'exemples d'utilisation en chaîne JSON sérialisée.
     */
    fun toJsonString(exemples: List<UsageExample>): String {
        if (exemples.isEmpty()) return ""
        val root = JSONObject()
        val array = JSONArray()
        for (ex in exemples) {
            val obj = JSONObject()
            obj.put("text_origine", ex.textOrigine)
            obj.put("traduction", ex.traduction)
            obj.put("count_listen", ex.countListen)
            obj.put("count_read", ex.countRead)
            array.put(obj)
        }
        root.put("exemples", array)
        return root.toString()
    }

    /**
     * Effectue une requête optimisée à l'API ChatGPT (gpt-4o-mini)
     * pour traduire un mot et fournir au maximum 2 exemples d'utilisation courts.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun translateAndGetExamples(
        word: String,
        contextSentence: String = "",
        sourceLangCode: String,
        targetLangCode: String
    ): Result<AiTranslationResult> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Le mot à traduire est vide."))
        }

        val srcName = TranslationManager.getLanguage(sourceLangCode).displayName
        val tgtName = TranslationManager.getLanguage(targetLangCode).displayName

        val promptBuilder = StringBuilder()
        promptBuilder.append("Traduis le mot/expression \"$cleanWord\" de la langue $srcName ($sourceLangCode) vers la langue $tgtName ($targetLangCode).")
        promptBuilder.append(" Fournis une traduction ultra-précise de ce mot et au maximum 2 courtes phrases d'exemples d'utilisation du mot avec leurs traductions.")
        promptBuilder.append(" Format JSON strict obligatoire :\n{\n  \"traduction\": \"...\",\n  \"exemples\": [\n    {\"text_origine\": \"...\", \"traduction\": \"...\"}\n  ]\n}")

        val systemContent = "Tu es un assistant de traduction dictionnaire expert, concis et ultra-précis. Tu réponds STRICTEMENT au format JSON valide sans aucun formatage markdown."

        try {
            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.2)
                put("response_format", JSONObject().put("type", "json_object"))
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemContent)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", promptBuilder.toString())
                    })
                }
                put("messages", messages)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                val errorMsg = if (!responseBody.isNullOrEmpty() && responseBody.contains("error")) {
                    try {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message", "Erreur API ${response.code}")
                    } catch (e: Exception) {
                        "Erreur HTTP ${response.code}"
                    }
                } else {
                    "Erreur HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Échec IA : $errorMsg"))
            }

            val rootObj = JSONObject(responseBody)
            val choices = rootObj.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("Réponse IA vide"))
            }

            val contentStr = choices.getJSONObject(0).getJSONObject("message").getString("content")
            val resultJson = JSONObject(contentStr)

            val traduction = resultJson.optString("traduction", "")
            val exemplesArray = resultJson.optJSONArray("exemples") ?: JSONArray()
            val exemplesList = mutableListOf<UsageExample>()

            for (i in 0 until minOf(exemplesArray.length(), 2)) {
                val item = exemplesArray.optJSONObject(i) ?: continue
                val textOrigine = item.optString("text_origine", item.optString("text-origine", ""))
                val exTraduction = item.optString("traduction", "")
                if (textOrigine.isNotBlank() || exTraduction.isNotBlank()) {
                    exemplesList.add(UsageExample(textOrigine, exTraduction))
                }
            }

            Result.success(AiTranslationResult(traduction, exemplesList))
        } catch (e: Exception) {
            Result.failure(Exception("Erreur IA : ${e.localizedMessage ?: "Erreur réseau"}", e))
        }
    }
}
