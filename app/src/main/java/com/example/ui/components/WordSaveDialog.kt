package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.data.VocabularyListEntity
import com.example.ocr.DetectedWord

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSaveDialog(
    detectedWord: DetectedWord,
    availableLists: List<VocabularyListEntity>,
    initialSelectedListId: Long?,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onCreateNewList: (String, (Long) -> Unit) -> Unit,
    onSave: (word: String, definition: String, contextSentence: String, listId: Long, listName: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val contextFocusRequester = remember { FocusRequester() }

    var word by remember(detectedWord) { mutableStateOf(detectedWord.cleanWord) }
    var definition by remember(detectedWord) { mutableStateOf("") }
    var contextSentence by remember(detectedWord) { mutableStateOf(detectedWord.lineText) }

    // Sélection de la liste
    var selectedListId by remember(availableLists, initialSelectedListId) {
        val initial = availableLists.find { it.id == initialSelectedListId }?.id
            ?: availableLists.firstOrNull()?.id
            ?: 1L
        mutableStateOf(initial)
    }

    var isListDropdownExpanded by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListNameInput by remember { mutableStateOf("") }

    // État d'édition inline du contexte sans aucun bouton additionnel
    var isEditingContext by remember { mutableStateOf(false) }
    var hasGainedContextFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isEditingContext) {
        if (isEditingContext) {
            hasGainedContextFocus = false
            contextFocusRequester.requestFocus()
        }
    }

    val selectedList = availableLists.find { it.id == selectedListId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .testTag("word_save_dialog_surface")
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mot extrait",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_save_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sélecteur de liste
                Text(
                    text = "Ajouter à la liste :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isListDropdownExpanded = true }
                            .testTag("select_list_dropdown_card"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedList?.name ?: "Sélectionner une liste",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choisir la liste"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isListDropdownExpanded,
                        onDismissRequest = { isListDropdownExpanded = false }
                    ) {
                        availableLists.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    selectedListId = item.id
                                    isListDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Créer une nouvelle liste...",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            onClick = {
                                isListDropdownExpanded = false
                                newListNameInput = ""
                                showCreateListDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Champ d'édition du mot extrait + bouton d'écoute audio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = word,
                        onValueChange = { word = it },
                        label = { Text("Mot à enregistrer") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("extracted_word_input"),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onSpeak(word) },
                        modifier = Modifier
                            .testTag("speak_word_button")
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Écouter la prononciation",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Contexte extrait de la page (cliquable pour édition en ligne, retour à la normale au clic dehors)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("context_sentence_card")
                        .clickable(enabled = !isEditingContext) {
                            isEditingContext = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEditingContext)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Contexte :",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
                                    }
                                    .testTag("context_sentence_input"),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                placeholder = {
                                    Text(
                                        "Corriger ou saisir le contexte...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                                    )
                                },
                                maxLines = 4,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Champ Définition ou note
                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Définition ou note personnelle (optionnel)") },
                    placeholder = { Text("Ex: Sens, synonyme ou pensée...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_definition_input"),
                    minLines = 2,
                    maxLines = 4
                )

                // Suggestions rapides de balises
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Nom", "Verbe", "Adjectif", "Figure de style").forEach { tag ->
                        AssistChip(
                            onClick = {
                                definition = if (definition.isBlank()) "[$tag] " else "$definition [$tag] "
                            },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (word.isNotBlank()) {
                                val currentListName = availableLists.find { it.id == selectedListId }?.name ?: ""
                                onSave(word, definition, contextSentence, selectedListId, currentListName)
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

    // Dialogue pour créer une nouvelle liste depuis l'enregistrement
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = { Text("Nouvelle liste") },
            text = {
                Column {
                    Text(
                        text = "Ex: « Rome tome 1 », « Ma voisine me plaît »...",
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListNameInput.isNotBlank()) {
                            onCreateNewList(newListNameInput) { createdId ->
                                selectedListId = createdId
                            }
                            showCreateListDialog = false
                        }
                    },
                    enabled = newListNameInput.isNotBlank()
                ) {
                    Text("Créer")
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
