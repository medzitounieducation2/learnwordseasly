package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.VocabularyEntity
import com.example.data.VocabularyListEntity
import com.example.ui.components.LanguagePairBadge

/**
 * Modale de Révision par Cartes Mémo (Flashcards).
 * Face A : Mot + Audio + Contexte de lecture.
 * Face B : Traduction + Auto-évaluation (Reconnu / Non-reconnu).
 */
@Composable
fun FlashcardsDialog(
    words: List<VocabularyEntity>,
    lists: List<VocabularyListEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onRevisionPerformed: ((VocabularyEntity) -> VocabularyEntity)? = null,
    onTranslationRecognized: ((VocabularyEntity) -> VocabularyEntity)? = null,
    onTranslationNotRecognized: ((VocabularyEntity) -> VocabularyEntity)? = null
) {
    if (words.isEmpty()) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    var cardList by remember(words) { mutableStateOf(words) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val revisedWordIdsInSession = remember { mutableSetOf<Long>() }
    val evaluatedWordIdsInSession = remember { mutableSetOf<Long>() }

    val safeIndex = currentIndex.coerceIn(0, cardList.size - 1)
    val currentWord = cardList[safeIndex]
    val associatedList = lists.find { it.id == currentWord.listId }

    val hasEvaluatedCurrentCard = evaluatedWordIdsInSession.contains(currentWord.id)

    // Animation de retournement de carte
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "flashcard_rotation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("flashcards_dialog")
                .fillMaxWidth(0.92f)
                .height(520.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Révision (${currentIndex + 1}/${cardList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        // Bouton Mélanger
                        IconButton(
                            onClick = {
                                cardList = cardList.shuffled()
                                currentIndex = 0
                                isFlipped = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Mélanger les cartes",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                // Carte réversible (Face A : Mot / Face B : Traduction & Auto-évaluation)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 14f * density
                        }
                        .clickable {
                            val willBeFlipped = !isFlipped
                            isFlipped = willBeFlipped
                            if (willBeFlipped && !revisedWordIdsInSession.contains(currentWord.id)) {
                                revisedWordIdsInSession.add(currentWord.id)
                                val updated = onRevisionPerformed?.invoke(currentWord)
                                if (updated != null) {
                                    val newList = cardList.toMutableList()
                                    newList[safeIndex] = updated
                                    cardList = newList
                                }
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // Face avant : Mot & Prononciation
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = currentWord.word,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                IconButton(
                                    onClick = { onSpeak(currentWord.word) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Écouter",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (currentWord.contextSentence.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "« ${currentWord.contextSentence} »",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Toucher pour révéler la traduction",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            // Face arrière : Traduction mise en avant & Auto-évaluation
                            Column(
                                modifier = Modifier
                                    .graphicsLayer { rotationY = 180f }
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Affichage de la traduction
                                if (currentWord.translation.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "TRADUCTION",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentWord.translation,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                } else {
                                    Text(
                                        text = "Aucune traduction enregistrée.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Boutons d'auto-évaluation Reconnu / Non-reconnu (Vert / Rouge sans libellé, désactivés après clic)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bouton Vert (Reconnu)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasEvaluatedCurrentCard) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFF4CAF50),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable(enabled = !hasEvaluatedCurrentCard) {
                                                evaluatedWordIdsInSession.add(currentWord.id)
                                                val updated = onTranslationRecognized?.invoke(currentWord)
                                                if (updated != null) {
                                                    val newList = cardList.toMutableList()
                                                    newList[safeIndex] = updated
                                                    cardList = newList
                                                }
                                            }
                                            .testTag("translation_recognized_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Reconnu",
                                                tint = if (hasEvaluatedCurrentCard) Color.White.copy(alpha = 0.5f) else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Bouton Rouge (Non reconnu)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasEvaluatedCurrentCard) Color(0xFFE53935).copy(alpha = 0.35f) else Color(0xFFE53935),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable(enabled = !hasEvaluatedCurrentCard) {
                                                evaluatedWordIdsInSession.add(currentWord.id)
                                                val updated = onTranslationNotRecognized?.invoke(currentWord)
                                                if (updated != null) {
                                                    val newList = cardList.toMutableList()
                                                    newList[safeIndex] = updated
                                                    cardList = newList
                                                }
                                            }
                                            .testTag("translation_not_recognized_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Non reconnu",
                                                tint = if (hasEvaluatedCurrentCard) Color.White.copy(alpha = 0.5f) else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                // Affichage des statistiques discrètes de révision sous les boutons
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✓ ${currentWord.translationRecognized}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "✗ ${currentWord.translationNotRecognized}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        text = "• Révisions : ${currentWord.nbrRevisions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Informations sur la liste
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (currentWord.listName.isNotBlank()) {
                                        Text(
                                            text = currentWord.listName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (associatedList != null) {
                                        LanguagePairBadge(
                                            sourceLangCode = associatedList.sourceLanguage,
                                            targetLangCode = associatedList.targetLanguage
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Commandes de navigation entre cartes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                isFlipped = false
                            }
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.testTag("prev_flashcard_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Carte précédente"
                        )
                    }

                    Text(
                        text = "${currentIndex + 1} / ${cardList.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            if (currentIndex < cardList.size - 1) {
                                currentIndex++
                                isFlipped = false
                            }
                        },
                        enabled = currentIndex < cardList.size - 1,
                        modifier = Modifier.testTag("next_flashcard_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Carte suivante"
                        )
                    }
                }
            }
        }
    }
}
