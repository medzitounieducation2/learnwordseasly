package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant une liste ou un carnet de vocabulaire personnalisé
 * (ex: "Rome tome 1", "Ma voisine me plaît", "Le Petit Prince").
 */
@Entity(tableName = "vocabulary_lists")
data class VocabularyListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
