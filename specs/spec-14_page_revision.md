# SPEC-14 : Écran de Révision Avancée & Immersion Contextuelle

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Nouvelle Fonctionnalité UI & Ergonomie de Révision  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

L'objectif de cette spécification est de créer un nouvel écran dédié à la **Révision Avancée**, positionné au centre de la barre de navigation inférieure entre l'écran *Vocabulaire* et l'écran *OCR Studio*.

Cet écran s'adresse spécifiquement aux apprenants souhaitant réviser leurs mots en contexte (immersion par la lecture de phrases et d'expressions), sans distraction visuelle liée aux traductions ou à la recherche.

**Objectifs clés :**
1. **Nouvel Onglet au Milieu (`NavigationBar`) :**
   * Structure de navigation : `Vocabulaire` (à gauche) | `Révision` (au milieu) | `OCR Studio` (à droite).
2. **Sélection de Liste & Mémorisation Persistante :**
   * Sélecteur de liste ("Toutes les listes" ou une liste spécifique).
   * Mémorisation du dernier choix dans SharedPreferences (`KEY_LAST_REVISION_LIST_ID`).
3. **Interface Épurée de Révision :**
   * **Sans champ de recherche**.
   * **Sans description de liste**.
4. **Mode "Exemples & Contextes Uniquement" (Immersion Contextuelle) :**
   * Filtrage et extraction des phrases de contexte (`contextSentence`) et des exemples d'utilisation IA (`textOrigine`).
   * Mise en valeur visuelle du mot cible dans les phrases (en gras / couleur primaire).
   * **Choix d'affichage de la traduction :** Interrupteur / Toggle pour masquer ou afficher la traduction (dans les phrases et sous les mots).
5. **Fonctionnalités Avancées IA & Ergonomie Révision :**
   * **Mode Devine le Mot (Fill-in-the-blank) :** Option pour masquer le mot cible dans la phrase avec `[ ... ]` (clic pour révéler le mot).
   * **Synthesiseur Vocal (TTS) :** Écoute audio individuelle de chaque phrase de contexte et exemple.
   * **Filtres de Révision Avancée :** *Tous les mots*, *Favoris*, *Non révisés / Priorités*.
   * **Lancement Rapide des Flashcards :** Bouton pour ouvrir le dialogue interactif de mémorisation.

---

## 2. Spécifications Ergonomiques & UX

### 2.1. Barre de Navigation (`NavigationBar`)
* **Onglet 1 (Gauche) :** **Vocabulaire** (Icône `MenuBook`)
* **Onglet 2 (Milieu, Nouveau) :** **Révision** (Icône `School` ou `Psychology`)
* **Onglet 3 (Droite) :** **OCR Studio** (Icône `TouchApp`)

### 2.2. En-tête de la Page Révision (`AdvancedRevisionScreen.kt`)
* Titre principal : **"Révision Avancée"** avec badge du nombre d'éléments.
* **Sélecteur de Liste Horizontal :** Puces/Chips ("Toutes", Liste 1, Liste 2...).
* **Bandeau d'Options & Filtres Compacts :**
  * Toggle *"Exemples seuls"* (Mode Contexte / Immersion).
  * Toggle *"Afficher les traductions"* (Interrupteur avec icône `Translate`).
  * Toggle *"Masquer le mot (Devinette)"* (Remplace le mot cible par `[ ... ]`).
  * Quick Filter Chips : *Tous*, *Favoris (★)*, *À réviser*.
  * Bouton d'action *"Flashcards"* pour réviser la sélection sous forme de cartes mémos.

### 2.3. Affichage des Cartes de Contextes / Expressions
* En mode **"Exemples uniquement"** :
  * Élimination de toute répétition inutile (pas d'en-tête "Mot : ...").
  * Présentation fluide sous forme de cartes d'expression épurées pour une immersion totale.
  * Chaque phrase de contexte ou exemple d'utilisation affiche directement le texte original avec le mot cible mis en évidence en **gras** (et couleur primaire).
  * Si la traduction est activée, elle apparaît sous la phrase en italique.
  * Bouton d'écoute audio TTS direct pour chaque phrase d'exemple.
  * Si le mot est masqué (`[ ... ]`), un clic sur l'emplacement du mot le dévoile.

---

## 3. Spécifications Techniques

### 3.1. Navigation & State (`MainViewModel.kt`)
* Extension de l'enum `AppTab` :
  ```kotlin
  enum class AppTab {
      VOCABULARY,
      REVISION,
      OCR_STUDIO
  }
  ```
* Mémorisation dans SharedPreferences :
  * Clé `KEY_LAST_REVISION_LIST_ID` ("last_revision_selected_list_id").

### 3.2. Composant UI (`AdvancedRevisionScreen.kt`)
* Nouveau composant Jetpack Compose dans `com.example.ui.screens.AdvancedRevisionScreen`.
* Composants d'affichage des exemples (`ContextSentenceCard`, `WordRevisionCard`).
* Fonctions utilitaires de mise en valeur en gras du mot cible dans le texte.

---

## 4. Matrice de Validation
1. Vérifier le basculement vers l'onglet Révision (au milieu de la barre de navigation).
2. Vérifier que la sélection d'une liste sur la page Révision est conservée après redémarrage de l'application.
3. Vérifier que la page Révision ne contient ni champ de recherche ni description de liste.
4. Valider le mode "Exemples seuls" et le masque/démasque de la traduction.
5. Valider le mode devinette (mot masqué) et l'écoute vocale TTS.
