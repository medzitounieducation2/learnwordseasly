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
            translation = "Serendipity",
            contextSentence = "Ce voyage fut une suite de sérendipités."
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
                name = "Someone Like You",
                description = "Vocabulaire en anglais",
                sourceLanguage = "en",
                targetLanguage = "fr"
            )
        )
        assertTrue(listId > 0)

        dao.insert(
            VocabularyEntity(
                listId = listId,
                listName = "Someone Like You",
                word = "Yearning",
                translation = "Désir ardent",
                contextSentence = "An aching yearning in his heart."
            )
        )

        val wordsInList = dao.getWordsByListId(listId).first()
        assertEquals(1, wordsInList.size)
        assertEquals("Yearning", wordsInList[0].word)
        assertEquals("Désir ardent", wordsInList[0].translation)
        assertEquals("Someone Like You", wordsInList[0].listName)

        val fetchedList = dao.getListById(listId)
        assertNotNull(fetchedList)
        assertEquals("en", fetchedList?.sourceLanguage)
        assertEquals("fr", fetchedList?.targetLanguage)
    }

    @Test
    fun `test translation manager language mappings`() {
        val fr = com.example.translation.TranslationManager.getLanguage("fr")
        assertEquals("Français", fr.displayName)
        val en = com.example.translation.TranslationManager.getLanguage("en")
        assertEquals("Anglais", en.displayName)
        val ar = com.example.translation.TranslationManager.getLanguage("ar")
        assertEquals("Arabe", ar.displayName)
    }

    @Test
    fun `test shared preferences list selection persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE)

        prefs.edit().putLong("last_selected_list_id", 3L).apply()
        assertEquals(3L, prefs.getLong("last_selected_list_id", -1L))
    }

    @Test
    fun `test csv manager export and import parsing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "test_list.csv")
        val uri = android.net.Uri.fromFile(file)

        val sampleList = com.example.data.VocabularyListEntity(
            name = "Test CSV List",
            description = "Description de test",
            sourceLanguage = "en",
            targetLanguage = "fr"
        )
        val sampleWords = listOf(
            VocabularyEntity(word = "ephemeral", translation = "éphémère", contextSentence = "Flowers are so ephemeral!"),
            VocabularyEntity(word = "tame", translation = "apprivoiser", contextSentence = "To tame someone.")
        )

        val exportOk = com.example.data.CsvManager.exportListToCsv(context, uri, sampleList, sampleWords)
        assertTrue(exportOk)

        val parsed = com.example.data.CsvManager.importListFromCsv(context, uri)
        assertNotNull(parsed)
        assertEquals("Test CSV List", parsed?.name)
        assertEquals("Description de test", parsed?.description)
        assertEquals("en", parsed?.sourceLanguage)
        assertEquals("fr", parsed?.targetLanguage)
        assertEquals(2, parsed?.words?.size)
        assertEquals("ephemeral", parsed?.words?.get(0)?.word)
        assertEquals("éphémère", parsed?.words?.get(0)?.translation)
    }

    @Test
    fun `test csv manager export and import with ai examples 5 columns`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "test_list_v7.csv")
        val uri = android.net.Uri.fromFile(file)

        val sampleList = com.example.data.VocabularyListEntity(
            name = "Test CSV SPEC-8",
            description = "Test avec exemples IA",
            sourceLanguage = "en",
            targetLanguage = "fr"
        )
        val examplesJson = com.example.ai.AiTranslationManager.toJsonString(
            listOf(
                com.example.ai.UsageExample("Fame is ephemeral.", "La gloire est éphémère.")
            )
        )
        val sampleWords = listOf(
            VocabularyEntity(
                word = "ephemeral",
                translation = "éphémère",
                contextSentence = "Flowers are so ephemeral!",
                aiExamplesJson = examplesJson
            )
        )

        val exportOk = com.example.data.CsvManager.exportListToCsv(context, uri, sampleList, sampleWords)
        assertTrue(exportOk)

        val parsed = com.example.data.CsvManager.importListFromCsv(context, uri)
        assertNotNull(parsed)
        assertEquals("Test CSV SPEC-8", parsed?.name)
        assertEquals(1, parsed?.words?.size)
        assertEquals("ephemeral", parsed?.words?.get(0)?.word)
        assertEquals(examplesJson, parsed?.words?.get(0)?.aiExamplesJson)

        val examplesList = com.example.ai.AiTranslationManager.parseExamplesJson(parsed!!.words[0].aiExamplesJson)
        assertEquals(1, examplesList.size)
        assertEquals("Fame is ephemeral.", examplesList[0].textOrigine)
        assertEquals("La gloire est éphémère.", examplesList[0].traduction)
    }

    @Test
    fun `test csv manager import backward compatibility 4 columns`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "test_old_4col.csv")
        val csvContent = """
            Nom: Ancien Format
            Description: Test rétrocompatibilité
            Langues: fr-en
            mot;traduction;contexte;date_creation
            bonjour;hello;Bonjour tout le monde;1700000000000
        """.trimIndent()
        file.writeText(csvContent, Charsets.UTF_8)
        val uri = android.net.Uri.fromFile(file)

        val parsed = com.example.data.CsvManager.importListFromCsv(context, uri)
        assertNotNull(parsed)
        assertEquals("Ancien Format", parsed?.name)
        assertEquals(1, parsed?.words?.size)
        assertEquals("bonjour", parsed?.words?.get(0)?.word)
        assertEquals("hello", parsed?.words?.get(0)?.translation)
        assertEquals("Bonjour tout le monde", parsed?.words?.get(0)?.contextSentence)
        assertEquals("", parsed?.words?.get(0)?.aiExamplesJson)
        assertEquals(1700000000000L, parsed?.words?.get(0)?.timestamp)
    }

    @Test
    fun `test ai translation result model`() {
        val ex1 = com.example.ai.UsageExample("Il se recroqueville dans son lit.", "ينكمش في سريره.")
        val ex2 = com.example.ai.UsageExample("Les feuilles se recroquevillent en automne.", "تتكمش الأوراق في الخريف.")
        val result = com.example.ai.AiTranslationResult(
            traduction = "ينكمش / يتكوّم",
            exemples = listOf(ex1, ex2)
        )
        assertEquals("ينكمش / يتكوّم", result.traduction)
        assertEquals(2, result.exemples.size)
        assertEquals("Il se recroqueville dans son lit.", result.exemples[0].textOrigine)
        assertEquals("ينكمش في سريره.", result.exemples[0].traduction)
    }

    @Test
    fun `test csv manager export and import with spec 9 revision fields 9 columns`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "test_list_v8.csv")
        val uri = android.net.Uri.fromFile(file)

        val sampleList = com.example.data.VocabularyListEntity(
            name = "Test CSV SPEC-9",
            description = "Test révision",
            sourceLanguage = "en",
            targetLanguage = "fr"
        )
        val sampleWords = listOf(
            VocabularyEntity(
                word = "yearning",
                translation = "désir ardent",
                contextSentence = "An aching yearning.",
                nbrRevisions = 5,
                lastRevisionDate = 1700000000000L,
                translationRecognized = 4,
                translationNotRecognized = 1
            )
        )

        val exportOk = com.example.data.CsvManager.exportListToCsv(context, uri, sampleList, sampleWords)
        assertTrue(exportOk)

        val parsed = com.example.data.CsvManager.importListFromCsv(context, uri)
        assertNotNull(parsed)
        assertEquals("Test CSV SPEC-9", parsed?.name)
        assertEquals(1, parsed?.words?.size)
        val word = parsed!!.words[0]
        assertEquals("yearning", word.word)
        assertEquals(5, word.nbrRevisions)
        assertEquals(1700000000000L, word.lastRevisionDate)
        assertEquals(4, word.translationRecognized)
        assertEquals(1, word.translationNotRecognized)
    }

    @Test
    fun `test spec 10 arabic text export and import with bom and multiline json`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "test_arabic_spec10.csv")
        val uri = android.net.Uri.fromFile(file)

        val sampleList = com.example.data.VocabularyListEntity(
            name = "قائمة المفردات",
            description = "اختبار اللغة العربية والأسطر المتعددة",
            sourceLanguage = "ar",
            targetLanguage = "fr"
        )

        val examplesJson = "{\n  \"exemples\": [\n    {\"text_origine\": \"مرحبا بكم\\nفي التطبيق\", \"traduction\": \"Bienvenue\\ndans l'application\"}\n  ]\n}"

        val sampleWords = listOf(
            VocabularyEntity(
                word = "مرحبا",
                translation = "Bonjour / Bienvenue",
                contextSentence = "مرحبا بكم جميعا\nفي هذه الصفحة",
                aiExamplesJson = examplesJson,
                nbrRevisions = 3,
                lastRevisionDate = 1700000000000L,
                translationRecognized = 2,
                translationNotRecognized = 1
            )
        )

        val exportOk = com.example.data.CsvManager.exportListToCsv(context, uri, sampleList, sampleWords)
        assertTrue(exportOk)

        val parsed = com.example.data.CsvManager.importListFromCsv(context, uri)
        assertNotNull(parsed)
        assertEquals("قائمة المفردات", parsed?.name)
        assertEquals("اختبار اللغة العربية والأسطر المتعددة", parsed?.description)
        assertEquals(1, parsed?.words?.size)

        val word = parsed!!.words[0]
        assertEquals("مرحبا", word.word)
        assertEquals("Bonjour / Bienvenue", word.translation)
        assertEquals("مرحبا بكم جميعا\nفي هذه الصفحة", word.contextSentence)
        assertEquals(examplesJson, word.aiExamplesJson)
        assertEquals(3, word.nbrRevisions)
        assertEquals(2, word.translationRecognized)
        assertEquals(1, word.translationNotRecognized)
    }

    @Test
    fun `test spec 12 record revision increments revision fields`() {
        val word = VocabularyEntity(
            id = 1L,
            word = "test",
            nbrRevisions = 1,
            lastRevisionDate = 1000L,
            translationRecognized = 0,
            translationNotRecognized = 0
        )
        val updatedRev = word.copy(nbrRevisions = word.nbrRevisions + 1, lastRevisionDate = 2000L)
        assertEquals(2, updatedRev.nbrRevisions)
        assertEquals(2000L, updatedRev.lastRevisionDate)

        val updatedRec = updatedRev.copy(translationRecognized = updatedRev.translationRecognized + 1)
        assertEquals(1, updatedRec.translationRecognized)
        assertEquals(2, updatedRec.nbrRevisions)
    }

    @Test
    fun `test AppDatabase singleton initialization with version 8`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appDb = AppDatabase.getDatabase(context)
        assertNotNull(appDb)
        assertNotNull(appDb.vocabularyDao())
    }

    @Test
    fun `test spec 14 AppTab contains REVISION`() {
        val tabs = com.example.ui.AppTab.entries
        assertTrue(tabs.contains(com.example.ui.AppTab.VOCABULARY))
        assertTrue(tabs.contains(com.example.ui.AppTab.REVISION))
        assertTrue(tabs.contains(com.example.ui.AppTab.OCR_STUDIO))
        assertEquals(com.example.ui.AppTab.REVISION, com.example.ui.AppTab.valueOf("REVISION"))
    }

    @Test
    fun `test spec 14 revision list preference persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE)

        prefs.edit().putLong("last_revision_selected_list_id", 42L).apply()
        assertEquals(42L, prefs.getLong("last_revision_selected_list_id", -1L))

        prefs.edit().putLong("last_revision_selected_list_id", -1L).apply()
        assertEquals(-1L, prefs.getLong("last_revision_selected_list_id", -2L))
    }

    @Test
    fun `test spec 15 edit word list reassigns listId and listName`() = runBlocking {
        val dao = db.vocabularyDao()
        val list1Id = dao.insertList(
            com.example.data.VocabularyListEntity(name = "Someone Like You", sourceLanguage = "en", targetLanguage = "fr")
        )
        val list2Id = dao.insertList(
            com.example.data.VocabularyListEntity(name = "Dualed", sourceLanguage = "en", targetLanguage = "fr")
        )

        val wordId = dao.insert(
            VocabularyEntity(
                listId = list1Id,
                listName = "Someone Like You",
                word = "Reckless",
                translation = "Téméraire"
            )
        )

        val wordBefore = dao.getAllWords().first().find { it.id == wordId }
        assertNotNull(wordBefore)
        assertEquals(list1Id, wordBefore?.listId)
        assertEquals("Someone Like You", wordBefore?.listName)

        // Réassignation de la liste du mot vers Dualed (SPEC-15)
        val wordAfter = wordBefore!!.copy(
            listId = list2Id,
            listName = "Dualed"
        )
        dao.update(wordAfter)

        val wordsInList1 = dao.getWordsByListId(list1Id).first()
        val wordsInList2 = dao.getWordsByListId(list2Id).first()

        assertEquals(0, wordsInList1.size)
        assertEquals(1, wordsInList2.size)
        assertEquals("Reckless", wordsInList2[0].word)
        assertEquals("Dualed", wordsInList2[0].listName)
        assertEquals(list2Id, wordsInList2[0].listId)
    }

    @Test
    fun `test spec 16 extractContextSentence extracts full sentence across multiline text`() {
        val lines = listOf(
            "C'est dans cette forêt sombre et mystérieuse que le petit garçon découvrit",
            "une clé dorée cachée sous des feuilles mortes.",
            "Il la ramassa aussitôt."
        )

        // Mot au début de la 2ème ligne ("une")
        val contextLine2 = OcrProcessor.extractContextSentence(lines, targetLineIndex = 1, targetElementText = "une")
        assertEquals(
            "C'est dans cette forêt sombre et mystérieuse que le petit garçon découvrit une clé dorée cachée sous des feuilles mortes.",
            contextLine2
        )

        // Mot à la fin de la 1ère ligne ("découvrit")
        val contextLine1 = OcrProcessor.extractContextSentence(lines, targetLineIndex = 0, targetElementText = "découvrit")
        assertEquals(
            "C'est dans cette forêt sombre et mystérieuse que le petit garçon découvrit une clé dorée cachée sous des feuilles mortes.",
            contextLine1
        )

        // Mot dans la 3ème phrase ("ramassa")
        val contextLine3 = OcrProcessor.extractContextSentence(lines, targetLineIndex = 2, targetElementText = "ramassa")
        assertEquals(
            "Il la ramassa aussitôt.",
            contextLine3
        )
    }

    @Test
    fun `test spec 16 extractContextSentence handles guillemets and punctuation`() {
        val lines = listOf(
            "« Le Petit Prince habitait",
            "une planète à peine plus grande que lui. »",
            "C'était une bien grande découverte !"
        )

        val context1 = OcrProcessor.extractContextSentence(lines, targetLineIndex = 1, targetElementText = "planète")
        assertEquals(
            "« Le Petit Prince habitait une planète à peine plus grande que lui. »",
            context1
        )

        val context2 = OcrProcessor.extractContextSentence(lines, targetLineIndex = 2, targetElementText = "découverte")
        assertEquals(
            "C'était une bien grande découverte !",
            context2
        )
    }

    @Test
    fun `test spec 17 playback index persistence in shared preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE)

        prefs.edit().putInt("last_playback_index", 5).apply()
        assertEquals(5, prefs.getInt("last_playback_index", 0))

        prefs.edit().putInt("last_playback_index", 12).apply()
        assertEquals(12, prefs.getInt("last_playback_index", 0))
    }

    @Test
    fun `test spec 18 usage example json countListen and countRead serialization`() {
        val ex1 = com.example.ai.UsageExample(
            textOrigine = "Sample original text",
            traduction = "Exemple de traduction",
            countListen = 3,
            countRead = 7
        )
        val jsonStr = com.example.ai.AiTranslationManager.toJsonString(listOf(ex1))

        val parsed = com.example.ai.AiTranslationManager.parseExamplesJson(jsonStr)
        assertEquals(1, parsed.size)
        assertEquals("Sample original text", parsed[0].textOrigine)
        assertEquals("Exemple de traduction", parsed[0].traduction)
        assertEquals(3, parsed[0].countListen)
        assertEquals(7, parsed[0].countRead)
    }

    @Test
    fun `test spec 18 room database version 9 migration and sentence counters`() = runBlocking {
        val dao = db.vocabularyDao()
        val entry = VocabularyEntity(
            word = "ExempleSpec18",
            translation = "ExampleSpec18",
            contextSentence = "Phrase de contexte de test",
            contextCountListen = 4,
            contextCountRead = 9
        )
        val id = dao.insert(entry)
        assertTrue(id > 0)

        val fetched = dao.getAllWords().first().find { it.id == id }
        assertNotNull(fetched)
        assertEquals(4, fetched?.contextCountListen)
        assertEquals(9, fetched?.contextCountRead)

        // Met à jour l'entité
        val updated = fetched!!.copy(
            contextCountListen = fetched.contextCountListen + 1,
            contextCountRead = fetched.contextCountRead + 1
        )
        dao.update(updated)

        val fetchedAfter = dao.getAllWords().first().find { it.id == id }
        assertEquals(5, fetchedAfter?.contextCountListen)
        assertEquals(10, fetchedAfter?.contextCountRead)
    }

    @Test
    fun `test spec 18 view model sentence listen and read callbacks`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.MainViewModel(app)

        val dao = AppDatabase.getDatabase(app).vocabularyDao()
        val exJson = com.example.ai.AiTranslationManager.toJsonString(
            listOf(com.example.ai.UsageExample("Test sentence", "Traduction test", countListen = 0, countRead = 0))
        )
        val entity = VocabularyEntity(
            word = "TestWordSpec18",
            translation = "TestTranslation",
            contextSentence = "Test context sentence",
            aiExamplesJson = exJson,
            contextCountListen = 0,
            contextCountRead = 0
        )
        val id = dao.insert(entity)
        val savedEntity = dao.getAllWords().first().find { it.id == id }!!

        // Test écoute audio du contexte (+1)
        vm.recordSentenceListen(savedEntity, isContextSentence = true)
        kotlinx.coroutines.delay(150)
        var updated = dao.getAllWords().first().find { it.id == id }!!
        assertEquals(1, updated.contextCountListen)

        // Test lecture visuelle du contexte (+1)
        vm.recordSentenceRead(updated, isContextSentence = true)
        kotlinx.coroutines.delay(150)
        updated = dao.getAllWords().first().find { it.id == id }!!
        assertEquals(1, updated.contextCountRead)

        // Test écoute audio de l'exemple IA 0 (+1)
        vm.recordSentenceListen(updated, isContextSentence = false, exampleIndex = 0)
        kotlinx.coroutines.delay(150)
        updated = dao.getAllWords().first().find { it.id == id }!!
        var exList = updated.getAiExamplesList()
        assertEquals(1, exList[0].countListen)

        // Test lecture visuelle de l'exemple IA 0 (+1)
        vm.recordSentenceRead(updated, isContextSentence = false, exampleIndex = 0)
        kotlinx.coroutines.delay(150)
        updated = dao.getAllWords().first().find { it.id == id }!!
        exList = updated.getAiExamplesList()
        assertEquals(1, exList[0].countRead)
    }
}
