package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant une liste ou un carnet de vocabulaire personnalisé
 * (ex: "Rome tome 1", "Ma voisine me plaît", "Someone Like You").
 *
 * Chaque liste définit la paire linguistique source/cible (ex: fr -> en, en -> fr, fr -> ar).
 * Tous les mots enregistrés dans cette liste héritent strictement de cette paire.
 */
@Entity(tableName = "vocabulary_lists")
data class VocabularyListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val sourceLanguage: String = "fr",
    val targetLanguage: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)
