package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.VocabularyEntity
import com.example.data.VocabularyListEntity
import com.example.ui.VocabFilter
import com.example.ui.components.LanguagePairBadge
import com.example.ui.components.LanguagePairSelector
import com.example.translation.TranslationManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran 3 : Enregistrement et Base de Données Locale (Room Database).
 * Affiche la liste des mots extraits, regroupés par listes/ouvrages personnalisés,
 * permet la recherche instantanée, le filtrage, les cartes mémo et l'édition.
 */
@Composable
fun VocabularyListScreen(
    words: List<VocabularyEntity>,
    lists: List<VocabularyListEntity>,
    selectedListId: Long?,
    searchQuery: String,
    filter: VocabFilter,
    onSearchChange: (String) -> Unit,
    onFilterChange: (VocabFilter) -> Unit,
    onSelectList: (Long?) -> Unit,
    onCreateList: (String, String, String, String) -> Unit,
    onUpdateList: (VocabularyListEntity) -> Unit,
    onDeleteList: (VocabularyListEntity) -> Unit,
    onToggleFavorite: (VocabularyEntity) -> Unit,
    onUpdateWord: (VocabularyEntity) -> Unit,
    onDeleteWord: (VocabularyEntity) -> Unit,
    onSpeak: (String) -> Unit,
    onNavigateToScan: () -> Unit,
    onTranslate: suspend (text: String, sourceLang: String, targetLang: String) -> Result<String> = { text, _, _ -> Result.success(text) },
    onTranslateAi: (suspend (text: String, context: String, sourceLang: String, targetLang: String) -> Result<com.example.ai.AiTranslationResult>)? = null,
    onExportCsv: (Uri, Long) -> Unit = { _, _ -> },
    onImportCsv: (Uri, Long?) -> Unit = { _, _ -> },
    onDownloadTemplateCsv: (Uri) -> Unit = { _ -> },
    onRevisionPerformed: ((VocabularyEntity) -> VocabularyEntity)? = null,
    onTranslationRecognized: ((VocabularyEntity) -> VocabularyEntity)? = null,
    onTranslationNotRecognized: ((VocabularyEntity) -> VocabularyEntity)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showFlashcards by remember { mutableStateOf(false) }
    var wordToEdit by remember { mutableStateOf<VocabularyEntity?>(null) }
    var wordToDelete by remember { mutableStateOf<VocabularyEntity?>(null) }

    // Dialogues de gestion des listes
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var newListDescription by remember { mutableStateOf("") }
    var newListSourceLang by remember { mutableStateOf("fr") }
    var newListTargetLang by remember { mutableStateOf("en") }

    var listToEdit by remember { mutableStateOf<VocabularyListEntity?>(null) }
    var listToDelete by remember { mutableStateOf<VocabularyListEntity?>(null) }

    var pendingExportListId by remember { mutableStateOf<Long?>(null) }
    var pendingImportListId by remember { mutableStateOf<Long?>(null) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val listId = pendingExportListId
        if (uri != null && listId != null) {
            onExportCsv(uri, listId)
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportCsv(uri, pendingImportListId)
            showCreateListDialog = false
            listToEdit = null
        }
    }

    val downloadTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            onDownloadTemplateCsv(uri)
        }
    }

    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.FRENCH) }

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

    val currentSelectedList = lists.find { it.id == selectedListId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vocabulary_list_screen")
    ) {
        // Barre supérieure avec barre de recherche et actions
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
                // Ligne 1 : Titre + Boutons Réviser / Partager
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentSelectedList != null) currentSelectedList.name else "Mon Vocabulaire",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${words.size})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bouton Flashcards
                        if (words.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showFlashcards = true },
                                modifier = Modifier.testTag("open_flashcards_button"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Réviser", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Bouton Partager
                        if (words.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val title = currentSelectedList?.name ?: "Mon Carnet de Vocabulaire"
                                    val summary = StringBuilder("📚 $title :\n\n")
                                    words.forEach {
                                        summary.append("• ${it.word}")
                                        if (it.translation.isNotBlank()) summary.append(" → ${it.translation}")
                                        if (it.contextSentence.isNotBlank()) summary.append("\n  « ${it.contextSentence} »")
                                        if (it.listName.isNotBlank()) summary.append(" [${it.listName}]")
                                        summary.append("\n")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, summary.toString())
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Partager le vocabulaire"))
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("share_vocabulary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager la liste",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ligne 2 : Sélecteur horizontal des listes
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
                                onClick = { onSelectList(null) },
                                label = { Text("Toutes", fontSize = 11.sp) },
                                modifier = Modifier.testTag("list_chip_all")
                            )
                        }

                        items(orderedLists, key = { it.id }) { list ->
                            FilterChip(
                                selected = selectedListId == list.id,
                                onClick = { onSelectList(list.id) },
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
                                modifier = Modifier.testTag("list_chip_${list.id}")
                            )
                        }

                        item {
                            ElevatedFilterChip(
                                selected = false,
                                onClick = {
                                    newListName = ""
                                    newListDescription = ""
                                    showCreateListDialog = true
                                },
                                label = { Text("Nouvelle", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                modifier = Modifier.testTag("add_list_chip")
                            )
                        }
                    }
                }

                // Bandeau d'actions si liste sélectionnée
                if (currentSelectedList != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentSelectedList.description.isNotBlank())
                                        currentSelectedList.description
                                    else
                                        "Liste : ${currentSelectedList.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                LanguagePairBadge(
                                    sourceLangCode = currentSelectedList.sourceLanguage,
                                    targetLangCode = currentSelectedList.targetLanguage
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        pendingExportListId = currentSelectedList.id
                                        exportCsvLauncher.launch("${currentSelectedList.name}.csv")
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Exporter en CSV",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { listToEdit = currentSelectedList },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Renommer la liste",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { listToDelete = currentSelectedList },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer la liste",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ligne 3 : Barre de Recherche + Filtres rapides consolidés sur UNE SEULE LIGNE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Rechercher...", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Effacer",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("vocabulary_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        FilterChip(
                            selected = filter == VocabFilter.ALL,
                            onClick = { onFilterChange(VocabFilter.ALL) },
                            label = { Text("Tous", fontSize = 10.sp) },
                            modifier = Modifier.testTag("filter_all_chip")
                        )
                        FilterChip(
                            selected = filter == VocabFilter.FAVORITES,
                            onClick = { onFilterChange(VocabFilter.FAVORITES) },
                            label = { Text("★", fontSize = 10.sp) },
                            modifier = Modifier.testTag("filter_favorites_chip")
                        )
                        FilterChip(
                            selected = filter == VocabFilter.RECENT,
                            onClick = { onFilterChange(VocabFilter.RECENT) },
                            label = { Text("24h", fontSize = 10.sp) },
                            modifier = Modifier.testTag("filter_recent_chip")
                        )
                    }
                }
            }
        }

        // Liste des cartes de vocabulaire
        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "Aucun mot ne correspond à votre recherche"
                        else if (currentSelectedList != null)
                            "Aucun mot dans « ${currentSelectedList.name} »"
                        else
                            "Aucun mot enregistré pour le moment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scannez une page pour toucher un mot et l'ajouter !",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToScan,
                        modifier = Modifier.testTag("start_scan_from_list_button")
                    ) {
                        Text("Scanner une page de livre")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(words, key = { it.id }) { item ->
                    VocabularyCard(
                        entity = item,
                        showListBadge = selectedListId == null,
                        dateString = dateFormat.format(Date(item.timestamp)),
                        onToggleFavorite = { onToggleFavorite(item) },
                        onSpeak = { onSpeak(item.word) },
                        onEdit = { wordToEdit = item },
                        onDelete = { wordToDelete = item }
                    )
                }
            }
        }
    }

    // Dialogue Créer une nouvelle liste
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
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Nom de la liste") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newListDescription,
                        onValueChange = { newListDescription = it },
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ou importer une liste depuis un fichier CSV :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            pendingImportListId = null
                            importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importer un fichier CSV", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    TextButton(
                        onClick = {
                            downloadTemplateLauncher.launch("modele_vocabulaire.csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Télécharger un modèle exemple (.csv)", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            onCreateList(
                                newListName.trim(),
                                newListDescription.trim(),
                                newListSourceLang,
                                newListTargetLang
                            )
                            showCreateListDialog = false
                        }
                    },
                    enabled = newListName.isNotBlank()
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

    // Dialogue Renommer / Modifier la liste
    if (listToEdit != null) {
        val editingList = listToEdit!!
        var editName by remember(editingList) { mutableStateOf(editingList.name) }
        var editDesc by remember(editingList) { mutableStateOf(editingList.description) }
        var editSourceLang by remember(editingList) { mutableStateOf(editingList.sourceLanguage) }
        var editTargetLang by remember(editingList) { mutableStateOf(editingList.targetLanguage) }

        AlertDialog(
            onDismissRequest = { listToEdit = null },
            title = { Text("Modifier la liste") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LanguagePairSelector(
                        sourceLanguageCode = editSourceLang,
                        targetLanguageCode = editTargetLang,
                        onSourceLanguageChange = { editSourceLang = it },
                        onTargetLanguageChange = { editTargetLang = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sauvegarde & Importation CSV :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pendingExportListId = editingList.id
                                exportCsvLauncher.launch("${editingList.name}.csv")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exporter", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                pendingImportListId = editingList.id
                                importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importer", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateList(
                                editingList.copy(
                                    name = editName.trim(),
                                    description = editDesc.trim(),
                                    sourceLanguage = editSourceLang,
                                    targetLanguage = editTargetLang
                                )
                            )
                            listToEdit = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation suppression de liste
    if (listToDelete != null) {
        val deletingList = listToDelete!!
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Supprimer la liste ?") },
            text = {
                Text("Voulez-vous supprimer la liste « ${deletingList.name} » ainsi que les mots qui y sont enregistrés ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteList(deletingList)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue d'édition d'un mot
    if (wordToEdit != null) {
        val editing = wordToEdit!!
        var newWord by remember(editing) { mutableStateOf(editing.word) }
        var newTranslation by remember(editing) { mutableStateOf(editing.translation) }
        var newContext by remember(editing) { mutableStateOf(editing.contextSentence) }
        var editListId by remember(editing) { mutableStateOf(editing.listId) }

        var isEditListDropdownExpanded by remember { mutableStateOf(false) }
        var isTranslatingWord by remember { mutableStateOf(false) }
        var isAiTranslatingWord by remember { mutableStateOf(false) }
        var editTranslationError by remember { mutableStateOf<String?>(null) }
        var editAiExamples by remember(editing) { mutableStateOf(editing.getAiExamplesList()) }

        val selectedEditList = lists.find { it.id == editListId }
        val editSrcLang = selectedEditList?.sourceLanguage ?: "fr"
        val editTgtLang = selectedEditList?.targetLanguage ?: "en"
        val tgtLangInfo = TranslationManager.getLanguage(editTgtLang)

        AlertDialog(
            onDismissRequest = { wordToEdit = null },
            title = { Text("Modifier l'entrée") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Sélecteur de liste rattachée
                    Text(
                        text = "Liste rattachée :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { isEditListDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_word_list_selector")
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
                                        text = selectedEditList?.name ?: "Sélectionner une liste",
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
                            expanded = isEditListDropdownExpanded,
                            onDismissRequest = { isEditListDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            lists.forEach { list ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = list.name,
                                                fontWeight = if (list.id == editListId) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            LanguagePairBadge(
                                                sourceLangCode = list.sourceLanguage,
                                                targetLangCode = list.targetLanguage
                                            )
                                        }
                                    },
                                    onClick = {
                                        editListId = list.id
                                        isEditListDropdownExpanded = false
                                        editTranslationError = null
                                    },
                                    modifier = Modifier.testTag("edit_word_list_item_${list.id}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newWord,
                        onValueChange = {
                            newWord = it
                            editTranslationError = null
                        },
                        label = { Text("Mot") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Champ Traduction avec bouton Traduire (ML Kit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTranslation,
                            onValueChange = {
                                newTranslation = it
                                editTranslationError = null
                            },
                            label = { Text("Traduction (${tgtLangInfo.flag} ${tgtLangInfo.displayName})") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newWord.isNotBlank()) {
                                    isTranslatingWord = true
                                    editTranslationError = null
                                    coroutineScope.launch {
                                        val res = onTranslate(newWord, editSrcLang, editTgtLang)
                                        res.onSuccess { translated ->
                                            newTranslation = translated
                                            editTranslationError = null
                                        }.onFailure { ex ->
                                            editTranslationError = ex.message ?: "Échec de la traduction"
                                        }
                                        isTranslatingWord = false
                                    }
                                }
                            },
                            enabled = newWord.isNotBlank() && !isTranslatingWord && !isAiTranslatingWord,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTranslatingWord) {
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

                    if (editTranslationError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = editTranslationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContext,
                        onValueChange = { newContext = it },
                        label = { Text("Contexte") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Section IA (Placée sous le Contexte, avant les boutons de validation)
                    if (onTranslateAi != null) {
                        if (editAiExamples.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    if (newWord.isNotBlank()) {
                                        isAiTranslatingWord = true
                                        editTranslationError = null
                                        coroutineScope.launch {
                                            val res = onTranslateAi(newWord, newContext, editSrcLang, editTgtLang)
                                            res.onSuccess { aiResult ->
                                                if (aiResult.traduction.isNotBlank()) {
                                                    newTranslation = aiResult.traduction
                                                }
                                                editAiExamples = aiResult.exemples
                                                editTranslationError = null
                                            }.onFailure { ex ->
                                                editTranslationError = ex.message ?: "Échec de l'analyse IA"
                                            }
                                            isAiTranslatingWord = false
                                        }
                                    }
                                },
                                enabled = newWord.isNotBlank() && !isTranslatingWord && !isAiTranslatingWord,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                if (isAiTranslatingWord) {
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
                                        contentDescription = "Exemples & Traduction IA",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Exemples & Traduction via IA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Carte des Exemples d'utilisation IA
                        if (editAiExamples.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.fillMaxWidth()
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
                                    editAiExamples.forEachIndexed { index, ex ->
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentListName = lists.find { it.id == editListId }?.name ?: editing.listName
                        val examplesJson = com.example.ai.AiTranslationManager.toJsonString(editAiExamples)
                        onUpdateWord(
                            editing.copy(
                                word = newWord.trim(),
                                translation = newTranslation.trim(),
                                contextSentence = newContext.trim(),
                                aiExamplesJson = examplesJson,
                                listId = editListId,
                                listName = currentListName
                            )
                        )
                        wordToEdit = null
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { wordToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation de suppression d'un mot
    if (wordToDelete != null) {
        AlertDialog(
            onDismissRequest = { wordToDelete = null },
            title = { Text("Supprimer ce mot ?") },
            text = { Text("Voulez-vous supprimer définitivement « ${wordToDelete?.word} » de votre vocabulaire ?") },
            confirmButton = {
                Button(
                    onClick = {
                        wordToDelete?.let { onDeleteWord(it) }
                        wordToDelete = null
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { wordToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modale Flashcards
    if (showFlashcards) {
        FlashcardsDialog(
            words = words,
            lists = lists,
            onDismiss = { showFlashcards = false },
            onSpeak = onSpeak,
            onRevisionPerformed = onRevisionPerformed,
            onTranslationRecognized = onTranslationRecognized,
            onTranslationNotRecognized = onTranslationNotRecognized
        )
    }
}

@Composable
private fun VocabularyCard(
    entity: VocabularyEntity,
    showListBadge: Boolean,
    dateString: String,
    onToggleFavorite: () -> Unit,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vocabulary_card_${entity.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Ligne supérieure : Mot, Prononciation, Badge liste, Favori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSpeak,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (entity.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favori",
                            tint = if (entity.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Traduction
            if (entity.translation.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Traduction",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entity.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 14.sp
                    )
                }
            }

            // Contexte extrait de la page
            if (entity.contextSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "« ${entity.contextSentence} »",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Exemples IA enregistrés
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
                        aiExamples.forEach { ex ->
                            Spacer(modifier = Modifier.height(2.dp))
                            if (ex.textOrigine.isNotBlank()) {
                                Text(
                                    text = "« ${ex.textOrigine} »",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (ex.traduction.isNotBlank()) {
                                Text(
                                    text = "→ ${ex.traduction}",
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

            Spacer(modifier = Modifier.height(4.dp))

            // Pied de carte : Date + Boutons Modifier/Supprimer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
