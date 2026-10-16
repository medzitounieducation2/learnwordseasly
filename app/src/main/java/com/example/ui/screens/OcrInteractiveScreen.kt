package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr.DetectedWord
import com.example.ocr.OcrScanResult
import com.example.ui.components.InteractiveOcrCanvas
import com.example.ui.components.WordSaveDialog

/**
 * Écran 2 : Sélection du Mot & Extraction OCR (Interactive Preview & OCR Screen).
 * Permet à l'utilisateur de toucher n'importe quel mot sur la page de livre capturée
 * pour l'extraire précisément via les BoundingBoxes de ML Kit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrInteractiveScreen(
    bitmap: Bitmap?,
    ocrResult: OcrScanResult?,
    isLoading: Boolean,
    errorMessage: String?,
    selectedWord: DetectedWord?,
    isSaveDialogOpen: Boolean,
    showBoundingBoxes: Boolean,
    currentBookTitle: String,
    onTapCoordinates: (Float, Float) -> Unit,
    onSelectWordDirectly: (DetectedWord) -> Unit,
    onToggleBoundingBoxes: () -> Unit,
    onCloseSaveDialog: () -> Unit,
    onSaveWord: (word: String, definition: String, contextSentence: String, bookTitle: String) -> Unit,
    onSpeak: (String) -> Unit,
    onNavigateToCamera: () -> Unit,
    onLoadSampleBook: (Int) -> Unit
) {
    var showWordsListDrawer by remember { mutableStateOf(false) }

    if (bitmap == null) {
        // État vide : aucune photo capturée
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("ocr_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune page scannée",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prenez en photo une page de votre livre ou lancez une page d'exemple pour commencer à extraire des mots.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToCamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_camera_empty_button")
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ouvrir la caméra")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onLoadSampleBook(0) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_sample_empty_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tester avec Le Petit Prince")
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_interactive_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barre d'outils supérieure
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicateur Tap-to-select
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Touchez un mot",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val wordCount = ocrResult?.words?.size ?: 0
                            Text(
                                text = if (isLoading) "Analyse en cours..." else "$wordCount mots identifiés",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Boutons de contrôle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bouton liste de mots
                        FilterChip(
                            selected = showWordsListDrawer,
                            onClick = { showWordsListDrawer = !showWordsListDrawer },
                            label = { Text("Mots", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("toggle_words_list_chip")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Toggle cadres
                        IconButton(
                            onClick = onToggleBoundingBoxes,
                            modifier = Modifier.testTag("toggle_bounding_boxes_button")
                        ) {
                            Icon(
                                imageVector = if (showBoundingBoxes) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Afficher les cadres",
                                tint = if (showBoundingBoxes) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Zone interactive d'affichage et sélection tactile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                InteractiveOcrCanvas(
                    bitmap = bitmap,
                    detectedWords = ocrResult?.words ?: emptyList(),
                    selectedWord = selectedWord,
                    showBoundingBoxes = showBoundingBoxes,
                    onTapCoordinates = onTapCoordinates,
                    modifier = Modifier.fillMaxSize()
                )

                // Indicateur de chargement OCR
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Reconnaissance OCR en cours",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Détection locale via Google ML Kit...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Tiroir / Carrousel bas des mots détectés (pour navigation alternative)
                if (showWordsListDrawer && ocrResult != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mots détectés sur la page (${ocrResult.words.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showWordsListDrawer = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer la liste"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Liste défilable des mots détectés
                            val uniqueWords = remember(ocrResult) {
                                ocrResult.words
                                    .filter { it.cleanWord.length > 2 }
                                    .distinctBy { it.cleanWord.lowercase() }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    uniqueWords.forEach { item ->
                                        FilterChip(
                                            selected = selectedWord?.cleanWord == item.cleanWord,
                                            onClick = { onSelectWordDirectly(item) },
                                            label = { Text(item.cleanWord, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modale d'enregistrement du mot sélectionné
        if (isSaveDialogOpen && selectedWord != null) {
            WordSaveDialog(
                detectedWord = selectedWord,
                initialBookTitle = currentBookTitle,
                onDismiss = onCloseSaveDialog,
                onSpeak = onSpeak,
                onSave = onSaveWord
            )
        }
    }
}
