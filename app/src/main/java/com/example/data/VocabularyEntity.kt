package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant un mot de vocabulaire extrait lors de la lecture,
 * rattaché à une liste ou un ouvrage spécifique.
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long = 1L,
    val listName: String = "",
    val word: String,
    val definition: String = "",
    val contextSentence: String = "",
    val bookTitle: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
