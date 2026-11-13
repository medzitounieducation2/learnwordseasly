package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.AdvancedRevisionScreen
import com.example.ui.screens.OcrInteractiveScreen
import com.example.ui.screens.VocabularyListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VocabulaireLectureApp(viewModel)
            }
        }
    }
}

@Composable
fun VocabulaireLectureApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val capturedBitmap by viewModel.currentBitmap.collectAsStateWithLifecycle()
    val ocrResult by viewModel.ocrResult.collectAsStateWithLifecycle()
    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()
    val ocrError by viewModel.ocrError.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val isSaveDialogOpen by viewModel.isSaveDialogOpen.collectAsStateWithLifecycle()
    val showDetectedBoxes by viewModel.showDetectedBoxes.collectAsStateWithLifecycle()

    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val activeListId by viewModel.activeListId.collectAsStateWithLifecycle()

    val vocabularyWords by viewModel.vocabularyList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()

    val revisionWords by viewModel.revisionVocabularyList.collectAsStateWithLifecycle()
    val revisionSelectedListId by viewModel.revisionSelectedListId.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Gestion du retour physique Android : revenir à l'écran Vocabulaire par défaut
    BackHandler(enabled = currentTab != AppTab.VOCABULARY) {
        viewModel.setTab(AppTab.VOCABULARY)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("app_navigation_bar")) {
                NavigationBarItem(
                    selected = currentTab == AppTab.VOCABULARY,
                    onClick = { viewModel.setTab(AppTab.VOCABULARY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (vocabularyWords.isNotEmpty()) {
                                    Badge { Text("${vocabularyWords.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Vocabulaire"
                            )
                        }
                    },
                    label = { Text("Vocabulaire") },
                    modifier = Modifier.testTag("nav_tab_vocabulary")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.REVISION,
                    onClick = { viewModel.setTab(AppTab.REVISION) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (revisionWords.isNotEmpty()) {
                                    Badge { Text("${revisionWords.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Révision"
                            )
                        }
                    },
                    label = { Text("Révision") },
                    modifier = Modifier.testTag("nav_tab_revision")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.OCR_STUDIO,
                    onClick = { viewModel.setTab(AppTab.OCR_STUDIO) },
                    icon = {
                        BadgedBox(
                            badge = {
                                val count = ocrResult?.words?.size ?: 0
                                if (count > 0) {
                                    Badge { Text("$count") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "OCR Studio"
                            )
                        }
                    },
                    label = { Text("OCR Studio") },
                    modifier = Modifier.testTag("nav_tab_ocr")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.VOCABULARY -> {
                    VocabularyListScreen(
                        words = vocabularyWords,
                        lists = allLists,
                        selectedListId = selectedListId,
                        searchQuery = searchQuery,
                        filter = filterType,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onFilterChange = { viewModel.filterType.value = it },
                        onSelectList = { viewModel.selectList(it) },
                        onCreateList = { name, desc, src, tgt -> viewModel.createList(name, desc, src, tgt) },
                        onUpdateList = { viewModel.updateList(it) },
                        onDeleteList = { viewModel.deleteList(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onUpdateWord = { viewModel.updateWord(it) },
                        onDeleteWord = { viewModel.deleteWord(it) },
                        onSpeak = { viewModel.speak(it) },
                        onNavigateToScan = { viewModel.setTab(AppTab.OCR_STUDIO) },
                        onTranslate = { text, src, tgt -> viewModel.translateWord(text, src, tgt) },
                        onTranslateAi = { text, context, src, tgt -> viewModel.translateWithAiAndGetExamples(text, context, src, tgt) },
                        onExportCsv = { uri, listId -> viewModel.exportListToCsv(uri, listId) },
                        onImportCsv = { uri, targetListId -> viewModel.importListFromCsv(uri, targetListId) },
                        onDownloadTemplateCsv = { uri -> viewModel.downloadTemplateCsv(uri) },
                        onRevisionPerformed = { viewModel.recordRevision(it) },
                        onTranslationRecognized = { viewModel.recordTranslationRecognized(it) },
                        onTranslationNotRecognized = { viewModel.recordTranslationNotRecognized(it) }
                    )
                }

                AppTab.REVISION -> {
                    AdvancedRevisionScreen(
                        words = revisionWords,
                        lists = allLists,
                        selectedListId = revisionSelectedListId,
                        onSelectList = { viewModel.selectRevisionList(it) },
                        onSpeak = { text, langCode, onDone -> viewModel.speak(text, langCode, onDone) },
                        onStopSpeak = { viewModel.stopSpeak() },
                        savedPlaybackIndex = viewModel.getSavedPlaybackIndex(),
                        onSavePlaybackIndex = { viewModel.savePlaybackIndex(it) },
                        onRecordSentenceListen = { entity, isContextSentence, exampleIndex ->
                            viewModel.recordSentenceListen(entity, isContextSentence, exampleIndex)
                        },
                        onRecordSentenceRead = { entity, isContextSentence, exampleIndex ->
                            viewModel.recordSentenceRead(entity, isContextSentence, exampleIndex)
                        },
                        onRevisionPerformed = { viewModel.recordRevision(it) },
                        onTranslationRecognized = { viewModel.recordTranslationRecognized(it) },
                        onTranslationNotRecognized = { viewModel.recordTranslationNotRecognized(it) }
                    )
                }

                AppTab.OCR_STUDIO -> {
                    OcrInteractiveScreen(
                        bitmap = capturedBitmap,
                        ocrResult = ocrResult,
                        isLoading = isOcrLoading,
                        errorMessage = ocrError,
                        selectedWord = selectedWord,
                        isSaveDialogOpen = isSaveDialogOpen,
                        showBoundingBoxes = showDetectedBoxes,
                        availableLists = allLists,
                        activeListId = activeListId,
                        onImageCaptured = { bitmap ->
                            viewModel.processImage(bitmap)
                        },
                        onTapCoordinates = { x, y ->
                            viewModel.handleTapOnImage(x, y)
                        },
                        onSelectWordDirectly = { word ->
                            viewModel.selectWordDirectly(word)
                        },
                        onToggleBoundingBoxes = {
                            viewModel.toggleShowDetectedBoxes()
                        },
                        onCloseSaveDialog = {
                            viewModel.closeSaveDialog()
                        },
                        onCreateNewList = { name, desc, src, tgt, callback ->
                            viewModel.createList(name, desc, src, tgt, onCreated = callback)
                        },
                        onTranslate = { text, src, tgt ->
                            viewModel.translateWord(text, src, tgt)
                        },
                        onTranslateAi = { text, context, src, tgt ->
                            viewModel.translateWithAiAndGetExamples(text, context, src, tgt)
                        },
                        onSaveWord = { word, translation, context, aiExamplesJson, listId, listName ->
                            viewModel.saveWord(word, translation, context, aiExamplesJson, listId, listName)
                        },
                        onSpeak = { text ->
                            viewModel.speak(text)
                        },
                        onLoadSampleBook = { index ->
                            viewModel.loadSamplePage(index)
                        }
                    )
                }
            }
        }
    }
}
