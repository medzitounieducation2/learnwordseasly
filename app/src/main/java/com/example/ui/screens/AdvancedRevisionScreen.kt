package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VocabularyEntity
import com.example.data.VocabularyListEntity
import com.example.ui.VocabFilter
import kotlinx.coroutines.launch

/**
 * Représente un élément d'expression/contexte pour le mode "Exemples uniquement".
 */
data class RevisionContextItem(
    val id: String,
    val word: String,
    val wordTranslation: String,
    val sentence: String,
    val sentenceTranslation: String,
    val listName: String,
    val isContextSentence: Boolean,
    val exampleIndex: Int,
    val countListen: Int,
    val countRead: Int,
    val entity: VocabularyEntity
)

/**
 * SPEC-20 : Bugfix Audio - Lecture en Boucle ("Play All") & Écoute Unitaire des Phrases.
 */
@Composable
fun AdvancedRevisionScreen(
    words: List<VocabularyEntity>,
    lists: List<VocabularyListEntity>,
    selectedListId: Long?,
    onSelectList: (Long?) -> Unit,
    onSpeak: (text: String, langCode: String, onDone: (() -> Unit)?) -> Unit,
    onStopSpeak: () -> Unit = {},
    savedPlaybackIndex: Int = 0,
    onSavePlaybackIndex: (Int) -> Unit = {},
    onRecordSentenceListen: (entity: VocabularyEntity, isContextSentence: Boolean, exampleIndex: Int) -> Unit = { _, _, _ -> },
    onRecordSentenceRead: (entity: VocabularyEntity, isContextSentence: Boolean, exampleIndex: Int) -> Unit = { _, _, _ -> },
    onRevisionPerformed: ((VocabularyEntity) -> VocabularyEntity)? = null,
    onTranslationRecognized: ((VocabularyEntity) -> VocabularyEntity)? = null,
    onTranslationNotRecognized: ((VocabularyEntity) -> VocabularyEntity)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var showFlashcards by remember { mutableStateOf(false) }

    // Options de révision
    var examplesOnlyMode by remember { mutableStateOf(true) }
    var showTranslations by remember { mutableStateOf(false) }
    var fillInBlankMode by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(VocabFilter.ALL) }

    // État du lecteur Audio
    var playbackIndex by remember(savedPlaybackIndex) { mutableIntStateOf(savedPlaybackIndex) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    // Suivi des devinettes révélées manuellement par l'utilisateur (id -> Boolean)
    val revealedItems = remember { mutableStateMapOf<String, Boolean>() }

    val currentSelectedList = lists.find { it.id == selectedListId }

    val orderedLists = remember(lists, selectedListId) {
        if (selectedListId == null) {
            lists
        } else {
            val selected = lists.find { it.id == selectedListId }
            if (selected != null) {
                listOf(selected) + lists.filter { it.id != selectedListId }
            } else {
                lists
            }
        }
    }

    // Filtrage des mots selon le filtre actif (Tous / Favoris / Récent)
    val filteredWords = remember(words, filterMode) {
        when (filterMode) {
            VocabFilter.ALL -> words
            VocabFilter.FAVORITES -> words.filter { it.isFavorite }
            VocabFilter.RECENT -> {
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                words.filter { it.timestamp >= oneDayAgo }
            }
        }
    }

    // Extraction des contextes & exemples pour le mode "Exemples uniquement"
    val contextItems = remember(filteredWords) {
        val list = mutableListOf<RevisionContextItem>()
        filteredWords.forEach { entity ->
            var count = 0
            if (entity.contextSentence.isNotBlank()) {
                list.add(
                    RevisionContextItem(
                        id = "ctx_${entity.id}",
                        word = entity.word,
                        wordTranslation = entity.translation,
                        sentence = entity.contextSentence,
                        sentenceTranslation = "",
                        listName = entity.listName,
                        isContextSentence = true,
                        exampleIndex = -1,
                        countListen = entity.contextCountListen,
                        countRead = entity.contextCountRead,
                        entity = entity
                    )
                )
                count++
            }
            val aiList = entity.getAiExamplesList()
            aiList.forEachIndexed { index, ex ->
                if (ex.textOrigine.isNotBlank()) {
                    list.add(
                        RevisionContextItem(
                            id = "ai_${entity.id}_$index",
                            word = entity.word,
                            wordTranslation = entity.translation,
                            sentence = ex.textOrigine,
                            sentenceTranslation = ex.traduction,
                            listName = entity.listName,
                            isContextSentence = false,
                            exampleIndex = index,
                            countListen = ex.countListen,
                            countRead = ex.countRead,
                            entity = entity
                        )
                    )
                    count++
                }
            }
            // Si le mot n'a aucun exemple ni contexte, on ajoute une entrée par défaut avec le mot
            if (count == 0) {
                list.add(
                    RevisionContextItem(
                        id = "word_${entity.id}",
                        word = entity.word,
                        wordTranslation = entity.translation,
                        sentence = entity.word,
                        sentenceTranslation = entity.translation,
                        listName = entity.listName,
                        isContextSentence = true,
                        exampleIndex = -1,
                        countListen = entity.contextCountListen,
                        countRead = entity.contextCountRead,
                        entity = entity
                    )
                )
            }
        }
        list
    }

    // Moteur de lecture audio (séquentiel en boucle si autoAdvance == true, ou unitaire si autoAdvance == false)
    fun playIndex(targetIndex: Int, autoAdvance: Boolean = isAutoPlaying) {
        val totalCount = if (examplesOnlyMode) contextItems.size else filteredWords.size
        if (totalCount == 0) return

        val validIndex = (targetIndex % totalCount + totalCount) % totalCount
        playbackIndex = validIndex
        onSavePlaybackIndex(validIndex)
        isSpeaking = true

        coroutineScope.launch {
            lazyListState.animateScrollToItem(validIndex)
        }

        val textToSpeak: String
        val langCode: String

        if (examplesOnlyMode) {
            val item = contextItems.getOrNull(validIndex) ?: run {
                isSpeaking = false
                return
            }
            textToSpeak = item.sentence
            val listEntity = lists.find { it.name == item.listName }
            langCode = listEntity?.sourceLanguage ?: "fr"

            onSpeak(textToSpeak, langCode) {
                onRecordSentenceListen(item.entity, item.isContextSentence, item.exampleIndex)
                if (isAutoPlaying && autoAdvance) {
                    val nextIdx = (validIndex + 1) % totalCount // En boucle infinie !
                    playIndex(nextIdx, autoAdvance = true)
                } else {
                    isSpeaking = false
                    isAutoPlaying = false
                    isPaused = false
                }
            }
        } else {
            val item = filteredWords.getOrNull(validIndex) ?: run {
                isSpeaking = false
                return
            }
            textToSpeak = if (item.contextSentence.isNotBlank()) item.contextSentence else item.word
            val listEntity = lists.find { it.id == item.listId }
            langCode = listEntity?.sourceLanguage ?: "fr"

            onSpeak(textToSpeak, langCode) {
                onRecordSentenceListen(item, true, -1)
                if (isAutoPlaying && autoAdvance) {
                    val nextIdx = (validIndex + 1) % totalCount // En boucle infinie !
                    playIndex(nextIdx, autoAdvance = true)
                } else {
                    isSpeaking = false
                    isAutoPlaying = false
                    isPaused = false
                }
            }
        }
    }

    fun stopAudio() {
        onStopSpeak()
        isSpeaking = false
        isAutoPlaying = false
        isPaused = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("advanced_revision_screen")
    ) {
        // En-tête supérieur compact
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Ligne 1 : Titre + Bouton Flashcards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Révision Avancée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (examplesOnlyMode) "${contextItems.size} phrases" else "${filteredWords.size} mots",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bouton Lancer Flashcards
                    if (filteredWords.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                stopAudio()
                                showFlashcards = true
                            },
                            modifier = Modifier.testTag("revision_flashcards_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flashcards", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Ligne 2 : Sélecteur horizontal des listes (Toutes / Liste X)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = selectedListId == null,
                                onClick = {
                                    stopAudio()
                                    onSelectList(null)
                                },
                                label = { Text("Toutes les listes", fontSize = 11.sp) },
                                modifier = Modifier.testTag("revision_list_chip_all")
                            )
                        }

                        items(orderedLists, key = { it.id }) { list ->
                            FilterChip(
                                selected = selectedListId == list.id,
                                onClick = {
                                    stopAudio()
                                    onSelectList(list.id)
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(list.name, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${list.sourceLanguage.uppercase()}→${list.targetLanguage.uppercase()}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedListId == list.id)
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            else
                                                MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("revision_list_chip_${list.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Ligne 3 : Contrôles & Options de Révision (Mode Exemples seuls, Traduction, Devinette, Filtres)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle Exemples Uniquement
                        item {
                            ElevatedFilterChip(
                                selected = examplesOnlyMode,
                                onClick = {
                                    stopAudio()
                                    examplesOnlyMode = !examplesOnlyMode
                                },
                                label = {
                                    Text(
                                        text = if (examplesOnlyMode) "Exemples seuls" else "Mots & Exemples",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("toggle_examples_only")
                            )
                        }

                        // Toggle Afficher Traduction
                        item {
                            FilterChip(
                                selected = showTranslations,
                                onClick = { showTranslations = !showTranslations },
                                label = {
                                    Text(
                                        text = if (showTranslations) "Traduction visible" else "Traduction masquée",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("toggle_show_translations")
                            )
                        }

                        // Toggle Devinette (Mot masqué)
                        item {
                            FilterChip(
                                selected = fillInBlankMode,
                                onClick = { fillInBlankMode = !fillInBlankMode },
                                label = {
                                    Text(
                                        text = if (fillInBlankMode) "Devinette ON" else "Devinette OFF",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (fillInBlankMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("toggle_fill_in_blank")
                            )
                        }

                        // Filtres rapides (Tous / Favoris)
                        item {
                            FilterChip(
                                selected = filterMode == VocabFilter.FAVORITES,
                                onClick = {
                                    stopAudio()
                                    filterMode = if (filterMode == VocabFilter.FAVORITES) VocabFilter.ALL else VocabFilter.FAVORITES
                                },
                                label = { Text("★ Favoris", fontSize = 11.sp) },
                                modifier = Modifier.testTag("revision_filter_favorites")
                            )
                        }
                    }
                }
            }
        }

        // Barre de Contrôle Audio Player ("Play All")
        val totalCount = if (examplesOnlyMode) contextItems.size else filteredWords.size
        if (totalCount > 0) {
            val isPlayingOrPaused = isAutoPlaying || isPaused
            val barContainerColor = when {
                isAutoPlaying -> MaterialTheme.colorScheme.primaryContainer
                isPaused -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val barBorder = when {
                isAutoPlaying -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                isPaused -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("audio_player_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = barContainerColor),
                border = barBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Statut & progression
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isAutoPlaying -> Icons.Default.GraphicEq
                                isPaused -> Icons.Default.PauseCircle
                                else -> Icons.Default.PlayCircle
                            },
                            contentDescription = null,
                            tint = when {
                                isAutoPlaying -> MaterialTheme.colorScheme.primary
                                isPaused -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val currentNum = (playbackIndex + 1).coerceAtMost(totalCount)
                            val titleText = when {
                                isAutoPlaying -> "Lecture $currentNum / $totalCount (En boucle)"
                                isPaused -> "Pause ($currentNum / $totalCount)"
                                else -> "Play All ($totalCount phrases)"
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isPlayingOrPaused) {
                                val activeWord = if (examplesOnlyMode)
                                    contextItems.getOrNull(playbackIndex)?.word
                                else
                                    filteredWords.getOrNull(playbackIndex)?.word

                                if (!activeWord.isNullOrBlank()) {
                                    Text(
                                        text = "Mot : $activeWord",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Text(
                                    text = "Écouter la liste en boucle",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Boutons de contrôle audio
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bouton Précédent (en boucle)
                        IconButton(
                            onClick = {
                                stopAudio()
                                val prevIdx = (playbackIndex - 1 + totalCount) % totalCount
                                isAutoPlaying = true
                                playIndex(prevIdx, autoAdvance = true)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Précédent",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Bouton Play / Pause (Play All)
                        IconButton(
                            onClick = {
                                if (isAutoPlaying) {
                                    onStopSpeak()
                                    isSpeaking = false
                                    isAutoPlaying = false
                                    isPaused = true
                                } else {
                                    isAutoPlaying = true
                                    isPaused = false
                                    playIndex(playbackIndex, autoAdvance = true)
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isAutoPlaying) "Pause" else "Lecture en boucle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Bouton Stop
                        IconButton(
                            onClick = {
                                stopAudio()
                                playbackIndex = 0
                                onSavePlaybackIndex(0)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Arrêter",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Bouton Suivant (en boucle)
                        IconButton(
                            onClick = {
                                stopAudio()
                                val nextIdx = (playbackIndex + 1) % totalCount
                                isAutoPlaying = true
                                playIndex(nextIdx, autoAdvance = true)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Suivant",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Bouton Replay (réécouter la phrase courante)
                        IconButton(
                            onClick = {
                                onStopSpeak()
                                playIndex(playbackIndex, autoAdvance = isAutoPlaying)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Recommencer la phrase",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Zone de contenu principal
        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (currentSelectedList != null)
                            "Aucun mot à réviser dans « ${currentSelectedList.name} »"
                        else
                            "Aucun mot dans votre vocabulaire",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ajoutez des mots depuis l'onglet Vocabulaire ou OCR Studio !",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (examplesOnlyMode) {
            // Mode "Exemples & Contextes Uniquement"
            if (contextItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune phrase d'exemple disponible pour ce filtre.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(contextItems, key = { _, item -> item.id }) { index, item ->
                        val isRevealed = revealedItems[item.id] ?: false
                        val isPlayingThis = isSpeaking && index == playbackIndex
                        ContextSentenceCard(
                            item = item,
                            showListBadge = selectedListId == null,
                            showTranslation = showTranslations,
                            isMasked = fillInBlankMode,
                            isRevealed = isRevealed,
                            isCurrentlyPlaying = isPlayingThis,
                            onToggleReveal = {
                                revealedItems[item.id] = !isRevealed
                            },
                            onSpeakSentence = {
                                stopAudio()
                                playIndex(index, autoAdvance = false)
                            },
                            onRecordRead = {
                                onRecordSentenceRead(item.entity, item.isContextSentence, item.exampleIndex)
                            }
                        )
                    }
                }
            }
        } else {
            // Mode standard "Mots & Exemples"
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredWords, key = { _, item -> item.id }) { index, item ->
                    val itemKey = "word_full_${item.id}"
                    val isRevealed = revealedItems[itemKey] ?: false
                    val isPlayingThis = isSpeaking && index == playbackIndex
                    WordRevisionCard(
                        entity = item,
                        showListBadge = selectedListId == null,
                        showTranslation = showTranslations,
                        isMasked = fillInBlankMode,
                        isRevealed = isRevealed,
                        isCurrentlyPlaying = isPlayingThis,
                        onToggleReveal = {
                            revealedItems[itemKey] = !isRevealed
                        },
                        onSpeakWord = {
                            stopAudio()
                            playIndex(index, autoAdvance = false)
                        },
                        onRecordRead = { isCtx, exIdx ->
                            onRecordSentenceRead(item, isCtx, exIdx)
                        }
                    )
                }
            }
        }
    }

    // Dialogue Flashcards si activé
    if (showFlashcards) {
        FlashcardsDialog(
            words = filteredWords,
            lists = lists,
            onDismiss = { showFlashcards = false },
            onSpeak = { text -> onSpeak(text, "fr", null) },
            onRevisionPerformed = onRevisionPerformed,
            onTranslationRecognized = onTranslationRecognized,
            onTranslationNotRecognized = onTranslationNotRecognized
        )
    }
}

/**
 * Carte d'affichage d'une phrase de contexte / expression d'exemple.
 */
@Composable
private fun ContextSentenceCard(
    item: RevisionContextItem,
    showListBadge: Boolean,
    showTranslation: Boolean,
    isMasked: Boolean,
    isRevealed: Boolean,
    isCurrentlyPlaying: Boolean,
    onToggleReveal: () -> Unit,
    onSpeakSentence: () -> Unit,
    onRecordRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("context_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentlyPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyPlaying) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Ligne supérieure : Badges & Compteurs (1. 🎧 Lancer Audio & Compteur, 2. 👁️ Compteur Lecture +1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentlyPlaying) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "En lecture...",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (showListBadge && item.listName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = item.listName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Groupe de 2 boutons interactifs (1. 🎧 Lancer Audio & Compteur, 2. 👁️ Compteur Lecture +1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 1. Bouton Écouter Audio + Compteur 🎧 (Clic = Lancer l'audio)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurrentlyPlaying)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { onSpeakSentence() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Default.VolumeUp else Icons.Default.Headphones,
                                contentDescription = "Lancer la lecture audio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.countListen}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 2. Bouton & Compteur Lecture Visuelle 👁️ (+1 au clic)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onRecordRead() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Incrémenter la lecture visuelle",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.countRead}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "+",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Phrase de Contexte / Exemple Principal avec le mot cible mis en évidence en gras (ou masqué)
            val primaryColor = MaterialTheme.colorScheme.primary
            val annotatedText = remember(item.sentence, item.word, isMasked, isRevealed, primaryColor) {
                buildHighlightedAnnotatedString(
                    fullText = item.sentence,
                    targetWord = item.word,
                    isMasked = isMasked,
                    isRevealed = isRevealed,
                    highlightColor = primaryColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isCurrentlyPlaying)
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = isMasked) { onToggleReveal() }
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isMasked && !isRevealed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Touchez pour révéler le mot masqué",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Traduction de la phrase ou du mot (si l'option est activée)
            if (showTranslation || isRevealed) {
                val translationText = when {
                    item.sentenceTranslation.isNotBlank() -> "→ ${item.sentenceTranslation}"
                    item.wordTranslation.isNotBlank() -> "→ ${item.wordTranslation}"
                    else -> ""
                }
                if (translationText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = translationText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * Carte de révision complète pour le mode "Mots & Exemples".
 */
@Composable
private fun WordRevisionCard(
    entity: VocabularyEntity,
    showListBadge: Boolean,
    showTranslation: Boolean,
    isMasked: Boolean,
    isRevealed: Boolean,
    isCurrentlyPlaying: Boolean,
    onToggleReveal: () -> Unit,
    onSpeakWord: () -> Unit,
    onRecordRead: (isContextSentence: Boolean, exampleIndex: Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_revision_card_${entity.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentlyPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyPlaying) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Ligne supérieure : Mot, Prononciation, Badge Liste
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayWord = if (isMasked && !isRevealed) "[ ... ]" else entity.word
                    Text(
                        text = displayWord,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(enabled = isMasked) { onToggleReveal() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSpeakWord,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Écouter",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (showListBadge && entity.listName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = entity.listName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Traduction du mot
            if (showTranslation || isRevealed) {
                if (entity.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entity.translation,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Contexte extrait
            if (entity.contextSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val primaryColor = MaterialTheme.colorScheme.primary
                val annotatedContext = remember(entity.contextSentence, entity.word, isMasked, isRevealed, primaryColor) {
                    buildHighlightedAnnotatedString(
                        fullText = entity.contextSentence,
                        targetWord = entity.word,
                        isMasked = isMasked,
                        isRevealed = isRevealed,
                        highlightColor = primaryColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = annotatedContext,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Indicateurs & Boutons interactifs (🎧 Audio, 👁️ Read +1) pour le contexte
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { onSpeakWord() }
                            ) {
                                Text(
                                    text = "🎧 ${entity.contextCountListen}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.clickable { onRecordRead(true, -1) }
                            ) {
                                Text(
                                    text = "👁️ ${entity.contextCountRead} +",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Exemples IA
            val aiExamples = remember(entity.aiExamplesJson) { entity.getAiExamplesList() }
            if (aiExamples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Exemples IA :",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        aiExamples.forEachIndexed { index, ex ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (ex.textOrigine.isNotBlank()) {
                                        Text(
                                            text = "« ${ex.textOrigine} »",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if ((showTranslation || isRevealed) && ex.traduction.isNotBlank()) {
                                        Text(
                                            text = "→ ${ex.traduction}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable { onSpeakWord() }
                                    ) {
                                        Text(
                                            text = "🎧 ${ex.countListen}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { onRecordRead(false, index) }
                                    ) {
                                        Text(
                                            text = "👁️ ${ex.countRead} +",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Utilitaire pour construire un [AnnotatedString] mettant en gras et en couleur le mot cible dans une phrase,
 * ou en le masquant par [ ... ] si le mode devinette est actif.
 */
private fun buildHighlightedAnnotatedString(
    fullText: String,
    targetWord: String,
    isMasked: Boolean,
    isRevealed: Boolean,
    highlightColor: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    if (fullText.isEmpty() || targetWord.isEmpty()) {
        builder.append(fullText)
        return builder.toAnnotatedString()
    }

    val pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(targetWord), java.util.regex.Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(fullText)

    var lastIndex = 0
    val replacement = if (isMasked && !isRevealed) "[ ... ]" else null

    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        builder.append(fullText.substring(lastIndex, start))
        if (replacement != null) {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor))
            builder.append(replacement)
            builder.pop()
        } else {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor))
            builder.append(fullText.substring(start, end))
            builder.pop()
        }
        lastIndex = end
    }
    if (lastIndex < fullText.length) {
        builder.append(fullText.substring(lastIndex))
    }
    return builder.toAnnotatedString()
}
