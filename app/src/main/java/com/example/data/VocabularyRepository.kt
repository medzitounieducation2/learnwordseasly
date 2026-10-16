package com.example.data

import kotlinx.coroutines.flow.Flow

class VocabularyRepository(private val dao: VocabularyDao) {
    val allWords: Flow<List<VocabularyEntity>> = dao.getAllWords()
    val favoriteWords: Flow<List<VocabularyEntity>> = dao.getFavoriteWords()
    val totalCount: Flow<Int> = dao.getWordCount()

    fun searchWords(query: String): Flow<List<VocabularyEntity>> = dao.searchWords(query)

    suspend fun getWordById(id: Long): VocabularyEntity? = dao.getWordById(id)

    suspend fun insert(entity: VocabularyEntity): Long = dao.insert(entity)

    suspend fun update(entity: VocabularyEntity) = dao.update(entity)

    suspend fun delete(entity: VocabularyEntity) = dao.delete(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun toggleFavorite(entity: VocabularyEntity) {
        dao.update(entity.copy(isFavorite = !entity.isFavorite))
    }
}
