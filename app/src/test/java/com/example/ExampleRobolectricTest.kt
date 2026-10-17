package com.example

import android.content.Context
import android.graphics.Rect
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.VocabularyEntity
import com.example.ocr.DetectedWord
import com.example.ocr.OcrProcessor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vocabulaire Lecture", appName)
    }

    @Test
    fun `test sanitizeWord removes punctuation and elisions`() {
        assertEquals("éphémère", OcrProcessor.sanitizeWord("l'éphémère,"))
        assertEquals("horizon", OcrProcessor.sanitizeWord("«d'horizon...»"))
        assertEquals("apprivoiser", OcrProcessor.sanitizeWord("apprivoiser!"))
    }

    @Test
    fun `test coordinate mapping finds word inside bounding box`() {
        val word1 = DetectedWord(
            rawText = "éphémère",
            cleanWord = "éphémère",
            boundingBox = Rect(100, 200, 250, 260),
            lineText = "Les fleurs sont si éphémères !"
        )
        val word2 = DetectedWord(
            rawText = "renard",
            cleanWord = "renard",
            boundingBox = Rect(300, 200, 420, 260),
            lineText = "dit le renard"
        )
        val list = listOf(word1, word2)

        // Exact match
        val found = OcrProcessor.findWordAtCoordinates(150f, 230f, list)
        assertNotNull(found)
        assertEquals("éphémère", found?.cleanWord)

        // Near match with touch tolerance
        val nearFound = OcrProcessor.findWordAtCoordinates(305f, 265f, list, tolerancePixels = 20f)
        assertNotNull(nearFound)
        assertEquals("renard", nearFound?.cleanWord)
    }

    @Test
    fun `test room database insert and flow query`() = runBlocking {
        val dao = db.vocabularyDao()
        val entry = VocabularyEntity(
            word = "Sérendipité",
            definition = "Découverte heureuse faite par hasard et sagacité.",
            contextSentence = "Ce voyage fut une suite de sérendipités.",
            bookTitle = "Essais Littéraires"
        )
        val id = dao.insert(entry)
        assertTrue(id > 0)

        val words = dao.getAllWords().first()
        assertEquals(1, words.size)
        assertEquals("Sérendipité", words[0].word)
    }

    @Test
    fun `test custom vocabulary lists grouping`() = runBlocking {
        val dao = db.vocabularyDao()
        val listId = dao.insertList(
            com.example.data.VocabularyListEntity(
                name = "Rome tome 1",
                description = "Vocabulaire du premier tome"
            )
        )
        assertTrue(listId > 0)

        dao.insert(
            VocabularyEntity(
                listId = listId,
                listName = "Rome tome 1",
                word = "Centurion",
                definition = "Officier commandant une centurie.",
                contextSentence = "Le centurion leva son glaive.",
                bookTitle = "Rome tome 1"
            )
        )

        val wordsInList = dao.getWordsByListId(listId).first()
        assertEquals(1, wordsInList.size)
        assertEquals("Centurion", wordsInList[0].word)
        assertEquals("Rome tome 1", wordsInList[0].listName)
    }
}
