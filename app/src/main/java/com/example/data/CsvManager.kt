package com.example.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class ParsedCsvWord(
    val word: String,
    val translation: String = "",
    val contextSentence: String = "",
    val aiExamplesJson: String = "",
    val nbrRevisions: Int = 0,
    val lastRevisionDate: Long = 0L,
    val translationRecognized: Int = 0,
    val translationNotRecognized: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class ParsedCsvData(
    val name: String,
    val description: String = "",
    val sourceLanguage: String = "fr",
    val targetLanguage: String = "en",
    val words: List<ParsedCsvWord> = emptyList()
)

object CsvManager {

    /**
     * Exporte une liste et tous ses mots dans un fichier CSV (délimiteur ;).
     * Inclut le marqueur BOM UTF-8 (\uFEFF) pour forcer la détection de l'encodage (arabe, cyrillique, etc.) dans Google Drive / Sheets / Excel.
     * Structure CSV : mot;traduction;contexte;exemples;nbr_revisions;derniere_revision;traduction_reconnue;traduction_non_reconnue;date_creation
     */
    fun exportListToCsv(
        context: Context,
        uri: Uri,
        list: VocabularyListEntity,
        words: List<VocabularyEntity>
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                    // Écrit le BOM UTF-8 pour la compatibilité Google Drive / Excel / Google Sheets
                    writer.write("\uFEFF")
                    writer.write("Nom: ${escapeCsvField(list.name)}\n")
                    writer.write("Description: ${escapeCsvField(list.description)}\n")
                    writer.write("Langues: ${list.sourceLanguage}-${list.targetLanguage}\n")
                    writer.write("mot;traduction;contexte;exemples;nbr_revisions;derniere_revision;traduction_reconnue;traduction_non_reconnue;date_creation\n")
                    words.forEach { w ->
                        val line = listOf(
                            escapeCsvField(w.word),
                            escapeCsvField(w.translation),
                            escapeCsvField(w.contextSentence),
                            escapeCsvField(w.aiExamplesJson),
                            w.nbrRevisions.toString(),
                            w.lastRevisionDate.toString(),
                            w.translationRecognized.toString(),
                            w.translationNotRecognized.toString(),
                            w.timestamp.toString()
                        ).joinToString(";")
                        writer.write("$line\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Importe et analyse un fichier CSV sélectionné par l'utilisateur.
     * Utilise un parser conforme RFC 4180 gérant le UTF-8 avec/sans BOM, ainsi que les champs multi-lignes.
     * Supporte les formats à 4 colonnes, 5 colonnes et 9 colonnes.
     */
    fun importListFromCsv(context: Context, uri: Uri): ParsedCsvData? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fullText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }

                if (fullText.isBlank()) return null

                val cleanText = if (fullText.startsWith("\uFEFF")) {
                    fullText.substring(1)
                } else {
                    fullText
                }

                val records = parseCsvRecords(cleanText, delimiter = ';')
                if (records.isEmpty()) return null

                var name = "Liste Importée"
                var description = ""
                var sourceLang = "fr"
                var targetLang = "en"
                val parsedWords = mutableListOf<ParsedCsvWord>()

                for (record in records) {
                    if (record.isEmpty()) continue
                    val firstField = record[0].trim()

                    if (firstField.startsWith("Nom:", ignoreCase = true)) {
                        name = firstField.substringAfter(":").trim()
                    } else if (firstField.startsWith("Description:", ignoreCase = true)) {
                        description = firstField.substringAfter(":").trim()
                    } else if (firstField.startsWith("Langues:", ignoreCase = true)) {
                        val langPart = firstField.substringAfter(":").trim()
                        val parts = langPart.split("-", "_", "/")
                        if (parts.size >= 2) {
                            sourceLang = parts[0].trim().lowercase()
                            targetLang = parts[1].trim().lowercase()
                        }
                    } else if (firstField.startsWith("mot", ignoreCase = true) && record.size >= 2) {
                        // Ligne d'en-tête de colonnes, ignorer
                        continue
                    } else {
                        val wordText = unescapeCsvField(firstField).trim()
                        if (wordText.isNotBlank() &&
                            !wordText.startsWith("Nom:", ignoreCase = true) &&
                            !wordText.startsWith("Description:", ignoreCase = true) &&
                            !wordText.startsWith("Langues:", ignoreCase = true)
                        ) {
                            val translationText = unescapeCsvField(record.getOrElse(1) { "" }).trim()
                            val contextText = unescapeCsvField(record.getOrElse(2) { "" }).trim()

                            val aiExamplesText: String
                            val nbrRev: Int
                            val lastRevDate: Long
                            val transRec: Int
                            val transNotRec: Int
                            val timestampVal: Long

                            if (record.size >= 9) {
                                // Format SPEC-9 à 9 colonnes
                                aiExamplesText = unescapeCsvField(record[3]).trim()
                                nbrRev = record[4].trim().toIntOrNull() ?: 0
                                lastRevDate = record[5].trim().toLongOrNull() ?: 0L
                                transRec = record[6].trim().toIntOrNull() ?: 0
                                transNotRec = record[7].trim().toIntOrNull() ?: 0
                                timestampVal = record[8].trim().toLongOrNull() ?: System.currentTimeMillis()
                            } else if (record.size >= 5) {
                                // Format SPEC-8 à 5 colonnes
                                aiExamplesText = unescapeCsvField(record[3]).trim()
                                nbrRev = 0
                                lastRevDate = 0L
                                transRec = 0
                                transNotRec = 0
                                timestampVal = record[4].trim().toLongOrNull() ?: System.currentTimeMillis()
                            } else if (record.size == 4) {
                                // Ancien format à 4 colonnes
                                aiExamplesText = ""
                                nbrRev = 0
                                lastRevDate = 0L
                                transRec = 0
                                transNotRec = 0
                                timestampVal = record[3].trim().toLongOrNull() ?: System.currentTimeMillis()
                            } else {
                                aiExamplesText = ""
                                nbrRev = 0
                                lastRevDate = 0L
                                transRec = 0
                                transNotRec = 0
                                timestampVal = System.currentTimeMillis()
                            }

                            parsedWords.add(
                                ParsedCsvWord(
                                    word = wordText,
                                    translation = translationText,
                                    contextSentence = contextText,
                                    aiExamplesJson = aiExamplesText,
                                    nbrRevisions = nbrRev,
                                    lastRevisionDate = lastRevDate,
                                    translationRecognized = transRec,
                                    translationNotRecognized = transNotRec,
                                    timestamp = timestampVal
                                )
                            )
                        }
                    }
                }

                ParsedCsvData(
                    name = name,
                    description = description,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    words = parsedWords
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Génère un fichier modèle exemple (template) pour l'utilisateur.
     */
    fun generateTemplateCsv(context: Context, uri: Uri): Boolean {
        val sampleList = VocabularyListEntity(
            name = "Exemple de Liste",
            description = "Fichier modèle pour importation massive de vocabulaire",
            sourceLanguage = "en",
            targetLanguage = "fr"
        )
        val sampleWords = listOf(
            VocabularyEntity(
                word = "ephemeral",
                translation = "éphémère",
                contextSentence = "Flowers are so ephemeral!",
                aiExamplesJson = com.example.ai.AiTranslationManager.toJsonString(
                    listOf(
                        com.example.ai.UsageExample("Fame in the world is ephemeral.", "La renommée dans le monde est éphémère."),
                        com.example.ai.UsageExample("Spring blossoms are beautiful but ephemeral.", "Les fleurs de printemps sont belles mais éphémères.")
                    )
                )
            ),
            VocabularyEntity(
                word = "to tame",
                translation = "apprivoiser",
                contextSentence = "If you tame me, we shall need each other."
            ),
            VocabularyEntity(
                word = "yearning",
                translation = "désir ardent",
                contextSentence = "An aching yearning in his heart."
            )
        )
        return exportListToCsv(context, uri, sampleList, sampleWords)
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(";") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }

    private fun unescapeCsvField(field: String): String {
        var str = field.trim()
        if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
            str = str.substring(1, str.length - 1).replace("\"\"", "\"")
        }
        return str
    }

    /**
     * Parser CSV complet conforme RFC 4180.
     * Gère les enregistrements multi-lignes entre guillemets.
     */
    private fun parseCsvRecords(content: String, delimiter: Char = ';'): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = content.length

        while (i < n) {
            val c = content[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && content[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                if (c == '"') {
                    inQuotes = true
                } else if (c == delimiter) {
                    currentRecord.add(currentField.toString())
                    currentField.setLength(0)
                } else if (c == '\r') {
                    if (i + 1 < n && content[i + 1] == '\n') {
                        i++
                    }
                    currentRecord.add(currentField.toString())
                    currentField.setLength(0)
                    if (currentRecord.any { it.isNotBlank() }) {
                        records.add(ArrayList(currentRecord))
                    }
                    currentRecord.clear()
                } else if (c == '\n') {
                    currentRecord.add(currentField.toString())
                    currentField.setLength(0)
                    if (currentRecord.any { it.isNotBlank() }) {
                        records.add(ArrayList(currentRecord))
                    }
                    currentRecord.clear()
                } else {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            if (currentRecord.any { it.isNotBlank() }) {
                records.add(ArrayList(currentRecord))
            }
        }

        return records
    }
}
