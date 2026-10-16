package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.VocabularyEntity
import com.example.ui.VocabFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran 3 : Enregistrement et Base de Données Locale (Room Database).
 * Affiche la liste des mots extraits, permet la recherche instantanée,
 * le filtrage (favoris / récents), la révision par cartes mémo et l'édition.
 */
@Composable
fun VocabularyListScreen(
    words: List<VocabularyEntity>,
    searchQuery: String,
    filter: VocabFilter,
    onSearchChange: (String) -> Unit,
    onFilterChange: (VocabFilter) -> Unit,
    onToggleFavorite: (VocabularyEntity) -> Unit,
    onUpdateWord: (VocabularyEntity) -> Unit,
    onDeleteWord: (VocabularyEntity) -> Unit,
    onSpeak: (String) -> Unit,
    onNavigateToScan: () -> Unit
) {
    val context = LocalContext.current
    var showFlashcards by remember { mutableStateOf(false) }
    var wordToEdit by remember { mutableStateOf<VocabularyEntity?>(null) }
    var wordToDelete by remember { mutableStateOf<VocabularyEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.FRENCH) }

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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mon Vocabulaire",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${words.size} mot${if (words.size > 1) "s" else ""} mémorisé${if (words.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        // Bouton Flashcards
                        if (words.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showFlashcards = true },
                                modifier = Modifier.testTag("open_flashcards_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Réviser", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Bouton Partager
                        if (words.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val summary = StringBuilder("📚 Mon Carnet de Vocabulaire :\n\n")
                                    words.forEach {
                                        summary.append("• ${it.word}")
                                        if (it.definition.isNotBlank()) summary.append(" : ${it.definition}")
                                        if (it.bookTitle.isNotBlank()) summary.append(" (${it.bookTitle})")
                                        summary.append("\n")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, summary.toString())
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Partager mon vocabulaire"))
                                },
                                modifier = Modifier.testTag("share_vocabulary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager la liste",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Champ de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Rechercher un mot, auteur, définition...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vocabulary_search_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filtres rapides
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedFilterChip(
                        selected = filter == VocabFilter.ALL,
                        onClick = { onFilterChange(VocabFilter.ALL) },
                        label = { Text("Tous") },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    ElevatedFilterChip(
                        selected = filter == VocabFilter.FAVORITES,
                        onClick = { onFilterChange(VocabFilter.FAVORITES) },
                        label = { Text("Favoris") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.testTag("filter_favorites_chip")
                    )
                    ElevatedFilterChip(
                        selected = filter == VocabFilter.RECENT,
                        onClick = { onFilterChange(VocabFilter.RECENT) },
                        label = { Text("Récents (24h)") },
                        modifier = Modifier.testTag("filter_recent_chip")
                    )
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(words, key = { it.id }) { item ->
                    VocabularyCard(
                        entity = item,
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

    // Dialogue d'édition de définition
    if (wordToEdit != null) {
        val editing = wordToEdit!!
        var newWord by remember { mutableStateOf(editing.word) }
        var newDef by remember { mutableStateOf(editing.definition) }
        var newBook by remember { mutableStateOf(editing.bookTitle) }

        AlertDialog(
            onDismissRequest = { wordToEdit = null },
            title = { Text("Modifier l'entrée") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newWord,
                        onValueChange = { newWord = it },
                        label = { Text("Mot") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDef,
                        onValueChange = { newDef = it },
                        label = { Text("Définition / note") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBook,
                        onValueChange = { newBook = it },
                        label = { Text("Ouvrage") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateWord(
                            editing.copy(
                                word = newWord.trim(),
                                definition = newDef.trim(),
                                bookTitle = newBook.trim()
                            )
                        )
                        wordToEdit = null
                    }
                ) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { wordToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Confirmation de suppression
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
            onDismiss = { showFlashcards = false },
            onSpeak = onSpeak
        )
    }
}

@Composable
private fun VocabularyCard(
    entity: VocabularyEntity,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ligne supérieure : Mot, Prononciation, Favori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Écouter",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (entity.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favori",
                        tint = if (entity.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Définition (si présente)
            if (entity.definition.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entity.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Contexte extrait de la page
            if (entity.contextSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "« ${entity.contextSentence} »",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pied de carte : Ouvrage, Date, Boutons Modifier/Supprimer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entity.bookTitle.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entity.bookTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
