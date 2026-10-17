package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant un mot de vocabulaire extrait lors de la lecture,
 * rattaché à une liste spécifique (ex: "Rome tome 1", "Someone Like You").
 *
 * Le mot hérite de la langue d'origine et de la langue cible de sa liste.
 * Le champ [translation] stocke la traduction obtenue dans la langue de destination.
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long = 1L,
    val listName: String = "",
    val word: String,
    val translation: String = "",
    val definition: String = "",
    val contextSentence: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
