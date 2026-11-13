package com.example.data

import kotlinx.coroutines.flow.Flow

class VocabularyRepository(private val dao: VocabularyDao) {
    // Mots
    val allWords: Flow<List<VocabularyEntity>> = dao.getAllWords()
    val favoriteWords: Flow<List<VocabularyEntity>> = dao.getFavoriteWords()
    val totalCount: Flow<Int> = dao.getWordCount()

    fun getWordsByListId(listId: Long): Flow<List<VocabularyEntity>> = dao.getWordsByListId(listId)

    fun searchWords(query: String): Flow<List<VocabularyEntity>> = dao.searchWords(query)

    suspend fun getWordById(id: Long): VocabularyEntity? = dao.getWordById(id)

    suspend fun insert(entity: VocabularyEntity): Long = dao.insert(entity)

    suspend fun update(entity: VocabularyEntity) = dao.update(entity)

    suspend fun delete(entity: VocabularyEntity) = dao.delete(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun toggleFavorite(entity: VocabularyEntity) {
        dao.update(entity.copy(isFavorite = !entity.isFavorite))
    }

    // Listes
    val allLists: Flow<List<VocabularyListEntity>> = dao.getAllLists()

    suspend fun getListById(id: Long): VocabularyListEntity? = dao.getListById(id)

    suspend fun insertList(list: VocabularyListEntity): Long = dao.insertList(list)

    suspend fun updateList(list: VocabularyListEntity) = dao.updateList(list)

    suspend fun deleteList(list: VocabularyListEntity) {
        dao.deleteWordsByListId(list.id)
        dao.deleteList(list)
    }
}
