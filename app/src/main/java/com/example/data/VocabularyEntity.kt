package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ai.AiTranslationManager
import com.example.ai.UsageExample

/**
 * Entité représentant un mot de vocabulaire extrait lors de la lecture,
 * rattaché à une liste spécifique (ex: "Rome tome 1", "Someone Like You").
 *
 * Le mot hérite de la langue d'origine et de la langue cible de sa liste.
 * Contient également la traduction, la phrase de contexte et les exemples IA enregistrés.
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long = 1L,
    val listName: String = "",
    val word: String,
    val translation: String = "",
    val contextSentence: String = "",
    val aiExamplesJson: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val nbrRevisions: Int = 0,
    val lastRevisionDate: Long = 0L,
    val translationRecognized: Int = 0,
    val translationNotRecognized: Int = 0,
    val contextCountListen: Int = 0,
    val contextCountRead: Int = 0
) {
    /**
     * Helper pour extraire la liste d'objets [UsageExample] à partir du JSON.
     */
    fun getAiExamplesList(): List<UsageExample> {
        return AiTranslationManager.parseExamplesJson(aiExamplesJson)
    }
}
