package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiTranslationResult
import com.example.ai.UsageExample
import com.example.data.VocabularyListEntity
import com.example.ocr.DetectedWord
import com.example.translation.TranslationManager
import kotlinx.coroutines.launch

@Composable
fun WordSaveDialog(
    detectedWord: DetectedWord,
    availableLists: List<VocabularyListEntity>,
    initialSelectedListId: Long?,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onTranslate: suspend (text: String, sourceLang: String, targetLang: String) -> Result<String>,
    onTranslateAi: (suspend (text: String, context: String, sourceLang: String, targetLang: String) -> Result<AiTranslationResult>)? = null,
    onCreateNewList: (name: String, description: String, sourceLang: String, targetLang: String, (Long) -> Unit) -> Unit,
    onSave: (word: String, translation: String, contextSentence: String, aiExamplesJson: String, listId: Long, listName: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val contextFocusRequester = remember { FocusRequester() }

    var word by remember(detectedWord) { mutableStateOf(detectedWord.cleanWord) }
    var translation by remember(detectedWord) { mutableStateOf("") }
    var contextSentence by remember(detectedWord) { mutableStateOf(detectedWord.lineText) }

    // État Traduction ML Kit & IA
    var isTranslating by remember { mutableStateOf(false) }
    var isAiTranslating by remember { mutableStateOf(false) }
    var translationErrorMessage by remember { mutableStateOf<String?>(null) }
    var aiExamples by remember { mutableStateOf<List<UsageExample>>(emptyList()) }

    // Sélection de la liste
    var selectedListId by remember(availableLists, initialSelectedListId) {
        val initial = availableLists.find { it.id == initialSelectedListId }?.id
            ?: availableLists.firstOrNull()?.id
            ?: 1L
        mutableStateOf(initial)
    }

    var isListDropdownExpanded by remember { mutableStateOf(false) }

    // Dialogue création de liste
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListNameInput by remember { mutableStateOf("") }
    var newListDescInput by remember { mutableStateOf("") }
    var newListSourceLang by remember { mutableStateOf("fr") }
    var newListTargetLang by remember { mutableStateOf("en") }

    // Édition inline du contexte
    var isEditingContext by remember { mutableStateOf(false) }
    var hasGainedContextFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isEditingContext) {
        if (isEditingContext) {
            hasGainedContextFocus = false
            contextFocusRequester.requestFocus()
        }
    }

    val selectedList = availableLists.find { it.id == selectedListId } ?: availableLists.firstOrNull()
    val sourceLangCode = selectedList?.sourceLanguage ?: "fr"
    val targetLangCode = selectedList?.targetLanguage ?: "en"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("word_save_dialog_surface")
                .fillMaxWidth(0.92f)
                .heightIn(max = 600.dp)
                .padding(vertical = 4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // En-tête de la modale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mot extrait",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fiche du mot principal avec prononciation
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MOT DÉTECTÉ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = word,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = { onSpeak(word) },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Écouter la prononciation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sélecteur de liste + Affichage de la paire de langues de la liste
                Text(
                    text = "Ajouter à la liste :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            onClick = { isListDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedList?.name ?: "Sélectionner une liste",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isListDropdownExpanded,
                            onDismissRequest = { isListDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            availableLists.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontWeight = if (item.id == selectedListId) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            LanguagePairBadge(
                                                sourceLangCode = item.sourceLanguage,
                                                targetLangCode = item.targetLanguage
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedListId = item.id
                                        isListDropdownExpanded = false
                                        translationErrorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = {
                            newListNameInput = ""
                            newListDescInput = ""
                            newListSourceLang = "fr"
                            newListTargetLang = "en"
                            showCreateListDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_new_list_button_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nouvelle liste",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Liste", fontSize = 12.sp)
                    }
                }

                // Badge de langues de la liste
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Langues de la liste : ",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguagePairBadge(
                        sourceLangCode = sourceLangCode,
                        targetLangCode = targetLangCode
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Champ Traduction avec bouton "Traduire" ML Kit
                val targetLangInfo = TranslationManager.getLanguage(targetLangCode)

                Text(
                    text = "Traduction (${targetLangInfo.flag} ${targetLangInfo.displayName}) :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = translation,
                        onValueChange = {
                            translation = it
                            translationErrorMessage = null
                        },
                        placeholder = { Text("Ex: sens en ${targetLangInfo.displayName}...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("word_translation_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Bouton Traduire Google ML Kit
                    Button(
                        onClick = {
                            if (word.isNotBlank()) {
                                isTranslating = true
                                translationErrorMessage = null
                                coroutineScope.launch {
                                    val res = onTranslate(word, sourceLangCode, targetLangCode)
                                    res.onSuccess { translatedText ->
                                        translation = translatedText
                                        translationErrorMessage = null
                                    }.onFailure { ex ->
                                        translationErrorMessage = ex.message ?: "Échec de la traduction"
                                    }
                                    isTranslating = false
                                }
                            }
                        },
                        enabled = word.isNotBlank() && !isTranslating && !isAiTranslating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("translate_word_button")
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Traduire",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Traduire", fontSize = 12.sp)
                        }
                    }
                }

                // Message d'erreur de traduction si applicable
                if (translationErrorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = translationErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bloc Contexte
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditingContext = true }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contexte :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEditingContext) "Touche Entrée pour valider" else "Toucher pour modifier",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))

                        if (isEditingContext) {
                            OutlinedTextField(
                                value = contextSentence,
                                onValueChange = { contextSentence = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(contextFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            hasGainedContextFocus = true
                                        } else if (hasGainedContextFocus) {
                                            isEditingContext = false
                                        }
                                    },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                minLines = 2,
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    isEditingContext = false
                                    focusManager.clearFocus()
                                })
                            )
                        } else {
                            Text(
                                text = if (contextSentence.isNotBlank()) "« $contextSentence »" else "« Toucher pour corriger le contexte »",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = if (contextSentence.isNotBlank())
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Section IA : Placée en dessous du champ Contexte et juste avant les boutons d'action
                if (onTranslateAi != null) {
                    if (aiExamples.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                if (word.isNotBlank()) {
                                    isAiTranslating = true
                                    translationErrorMessage = null
                                    coroutineScope.launch {
                                        val res = onTranslateAi(word, contextSentence, sourceLangCode, targetLangCode)
                                        res.onSuccess { aiResult ->
                                            if (aiResult.traduction.isNotBlank()) {
                                                translation = aiResult.traduction
                                            }
                                            aiExamples = aiResult.exemples
                                            translationErrorMessage = null
                                        }.onFailure { ex ->
                                            translationErrorMessage = ex.message ?: "Échec de l'analyse IA"
                                        }
                                        isAiTranslating = false
                                    }
                                }
                            },
                            enabled = word.isNotBlank() && !isTranslating && !isAiTranslating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("translate_ai_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            if (isAiTranslating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyse IA en cours...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "IA Exemples & Traduction",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exemples & Traduction via IA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Carte des Exemples d'utilisation IA (affichée quand des exemples sont présentés)
                    if (aiExamples.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_examples_card")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Exemples d'utilisation (IA) :",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                aiExamples.forEachIndexed { index, ex ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        if (ex.textOrigine.isNotBlank()) {
                                            Text(
                                                text = "« ${ex.textOrigine} »",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (ex.traduction.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "→ ${ex.traduction}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_save_button")
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (word.isNotBlank()) {
                                val currentListName = availableLists.find { it.id == selectedListId }?.name ?: ""
                                val examplesJson = com.example.ai.AiTranslationManager.toJsonString(aiExamples)
                                onSave(word, translation, contextSentence, examplesJson, selectedListId, currentListName)
                            }
                        },
                        enabled = word.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("confirm_save_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }

    // Dialogue pour créer une nouvelle liste depuis l'enregistrement avec choix des langues
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = { Text("Nouvelle liste") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Ex: « Rome tome 1 », « Someone Like You », « Ma voisine me plaît »...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newListNameInput,
                        onValueChange = { newListNameInput = it },
                        label = { Text("Nom de la liste") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newListDescInput,
                        onValueChange = { newListDescInput = it },
                        label = { Text("Description (optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LanguagePairSelector(
                        sourceLanguageCode = newListSourceLang,
                        targetLanguageCode = newListTargetLang,
                        onSourceLanguageChange = { newListSourceLang = it },
                        onTargetLanguageChange = { newListTargetLang = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListNameInput.isNotBlank()) {
                            onCreateNewList(
                                newListNameInput.trim(),
                                newListDescInput.trim(),
                                newListSourceLang,
                                newListTargetLang
                            ) { createdId ->
                                selectedListId = createdId
                            }
                            showCreateListDialog = false
                        }
                    },
                    enabled = newListNameInput.isNotBlank()
                ) {
                    Text("Créer la liste")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
