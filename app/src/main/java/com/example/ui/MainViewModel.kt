package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VocabularyEntity
import com.example.data.VocabularyListEntity
import com.example.data.VocabularyRepository
import com.example.ocr.DetectedWord
import com.example.ocr.OcrProcessor
import com.example.ocr.OcrScanResult
import com.example.ocr.SampleBookPages
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
    SCANNER,
    OCR_STUDIO,
    VOCABULARY
}

enum class VocabFilter {
    ALL,
    FAVORITES,
    RECENT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VocabularyRepository
    val ttsManager = TtsManager(application)

    // Listes de vocabulaire
    val allLists: StateFlow<List<VocabularyListEntity>>

    // Filtrage par liste dans l'écran de vocabulaire (null = Toutes les listes)
    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

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

        // Initialiser avec des listes et exemples littéraires si la base est vide
        viewModelScope.launch(Dispatchers.IO) {
            val existingLists = repository.allLists.firstOrNull() ?: emptyList()
            if (existingLists.isEmpty()) {
                val list1Id = repository.insertList(
                    VocabularyListEntity(
                        name = "Le Petit Prince",
                        description = "Mots et citations poétiques du conte d'Antoine de Saint-Exupéry"
                    )
                )
                val list2Id = repository.insertList(
                    VocabularyListEntity(
                        name = "Les Misérables",
                        description = "Vocabulaire classique et philosophique de Victor Hugo"
                    )
                )
                val list3Id = repository.insertList(
                    VocabularyListEntity(
                        name = "Rome tome 1",
                        description = "Lecture historique et vocabulaire latin / antique"
                    )
                )

                _activeListId.value = list1Id

                repository.insert(
                    VocabularyEntity(
                        listId = list1Id,
                        listName = "Le Petit Prince",
                        word = "Éphémère",
                        definition = "Qui ne dure que très peu de temps, passager.",
                        contextSentence = "Les fleurs sont si éphémères !",
                        isFavorite = true
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        listId = list1Id,
                        listName = "Le Petit Prince",
                        word = "Apprivoiser",
                        definition = "Rendre plus traitable, créer des liens d'affection réciproque.",
                        contextSentence = "Si tu m'apprivoises, nous aurons besoin l'un de l'autre.",
                        isFavorite = false
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        listId = list2Id,
                        listName = "Les Misérables",
                        word = "Ineffable",
                        definition = "Qu'on ne peut exprimer par des paroles en raison de sa beauté ou intensité.",
                        contextSentence = "Il contemplait dans cette ombre silencieuse l'ineffable clarté du devoir.",
                        isFavorite = true
                    )
                )
            } else {
                _activeListId.value = existingLists.first().id
            }
        }
    }

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.SCANNER)
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
                it.definition.lowercase().contains(q) ||
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

    /**
     * Sélectionne ou désélectionne une liste pour filtrer l'affichage.
     */
    fun selectList(listId: Long?) {
        _selectedListId.value = listId
    }

    /**
     * Définit la liste par défaut pour les prochains mots enregistrés.
     */
    fun setActiveList(listId: Long?) {
        _activeListId.value = listId
    }

    /**
     * Crée une nouvelle liste de vocabulaire (ex: "Rome tome 1").
     */
    fun createList(name: String, description: String = "", onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isNotBlank()) {
                val newId = repository.insertList(
                    VocabularyListEntity(name = trimmedName, description = description.trim())
                )
                _activeListId.value = newId
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
            }
            if (_activeListId.value == list.id) {
                _activeListId.value = null
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
     * Enregistre le mot dans la base Room rattaché à une liste.
     */
    fun saveWord(
        word: String,
        definition: String,
        contextSentence: String,
        listId: Long? = null,
        listName: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetListId = listId ?: _activeListId.value ?: 1L
            _activeListId.value = targetListId

            val entity = VocabularyEntity(
                listId = targetListId,
                listName = listName,
                word = word.trim(),
                definition = definition.trim(),
                contextSentence = contextSentence.trim(),
                timestamp = System.currentTimeMillis()
            )
            repository.insert(entity)
            withContext(Dispatchers.Main) {
                _isSaveDialogOpen.value = false
                val destination = if (listName.isNotBlank()) " dans « $listName »" else ""
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

    fun deleteWord(entity: VocabularyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entity)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Mot « ${entity.word} » supprimé."
            }
        }
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
