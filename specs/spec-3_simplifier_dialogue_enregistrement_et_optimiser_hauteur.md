# SPEC-3 : Simplification du Dialogue d'Enregistrement, Suppression de la Définition et Optimisation de la Hauteur

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Amélioration UX / Ergonomie & Simplification de Schéma  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Pour rendre la saisie de vocabulaire plus fluide et directe pendant la lecture :
1. **Suppression définitive du champ Définition / Note personnelle** :
   * Retrait complet du champ *"Définition ou note personnelle"* et des puces de balises (*"Nom"*, *"Verbe"*, *"Adjectif"*, *"Figure de style"*).
   * Suppression de la colonne `definition` du schéma de base de données Room (`VocabularyEntity` v6).
   * Retrait de l'affichage de la définition dans toutes les interfaces (Fiches de mots, Cartes mémo Flashcards, Export/Partage, Données de test).
2. **Optimisation de la hauteur et des espacements (Paddings/Margins)** du dialogue *"Mot extrait"* (`WordSaveDialog`) et du dialogue d'édition :
   * Réduire les marges et paddings internes pour compacter l'interface.
   * Définir une hauteur maximale adaptative (`heightIn(max = 520.dp)`) afin de garantir que les boutons d'action (*"Annuler"* et *"Enregistrer"*) restent bien dégagés et visibles au-dessus des boutons de navigation système natifs d'Android (ex: Xiaomi Redmi 13C).
   * Conserver un défilement vertical interne (`verticalScroll`) si le clavier virtuel s'ouvre.

---

## 2. Spécifications Fonctionnelles & UI

### 2.1. Simplification du Dialogue "Mot extrait" (`WordSaveDialog`)
* **Suppression complète de la Définition :**
  * Retrait du champ `OutlinedTextField` de définition et de la signature du callback `onSave(word, translation, contextSentence, listId, listName)`.
  * Suppression des puces d'aide (*Nom*, *Verbe*, *Adjectif*, *Figure de style*).
* **Réduction de la hauteur et ajustement des paddings :**
  * Le rembourrage interne principal de la `Surface` passe de `22.dp` à `14.dp` / `16.dp`.
  * Les espacements verticaux entre composants (`Spacer`) passent de `14.dp` à `8.dp` - `10.dp`.
  * La carte du mot extrait et le bloc de contexte sont rendus plus compacts.
  * La modale utilise une hauteur maximale adaptative (`heightIn(max = 520.dp)`) tout en conservant `verticalScroll(rememberScrollState())`.

### 2.2. Simplification du Dialogue de Modification de Mot (`VocabularyListScreen`)
* **Retrait du champ Définition / Note :**
  * Dans le dialogue d'édition accessible depuis la liste de vocabulaire, le champ *"Définition / note"* est également retiré.
  * Les champs conservés sont : *Mot*, *Traduction* (avec le bouton *Traduire*), et *Contexte*.

### 2.3. Suppression dans l'Affichage (Fiches & Flashcards)
* **Cartes de vocabulaire (`VocabularyCard`) :** Retrait de la section affichant la définition. Les éléments affichés sont : Mot, Prononciation audio, Traduction, Contexte et Badge de liste.
* **Révision Flashcards (`FlashcardsDialog`) :** Sur le verso de la carte mémo, affichage de la Traduction en grand, du Contexte et du badge de langues/liste. Le champ définition est retiré.

---

## 3. Spécifications Techniques

### 3.1. Évolution de la Base de Données Room (Passage en Version 6)
* **Entité `VocabularyEntity` :** Suppression de la propriété `val definition: String`.
* **Migration Room `MIGRATION_5_6` :**
  * Recréation de la table `vocabulary` sans la colonne `definition`.
  * `fallbackToDestructiveMigration` gère le nettoyage ou le réapprentissage sans bloquer l'application.
* **Dao & Repository :**
  * Mise à jour des requêtes de recherche (`searchWords`) pour chercher uniquement dans `word`, `translation`, `contextSentence` et `listName`.

### 3.2. Dispositions Compose Compactes
* Utilisation de `Modifier.fillMaxWidth(0.92f)` et `Modifier.heightIn(max = 520.dp)` sur la `Surface` des dialogues.
* Conservation de `verticalScroll` pour la gestion du clavier virtuel.
