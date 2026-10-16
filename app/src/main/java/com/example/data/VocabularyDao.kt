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
    @Query("SELECT * FROM vocabulary ORDER BY timestamp DESC")
    fun getAllWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%' OR bookTitle LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchWords(query: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Long): VocabularyEntity?

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getWordCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VocabularyEntity): Long

    @Update
    suspend fun update(item: VocabularyEntity)

    @Delete
    suspend fun delete(item: VocabularyEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteById(id: Long)
}
