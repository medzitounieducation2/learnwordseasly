package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VocabularyEntity
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

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VocabularyRepository(db.vocabularyDao())

        // Initialiser avec des exemples littéraires si la base est vide au premier lancement
        viewModelScope.launch(Dispatchers.IO) {
            val starterCount = db.vocabularyDao().getWordById(1)
            if (starterCount == null) {
                repository.insert(
                    VocabularyEntity(
                        word = "Éphémère",
                        definition = "Qui ne dure que très peu de temps, passager.",
                        contextSentence = "Les fleurs sont si éphémères !",
                        bookTitle = "Le Petit Prince",
                        isFavorite = true
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        word = "Apprivoiser",
                        definition = "Rendre plus traitable, créer des liens d'affection réciproque.",
                        contextSentence = "Si tu m'apprivoises, nous aurons besoin l'un de l'autre.",
                        bookTitle = "Le Petit Prince",
                        isFavorite = false
                    )
                )
                repository.insert(
                    VocabularyEntity(
                        word = "Ineffable",
                        definition = "Qu'on ne peut exprimer par des paroles en raison de sa beauté ou intensité.",
                        contextSentence = "Il contemplait dans cette ombre silencieuse l'ineffable clarté du devoir.",
                        bookTitle = "Les Misérables",
                        isFavorite = true
                    )
                )
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

    private val _currentBookTitle = MutableStateFlow("")
    val currentBookTitle: StateFlow<String> = _currentBookTitle.asStateFlow()

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
        filterType
    ) { words, query, filter ->
        var list = if (query.isBlank()) {
            words
        } else {
            val q = query.trim().lowercase()
            words.filter {
                it.word.lowercase().contains(q) ||
                it.definition.lowercase().contains(q) ||
                it.bookTitle.lowercase().contains(q) ||
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
     * Traite un nouveau Bitmap (capturé par caméra ou sélectionné dans la galerie).
     */
    fun processImage(bitmap: Bitmap, bookTitle: String = "") {
        _currentBitmap.value = bitmap
        _currentBookTitle.value = bookTitle
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
                processImage(bitmap, "${sample.title} (${sample.author})")
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
     * Enregistre le mot dans la base Room.
     */
    fun saveWord(
        word: String,
        definition: String,
        contextSentence: String,
        bookTitle: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = VocabularyEntity(
                word = word.trim(),
                definition = definition.trim(),
                contextSentence = contextSentence.trim(),
                bookTitle = bookTitle.trim(),
                timestamp = System.currentTimeMillis()
            )
            repository.insert(entity)
            withContext(Dispatchers.Main) {
                _isSaveDialogOpen.value = false
                _userMessage.value = "Mot « $word » enregistré dans votre vocabulaire !"
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
