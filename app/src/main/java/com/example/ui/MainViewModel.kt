package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CsvManager
import com.example.data.VocabularyEntity
import com.example.data.VocabularyListEntity
import com.example.data.VocabularyRepository
import com.example.ocr.DetectedWord
import com.example.ocr.OcrProcessor
import com.example.ocr.OcrScanResult
import com.example.ocr.SampleBookPages
import com.example.translation.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
    VOCABULARY,
    REVISION,
    OCR_STUDIO
}

enum class VocabFilter {
    ALL,
    FAVORITES,
    RECENT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VocabularyRepository
    val ttsManager = TtsManager(application)
    private val prefs = application.getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SELECTED_LIST_ID = "last_selected_list_id"
        private const val KEY_LAST_ACTIVE_LIST_ID = "last_active_list_id"
        private const val KEY_LAST_REVISION_LIST_ID = "last_revision_selected_list_id"
        private const val KEY_LAST_PLAYBACK_INDEX = "last_playback_index"
    }

    // Listes de vocabulaire
    val allLists: StateFlow<List<VocabularyListEntity>>

    // Filtrage par liste dans l'écran de vocabulaire (null = Toutes les listes)
    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    // Filtrage par liste dans l'écran de révision avancée (null = Toutes les listes)
    private val _revisionSelectedListId = MutableStateFlow<Long?>(null)
    val revisionSelectedListId: StateFlow<Long?> = _revisionSelectedListId.asStateFlow()

    // Liste active par défaut pour les futurs enregistrements
    private val _activeListId = MutableStateFlow<Long?>(null)
    val activeListId: StateFlow<Long?> = _activeListId.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VocabularyRepository(db.vocabularyDao())

        allLists = repository.allLists.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Initialiser ou restaurer la dernière liste sélectionnée
        viewModelScope.launch(Dispatchers.IO) {
            val existingLists = repository.allLists.firstOrNull() ?: emptyList()
            if (existingLists.isEmpty()) {
                val list1Id = repository.insertList(
                    VocabularyListEntity(
                        name = "Le Petit Prince",
                        description = "Mots et citations poétiques du conte d'Antoine de Saint-Exupéry",
                        sourceLanguage = "fr",
                        targetLanguage = "en"
                    )
                )
                val list2Id = repository.insertList(
                    VocabularyListEntity(
                        name = "Les Misérables",
                        description = "Vocabulaire classique et philosophique de Victor Hugo",
                        sourceLanguage = "fr",
                        targetLanguage = "en"
                    )
                )
                val list3Id = repository.insertList(
                    VocabularyListEntity(
                        name = "Someone Like You",
                        description = "Lecture littéraire en anglais (Roald Dahl)",
                        sourceLanguage = "en",
                        targetLanguage = "fr"
                    )
                )

                _activeListId.value = list1Id
                _selectedListId.value = list1Id
                _revisionSelectedListId.value = list1Id

                prefs.edit()
                    .putLong(KEY_LAST_SELECTED_LIST_ID, list1Id)
                    .putLong(KEY_LAST_ACTIVE_LIST_ID, list1Id)
                    .putLong(KEY_LAST_REVISION_LIST_ID, list1Id)
                    .apply()

                repository.insert(
                    VocabularyEntity(
                        listId = list1Id,
                        listName = "Le Petit Prince",
                        word = "Éphémère",
                        translation = "Ephemeral",
                        contextSentence = "Les fleurs sont si éphémères !",
                        isFavorite = true
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        listId = list1Id,
                        listName = "Le Petit Prince",
                        word = "Apprivoiser",
                        translation = "To tame",
                        contextSentence = "Si tu m'apprivoises, nous aurons besoin l'un de l'autre.",
                        isFavorite = false
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        listId = list2Id,
                        listName = "Les Misérables",
                        word = "Ineffable",
                        translation = "Ineffable",
                        contextSentence = "Il contemplait dans cette ombre silencieuse l'ineffable clarté du devoir.",
                        isFavorite = true
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        listId = list3Id,
                        listName = "Someone Like You",
                        word = "Yearning",
                        translation = "Désir ardent / nostalgie",
                        contextSentence = "He stared out the window with an aching yearning.",
                        isFavorite = false
                    )
                )
            } else {
                val savedSelectedId = prefs.getLong(KEY_LAST_SELECTED_LIST_ID, -2L)
                val savedActiveId = prefs.getLong(KEY_LAST_ACTIVE_LIST_ID, -1L)
                val savedRevisionSelectedId = prefs.getLong(KEY_LAST_REVISION_LIST_ID, -2L)

                val activeListExists = existingLists.any { it.id == savedActiveId }
                val selectedListExists = existingLists.any { it.id == savedSelectedId }
                val revisionListExists = existingLists.any { it.id == savedRevisionSelectedId }

                val activeIdToUse = if (activeListExists) savedActiveId else existingLists.first().id
                _activeListId.value = activeIdToUse

                val selectedIdToUse = when {
                    savedSelectedId == -1L -> null
                    selectedListExists -> savedSelectedId
                    else -> null
                }
                _selectedListId.value = selectedIdToUse

                val revisionIdToUse = when {
                    savedRevisionSelectedId == -1L -> null
                    revisionListExists -> savedRevisionSelectedId
                    else -> null
                }
                _revisionSelectedListId.value = revisionIdToUse
            }
        }
    }

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.VOCABULARY)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // État Image & OCR
    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    private val _ocrResult = MutableStateFlow<OcrScanResult?>(null)
    val ocrResult: StateFlow<OcrScanResult?> = _ocrResult.asStateFlow()

    private val _ocrError = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()

    private val _selectedWord = MutableStateFlow<DetectedWord?>(null)
    val selectedWord: StateFlow<DetectedWord?> = _selectedWord.asStateFlow()

    private val _isSaveDialogOpen = MutableStateFlow(false)
    val isSaveDialogOpen: StateFlow<Boolean> = _isSaveDialogOpen.asStateFlow()

    private val _showDetectedBoxes = MutableStateFlow(true)
    val showDetectedBoxes: StateFlow<Boolean> = _showDetectedBoxes.asStateFlow()

    // Notification toast / snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun toggleShowDetectedBoxes() {
        _showDetectedBoxes.value = !_showDetectedBoxes.value
    }

    // Filtrage et recherche vocabulaire
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow(VocabFilter.ALL)

    val vocabularyList: StateFlow<List<VocabularyEntity>> = combine(
        repository.allWords,
        searchQuery,
        filterType,
        _selectedListId
    ) { words, query, filter, listIdFilter ->
        var list = if (listIdFilter != null) {
            words.filter { it.listId == listIdFilter }
        } else {
            words
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.word.lowercase().contains(q) ||
                it.listName.lowercase().contains(q) ||
                it.contextSentence.lowercase().contains(q)
            }
        }

        when (filter) {
            VocabFilter.ALL -> list
            VocabFilter.FAVORITES -> list.filter { it.isFavorite }
            VocabFilter.RECENT -> {
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                list.filter { it.timestamp >= oneDayAgo }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisionVocabularyList: StateFlow<List<VocabularyEntity>> = combine(
        repository.allWords,
        _revisionSelectedListId
    ) { words, listIdFilter ->
        if (listIdFilter != null) {
            words.filter { it.listId == listIdFilter }
        } else {
            words
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Sélectionne ou désélectionne une liste pour la révision avancée et mémorise le choix.
     */
    fun selectRevisionList(listId: Long?) {
        _revisionSelectedListId.value = listId
        if (listId != null) {
            prefs.edit()
                .putLong(KEY_LAST_REVISION_LIST_ID, listId)
                .apply()
        } else {
            prefs.edit()
                .putLong(KEY_LAST_REVISION_LIST_ID, -1L)
                .apply()
        }
    }

    /**
     * Sélectionne ou désélectionne une liste pour filtrer l'affichage et mémorise le choix.
     */
    fun selectList(listId: Long?) {
        _selectedListId.value = listId
        if (listId != null) {
            _activeListId.value = listId
            prefs.edit()
                .putLong(KEY_LAST_SELECTED_LIST_ID, listId)
                .putLong(KEY_LAST_ACTIVE_LIST_ID, listId)
                .apply()
        } else {
            prefs.edit()
                .putLong(KEY_LAST_SELECTED_LIST_ID, -1L)
                .apply()
        }
    }

    /**
     * Définit la liste par défaut pour les prochains mots enregistrés.
     */
    fun setActiveList(listId: Long?) {
        _activeListId.value = listId
        if (listId != null) {
            prefs.edit()
                .putLong(KEY_LAST_ACTIVE_LIST_ID, listId)
                .apply()
        }
    }

    /**
     * Exporte une liste et ses mots dans un fichier CSV local.
     */
    fun exportListToCsv(uri: Uri, listId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getListById(listId) ?: return@launch
            val words = repository.getWordsByListId(listId).firstOrNull() ?: emptyList()
            val success = CsvManager.exportListToCsv(getApplication(), uri, list, words)
            withContext(Dispatchers.Main) {
                if (success) {
                    _userMessage.value = "Liste « ${list.name} » exportée en CSV !"
                } else {
                    _userMessage.value = "Échec de l'exportation."
                }
            }
        }
    }

    /**
     * Importe un fichier CSV pour créer une nouvelle liste ou mettre à jour une liste existante.
     */
    fun importListFromCsv(uri: Uri, targetListId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = CsvManager.importListFromCsv(getApplication(), uri)
            if (parsed == null) {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Échec de l'importation : fichier CSV invalide ou vide."
                }
                return@launch
            }

            if (targetListId != null) {
                val existingList = repository.getListById(targetListId)
                if (existingList != null) {
                    val existingWords = repository.getWordsByListId(targetListId).firstOrNull() ?: emptyList()
                    var addedCount = 0
                    var updatedCount = 0

                    parsed.words.forEach { csvWord ->
                        val existing = existingWords.find { it.word == csvWord.word }
                        if (existing != null) {
                            repository.update(
                                existing.copy(
                                    translation = if (csvWord.translation.isNotBlank()) csvWord.translation else existing.translation,
                                    contextSentence = if (csvWord.contextSentence.isNotBlank()) csvWord.contextSentence else existing.contextSentence,
                                    aiExamplesJson = if (csvWord.aiExamplesJson.isNotBlank()) csvWord.aiExamplesJson else existing.aiExamplesJson,
                                    nbrRevisions = if (csvWord.nbrRevisions > 0) csvWord.nbrRevisions else existing.nbrRevisions,
                                    lastRevisionDate = if (csvWord.lastRevisionDate > 0) csvWord.lastRevisionDate else existing.lastRevisionDate,
                                    translationRecognized = if (csvWord.translationRecognized > 0) csvWord.translationRecognized else existing.translationRecognized,
                                    translationNotRecognized = if (csvWord.translationNotRecognized > 0) csvWord.translationNotRecognized else existing.translationNotRecognized,
                                    timestamp = csvWord.timestamp
                                )
                            )
                            updatedCount++
                        } else {
                            repository.insert(
                                VocabularyEntity(
                                    listId = targetListId,
                                    listName = existingList.name,
                                    word = csvWord.word,
                                    translation = csvWord.translation,
                                    contextSentence = csvWord.contextSentence,
                                    aiExamplesJson = csvWord.aiExamplesJson,
                                    nbrRevisions = csvWord.nbrRevisions,
                                    lastRevisionDate = csvWord.lastRevisionDate,
                                    translationRecognized = csvWord.translationRecognized,
                                    translationNotRecognized = csvWord.translationNotRecognized,
                                    timestamp = csvWord.timestamp
                                )
                            )
                            addedCount++
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _userMessage.value = "Import réussi dans « ${existingList.name} » : $addedCount mots ajoutés, $updatedCount mis à jour !"
                    }
                }
            } else {
                val newListId = repository.insertList(
                    VocabularyListEntity(
                        name = parsed.name,
                        description = parsed.description,
                        sourceLanguage = parsed.sourceLanguage,
                        targetLanguage = parsed.targetLanguage
                    )
                )
                parsed.words.forEach { csvWord ->
                    repository.insert(
                        VocabularyEntity(
                            listId = newListId,
                            listName = parsed.name,
                            word = csvWord.word,
                            translation = csvWord.translation,
                            contextSentence = csvWord.contextSentence,
                            aiExamplesJson = csvWord.aiExamplesJson,
                            nbrRevisions = csvWord.nbrRevisions,
                            lastRevisionDate = csvWord.lastRevisionDate,
                            translationRecognized = csvWord.translationRecognized,
                            translationNotRecognized = csvWord.translationNotRecognized,
                            timestamp = csvWord.timestamp
                        )
                    )
                }

                _activeListId.value = newListId
                _selectedListId.value = newListId
                prefs.edit()
                    .putLong(KEY_LAST_SELECTED_LIST_ID, newListId)
                    .putLong(KEY_LAST_ACTIVE_LIST_ID, newListId)
                    .apply()

                withContext(Dispatchers.Main) {
                    _userMessage.value = "Nouvelle liste « ${parsed.name} » créée avec ${parsed.words.size} mots !"
                }
            }
        }
    }

    /**
     * Génère un fichier modèle CSV exemple pour l'utilisateur.
     */
    fun downloadTemplateCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = CsvManager.generateTemplateCsv(getApplication(), uri)
            withContext(Dispatchers.Main) {
                if (success) {
                    _userMessage.value = "Modèle CSV exemple téléchargé avec succès !"
                } else {
                    _userMessage.value = "Échec du téléchargement du modèle CSV."
                }
            }
        }
    }

    /**
     * Crée une nouvelle liste de vocabulaire avec sa paire de langues (ex: "Rome tome 1", "fr" -> "en").
     */
    fun createList(
        name: String,
        description: String = "",
        sourceLanguage: String = "fr",
        targetLanguage: String = "en",
        onCreated: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isNotBlank()) {
                val newId = repository.insertList(
                    VocabularyListEntity(
                        name = trimmedName,
                        description = description.trim(),
                        sourceLanguage = sourceLanguage.trim().lowercase(),
                        targetLanguage = targetLanguage.trim().lowercase()
                    )
                )
                _activeListId.value = newId
                _selectedListId.value = newId
                prefs.edit()
                    .putLong(KEY_LAST_SELECTED_LIST_ID, newId)
                    .putLong(KEY_LAST_ACTIVE_LIST_ID, newId)
                    .apply()

                withContext(Dispatchers.Main) {
                    _userMessage.value = "Nouvelle liste « $trimmedName » créée !"
                    onCreated?.invoke(newId)
                }
            }
        }
    }

    /**
     * Met à jour une liste existante (nom / description).
     */
    fun updateList(list: VocabularyListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateList(list)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Liste « ${list.name} » mise à jour."
            }
        }
    }

    /**
     * Supprime une liste et ses mots associés.
     */
    fun deleteList(list: VocabularyListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteList(list)
            if (_selectedListId.value == list.id) {
                _selectedListId.value = null
                prefs.edit().putLong(KEY_LAST_SELECTED_LIST_ID, -1L).apply()
            }
            if (_revisionSelectedListId.value == list.id) {
                _revisionSelectedListId.value = null
                prefs.edit().putLong(KEY_LAST_REVISION_LIST_ID, -1L).apply()
            }
            if (_activeListId.value == list.id) {
                _activeListId.value = null
                prefs.edit().remove(KEY_LAST_ACTIVE_LIST_ID).apply()
            }
            withContext(Dispatchers.Main) {
                _userMessage.value = "Liste « ${list.name} » et ses mots supprimés."
            }
        }
    }

    /**
     * Traite un nouveau Bitmap (capturé par caméra ou sélectionné dans la galerie).
     */
    fun processImage(bitmap: Bitmap) {
        _currentBitmap.value = bitmap
        _selectedWord.value = null
        _ocrError.value = null
        _isOcrLoading.value = true
        _currentTab.value = AppTab.OCR_STUDIO

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    OcrProcessor.processImage(bitmap)
                }
                _ocrResult.value = result
                _userMessage.value = "${result.words.size} mots détectés. Touchez un mot pour l'extraire !"
            } catch (e: Exception) {
                _ocrError.value = "Erreur OCR : ${e.localizedMessage ?: "Échec de reconnaissance"}"
            } finally {
                _isOcrLoading.value = false
            }
        }
    }

    /**
     * Charge une page de livre d'exemple pour tester immédiatement.
     */
    fun loadSamplePage(index: Int = 0) {
        val sample = SampleBookPages.availableSamples.getOrElse(index) { SampleBookPages.availableSamples[0] }
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = SampleBookPages.renderPageToBitmap(sample)
            withContext(Dispatchers.Main) {
                processImage(bitmap)
            }
        }
    }

    /**
     * Interaction Tap-to-select : coordonnées (imageX, imageY) sur le Bitmap réel.
     */
    fun handleTapOnImage(imageX: Float, imageY: Float) {
        val words = _ocrResult.value?.words ?: return
        val found = OcrProcessor.findWordAtCoordinates(imageX, imageY, words)
        if (found != null) {
            _selectedWord.value = found
            _isSaveDialogOpen.value = true
            ttsManager.speak(found.cleanWord)
        } else {
            _userMessage.value = "Aucun mot détecté à cet endroit. Essayez de toucher un mot."
        }
    }

    fun selectWordDirectly(word: DetectedWord) {
        _selectedWord.value = word
        _isSaveDialogOpen.value = true
        ttsManager.speak(word.cleanWord)
    }

    fun closeSaveDialog() {
        _isSaveDialogOpen.value = false
    }

    /**
     * Traduit un mot d'une langue source vers une langue cible via Google ML Kit Translate on-device.
     */
    suspend fun translateWord(text: String, sourceLang: String, targetLang: String): Result<String> {
        return TranslationManager.translate(text, sourceLang, targetLang)
    }

    /**
     * Traduit un mot et récupère des exemples d'utilisation via l'IA ChatGPT (gpt-4o-mini).
     */
    suspend fun translateWithAiAndGetExamples(
        text: String,
        contextSentence: String = "",
        sourceLang: String,
        targetLang: String
    ): Result<com.example.ai.AiTranslationResult> {
        return com.example.ai.AiTranslationManager.translateAndGetExamples(
            word = text,
            contextSentence = contextSentence,
            sourceLangCode = sourceLang,
            targetLangCode = targetLang
        )
    }

    /**
     * Enregistre le mot dans la base Room rattaché à une liste.
     */
    fun saveWord(
        word: String,
        translation: String = "",
        contextSentence: String = "",
        aiExamplesJson: String = "",
        listId: Long? = null,
        listName: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetListId = listId ?: _activeListId.value ?: 1L
            _activeListId.value = targetListId
            prefs.edit().putLong(KEY_LAST_ACTIVE_LIST_ID, targetListId).apply()

            val resolvedListName = if (listName.isNotBlank()) {
                listName
            } else {
                repository.getListById(targetListId)?.name ?: ""
            }

            val entity = VocabularyEntity(
                listId = targetListId,
                listName = resolvedListName,
                word = word.trim(),
                translation = translation.trim(),
                contextSentence = contextSentence.trim(),
                aiExamplesJson = aiExamplesJson.trim(),
                timestamp = System.currentTimeMillis()
            )
            repository.insert(entity)
            withContext(Dispatchers.Main) {
                _isSaveDialogOpen.value = false
                val destination = if (resolvedListName.isNotBlank()) " dans « $resolvedListName »" else ""
                _userMessage.value = "Mot « $word » enregistré$destination !"
            }
        }
    }

    fun toggleFavorite(entity: VocabularyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(entity)
        }
    }

    fun updateWord(entity: VocabularyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(entity)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Mot « ${entity.word} » mis à jour."
            }
        }
    }

    /**
     * Enregistre une révision pour un mot (incrémente nbrRevisions et met à jour lastRevisionDate).
     */
    fun recordRevision(entity: VocabularyEntity): VocabularyEntity {
        val updated = entity.copy(
            nbrRevisions = entity.nbrRevisions + 1,
            lastRevisionDate = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(updated)
        }
        return updated
    }

    /**
     * Enregistre le succès de reconnaissance de la traduction (bouton vert).
     */
    fun recordTranslationRecognized(entity: VocabularyEntity): VocabularyEntity {
        val updated = entity.copy(
            translationRecognized = entity.translationRecognized + 1
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(updated)
        }
        return updated
    }

    /**
     * Enregistre l'échec de reconnaissance de la traduction (bouton rouge).
     */
    fun recordTranslationNotRecognized(entity: VocabularyEntity): VocabularyEntity {
        val updated = entity.copy(
            translationNotRecognized = entity.translationNotRecognized + 1
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(updated)
        }
        return updated
    }

    fun deleteWord(entity: VocabularyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entity)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Mot « ${entity.word} » supprimé."
            }
        }
    }

    fun speak(
        text: String,
        languageCode: String = "fr",
        onDone: (() -> Unit)? = null
    ) {
        ttsManager.speak(text = text, languageCode = languageCode, onDone = onDone)
    }

    fun stopSpeak() {
        ttsManager.stop()
    }

    fun getSavedPlaybackIndex(): Int {
        return prefs.getInt(KEY_LAST_PLAYBACK_INDEX, 0)
    }

    fun savePlaybackIndex(index: Int) {
        prefs.edit().putInt(KEY_LAST_PLAYBACK_INDEX, index).apply()
    }

    /**
     * Incrémente le compteur d'écoute audio (countListen) d'une phrase (contexte ou exemple IA).
     */
    fun recordSentenceListen(entity: VocabularyEntity, isContextSentence: Boolean, exampleIndex: Int = -1) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = if (isContextSentence) {
                entity.copy(contextCountListen = entity.contextCountListen + 1)
            } else {
                val examples = entity.getAiExamplesList().toMutableList()
                if (exampleIndex in examples.indices) {
                    val ex = examples[exampleIndex]
                    examples[exampleIndex] = ex.copy(countListen = ex.countListen + 1)
                    val newJson = com.example.ai.AiTranslationManager.toJsonString(examples)
                    entity.copy(aiExamplesJson = newJson)
                } else {
                    entity
                }
            }
            repository.update(updated)
        }
    }

    /**
     * Incrémente le compteur de lecture visuelle (countRead) d'une phrase (contexte ou exemple IA).
     */
    fun recordSentenceRead(entity: VocabularyEntity, isContextSentence: Boolean, exampleIndex: Int = -1) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = if (isContextSentence) {
                entity.copy(contextCountRead = entity.contextCountRead + 1)
            } else {
                val examples = entity.getAiExamplesList().toMutableList()
                if (exampleIndex in examples.indices) {
                    val ex = examples[exampleIndex]
                    examples[exampleIndex] = ex.copy(countRead = ex.countRead + 1)
                    val newJson = com.example.ai.AiTranslationManager.toJsonString(examples)
                    entity.copy(aiExamplesJson = newJson)
                } else {
                    entity
                }
            }
            repository.update(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
