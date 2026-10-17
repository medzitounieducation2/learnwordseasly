package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MenuBook
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
import com.example.ui.screens.CameraCaptureScreen
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
    val currentBookTitle by viewModel.currentBookTitle.collectAsStateWithLifecycle()

    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val activeListId by viewModel.activeListId.collectAsStateWithLifecycle()

    val vocabularyWords by viewModel.vocabularyList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Gestion du retour physique Android
    BackHandler(enabled = currentTab != AppTab.SCANNER) {
        viewModel.setTab(AppTab.SCANNER)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("app_navigation_bar")) {
                NavigationBarItem(
                    selected = currentTab == AppTab.SCANNER,
                    onClick = { viewModel.setTab(AppTab.SCANNER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scanner"
                        )
                    },
                    label = { Text("Scanner") },
                    modifier = Modifier.testTag("nav_tab_scanner")
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
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.SCANNER -> {
                    CameraCaptureScreen(
                        onImageCaptured = { bitmap ->
                            viewModel.processImage(bitmap, "Page photographiée")
                        },
                        onLoadSampleBook = { index ->
                            viewModel.loadSamplePage(index)
                        }
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
                        currentBookTitle = currentBookTitle,
                        availableLists = allLists,
                        activeListId = activeListId,
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
                        onCreateNewList = { name, callback ->
                            viewModel.createList(name, onCreated = callback)
                        },
                        onSaveWord = { word, def, context, book, listId, listName ->
                            viewModel.saveWord(word, def, context, book, listId, listName)
                        },
                        onSpeak = { text ->
                            viewModel.speak(text)
                        },
                        onNavigateToCamera = {
                            viewModel.setTab(AppTab.SCANNER)
                        },
                        onLoadSampleBook = { index ->
                            viewModel.loadSamplePage(index)
                        }
                    )
                }

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
                        onCreateList = { name, desc -> viewModel.createList(name, desc) },
                        onUpdateList = { viewModel.updateList(it) },
                        onDeleteList = { viewModel.deleteList(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onUpdateWord = { viewModel.updateWord(it) },
                        onDeleteWord = { viewModel.deleteWord(it) },
                        onSpeak = { viewModel.speak(it) },
                        onNavigateToScan = { viewModel.setTab(AppTab.SCANNER) }
                    )
                }
            }
        }
    }
}
