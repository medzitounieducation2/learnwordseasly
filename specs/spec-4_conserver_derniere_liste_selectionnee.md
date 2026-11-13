# SPEC-4 : Persistance et Placement Prioritaire de la Dernière Liste Sélectionnée

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Amélioration UX / Ergonomie & Mémorisation d'État  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Actuellement, lors de la réouverture de l'application ou lors d'un nouveau scan, la sélection de la liste de vocabulaire est réinitialisée. De plus, lorsqu'une liste est sélectionnée, si elle se trouve plus loin dans le défilement horizontal, l'utilisateur ne la voit pas immédiatement au premier coup d'œil.

L'objectif de cette spécification est de :
1. **Mémoriser automatiquement et durablement la dernière liste sélectionnée** par l'utilisateur (ou le choix *"Toutes"* les listes) afin d'éviter les sélections répétitives.
2. **Placer la liste sélectionnée en position prioritaire au début du carrousel horizontal** (juste après la puce *"Toutes"*) dans l'écran de vocabulaire (`VocabularyListScreen`) pour une visibilité immédiate sans défilement horizontal obligatoire.

### Scénarios d'usage cibles :
1. **Pendant la lecture d'un livre (OCR & Enregistrement) :**
   * L'utilisateur lit l'ouvrage *"Someone Like You"*, scanne une page et enregistre un mot dans cette liste.
   * Lorsqu'il réouvre l'application 5 minutes ou 1 heure plus tard pour scanner une nouvelle page, le dialogue *"Mot extrait"* (`WordSaveDialog`) pré-sélectionne automatiquement la liste *"Someone Like You"*.
2. **Pendant la révision dans l'écran Vocabulaire (`VocabularyListScreen`) :**
   * L'utilisateur filtre son vocabulaire sur la liste *"Someone Like You"* pour réviser ses fiches ou ses Flashcards.
   * Lorsqu'il ferme puis relance l'application, l'écran de vocabulaire conserve le filtre *"Someone Like You"* actif ET affiche le chip *"Someone Like You"* en tête de liste, directement après *"Toutes"*.
   * Si l'utilisateur choisit explicitement l'onglet *"Toutes"* les listes, ce choix est également mémorisé et restauré au redémarrage, avec l'ordre initial des listes.

---

## 2. Spécifications Fonctionnelles & Comportementales

### 2.1. Unification de l'état de la liste active
* **Changement de liste dans l'Écran Vocabulaire (`VocabularyListScreen`) :**
  * Cliquer sur une liste spécifique (ex: *"Someone Like You"*) définit cette liste comme **liste active globale** pour l'affichage ET pour les futurs enregistrements de mots.
  * Cliquer sur *"Toutes"* conserve *"Toutes"* pour le filtrage de l'écran de vocabulaire, tout en conservant la dernière liste spécifique connue pour les enregistrements.
* **Changement ou création de liste dans le Dialogue d'Enregistrement (`WordSaveDialog`) :**
  * Sélectionner ou créer une nouvelle liste dans le dialogue modifie immédiatement la **liste active globale**.
  * À la réouverture de l'écran Vocabulaire, cette liste sera également pré-sélectionnée et affichée en tête.

### 2.2. Ordre d'affichage dynamique des chips de listes
* **Positionnement en tête de la liste active (`VocabularyListScreen`) :**
  * Lorsque `selectedListId` est non nul (une liste spécifique est sélectionnée), cette liste est placée en **première position** de la boucle `items(...)`, immédiatement après le chip *"Toutes"*.
  * Les autres listes non sélectionnées suivent dans leur ordre chronologique/alphabétique habituel.
  * Lorsque `selectedListId` est nul (l'onglet *"Toutes"* est sélectionné), les listes sont affichées dans leur ordre naturel.

### 2.3. Persistance au redémarrage
* L'état de sélection (`lastSelectedListId` et `lastActiveListId`) est sauvegardé immédiatement dès que l'utilisateur modifie sa sélection via `SharedPreferences`.
* Au démarrage de l'application (`MainViewModel`), l'état sauvegardé est restauré :
  * Si la liste enregistrée existe toujours dans la base de données Room, elle est activée et placée en tête.
  * Si la liste a été supprimée ou si c'est la toute première utilisation, l'application retombe proprement sur la première liste disponible ou *"Toutes"*.

---

## 3. Spécifications Techniques

### 3.1. Stockage léger via `SharedPreferences`
* Utilisation des `SharedPreferences` natifs d'Android via `Context.getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE)`.
* Clés de stockage :
  * `KEY_LAST_SELECTED_LIST_ID` (Long) : ID de la liste sélectionnée dans l'écran de vocabulaire (`-1L` pour *"Toutes"*).
  * `KEY_LAST_ACTIVE_LIST_ID` (Long) : ID de la liste active par défaut pour les enregistrements d'un mot.

### 3.2. Réordonnancement Compose (`VocabularyListScreen`)
* Réordonnancement réactif de la liste transmise aux chips dans Compose :
  ```kotlin
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
  ```
