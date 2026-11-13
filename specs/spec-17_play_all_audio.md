# SPEC-17 : Lecture Audio Séquentielle Automatique ("Play All")

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Nouvelle Fonctionnalité Audio & Ergonomie Révision  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Dans l'écran de **Révision Avancée**, chaque phrase de contexte et exemple dispose d'une icône pour l'écoute individuelle.

**Objectif :**
Offrir un bouton et une barre de lecture audio intégrée (**"Play All" / Lecture Continue**) permettant d'écouter automatiquement toutes les phrases de la liste de révision, les unes après les autres de manière séquentielle et fluide.

**Fonctionnalités clés :**
1. **Lecture Séquentielle Automatique ("Play All") :** Enchaînement automatique des phrases via la synthèse vocale native TTS.
2. **Mise en Valeur Visuelle de la Phrase Active :** Mise en évidence distincte de la carte en cours de lecture (couleur de fond accentuée, bordure primaire, badge audio animé 🔊) et défilement automatique (*Auto-scroll*) pour garder la phrase active visible à l'écran.
3. **Barre de Contrôles Audio Complète :**
   * **Play / Pause :** Démarrer / Mettre en pause la lecture.
   * **Stop :** Arrêter la lecture et réinitialiser la session.
   * **Précédent / Suivant :** Naviguer manuellement vers la phrase précédente ou suivante.
   * **Replay / Recommencer :** Réécouter la phrase courante ou réinitialiser au début.
4. **Mémorisation Persistante de l'Index de Lecture :** Sauvegarde automatique dans SharedPreferences (`last_playback_index`) de la dernière phrase lue pour reprendre la lecture ultérieurement.

---

## 2. Spécifications Ergonomiques & UX

### 2.1. Barre de Contrôles Audio (`AdvancedRevisionScreen.kt`)
* Un bandeau/carte de contrôle audio épuré est placé en haut de la zone de révision.
* Affichage de la progression : *"Lecture X / Y"* avec le nom de la liste et du mot actif.
* Boutons d'action compacts : `SkipPrevious`, `PlayArrow` / `Pause`, `Stop`, `SkipNext`, `Replay`.

### 2.2. Style Visuel de la Carte Active (`ContextSentenceCard` & `WordRevisionCard`)
* Lorsque `itemIndex == activePlaybackIndex` et que la lecture est en cours :
  * Le fond de la carte passe en `MaterialTheme.colorScheme.primaryContainer`.
  * Une bordure visible de 2.dp aux couleurs primaires encadre la carte.
  * Un badge d'animation audio *"🔊 En lecture"* est affiché dans l'en-tête de la carte.
  * La liste `LazyColumn` défile automatiquement vers l'élément courant via `LazyListState.animateScrollToItem`.

---

## 3. Spécifications Techniques

### 3.1. Gestionnaire TTS Avancé (`TtsManager.kt`)
* Ajout d'un `UtteranceProgressListener` sur l'instance `TextToSpeech` native Android.
* Émission d'un callback `onUtteranceCompleted(utteranceId)` à la fin de la prononciation de chaque phrase.
* Support du changement dynamique de langue (Français, Anglais, Espagnol, Arabe, etc.) via `setLanguage(Locale)`.

### 3.2. Mémorisation du Curseur (`MainViewModel.kt` / `AdvancedRevisionScreen.kt`)
* Clé SharedPreferences : `last_playback_index`.
* Maintien de l'index actif et reprise automatique lors de la réouverture de l'écran.

---

## 4. Matrice de Validation
1. Cliquer sur "Play All" et vérifier l'enchaînement séquentiel automatique des phrases.
2. Vérifier le style visuel de la phrase en cours de lecture et l'auto-scroll de la liste.
3. Tester les boutons Pause, Play, Stop, Suivant et Précédent.
4. Vérifier la mémorisation de l'index de lecture après fermeture et réouverture de l'écran.
