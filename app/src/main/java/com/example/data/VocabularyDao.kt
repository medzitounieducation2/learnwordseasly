package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    // --- Mots de vocabulaire ---
    @Query("SELECT * FROM vocabulary ORDER BY timestamp DESC")
    fun getAllWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE listId = :listId ORDER BY timestamp DESC")
    fun getWordsByListId(listId: Long): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%' OR bookTitle LIKE '%' || :query || '%' OR listName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchWords(query: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Long): VocabularyEntity?

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary WHERE listId = :listId")
    fun getWordCountByListId(listId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VocabularyEntity): Long

    @Update
    suspend fun update(item: VocabularyEntity)

    @Delete
    suspend fun delete(item: VocabularyEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vocabulary WHERE listId = :listId")
    suspend fun deleteWordsByListId(listId: Long)

    // --- Listes de vocabulaire ---
    @Query("SELECT * FROM vocabulary_lists ORDER BY createdAt ASC")
    fun getAllLists(): Flow<List<VocabularyListEntity>>

    @Query("SELECT * FROM vocabulary_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: Long): VocabularyListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: VocabularyListEntity): Long

    @Update
    suspend fun updateList(list: VocabularyListEntity)

    @Delete
    suspend fun deleteList(list: VocabularyListEntity)

    @Query("DELETE FROM vocabulary_lists WHERE id = :id")
    suspend fun deleteListById(id: Long)
}
