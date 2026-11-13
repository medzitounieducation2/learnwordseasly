# SPEC-15 : Bugfix - Modification de la Liste Rattachée à un Mot

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Bugfix & Ergonomie Édition  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Problématique

Lors de la lecture d'un livre, un utilisateur peut enregistrer par inadvertance un mot extrait dans la mauvaise liste de vocabulaire (par exemple dans la liste *"Someone Like You"* au lieu de *"Dualed"*).

Actuellement, lors de l'ouverture du dialogue d'édition d'un mot (*"Modifier l'entrée"*), l'utilisateur a la possibilité de modifier le mot, la traduction, la phrase de contexte et les exemples IA, mais **aucun sélecteur de liste n'est affiché**, rendant impossible la réassignation du mot à une autre liste.

**Objectif :**
Offrir la possibilité de changer la liste rattachée à un mot directement depuis le dialogue d'édition de mot.

---

## 2. Spécifications Ergonomiques & UX

### 2.1. Dialogue de Modification d'un Mot (`VocabularyListScreen.kt`)
* Ajout d'un sélecteur de liste d'attachement (`Liste rattachée :`) en haut ou au sein du dialogue d'édition.
* Le sélecteur présente sous forme de menu déroulant (`OutlinedCard` + `DropdownMenu`) la liste actuellement rattachée ainsi que toutes les listes de vocabulaire disponibles.
* Chaque entrée du menu déroulant affiche le nom de la liste et son badge de paire de langues (ex: `FR → EN`).
* Lors de la sélection d'une nouvelle liste :
  1. `editListId` est mis à jour avec l'identifiant de la nouvelle liste.
  2. Le libellé du champ de traduction s'adapte automatiquement à la langue cible de la nouvelle liste.

### 2.2. Scénario Utilisateur
1. **Étape 1 :** L'utilisateur extrait un mot et l'enregistre par erreur dans la liste *"Someone Like You"*.
2. **Étape 2 :** Le lendemain, l'utilisateur ouvre l'application, repère le mot et clique sur le bouton de modification (icône Crayon `Edit`).
3. **Étape 3 :** Dans le dialogue *"Modifier l'entrée"*, il clique sur le sélecteur de liste, choisit la liste *"Dualed"*, puis clique sur *"Enregistrer"*.
4. **Résultat :** Le mot est transféré dans la liste *"Dualed"* avec son `listId` et `listName` correctement mis à jour en base Room.

---

## 3. Spécifications Techniques

### 3.1. Dialogue `wordToEdit` dans `VocabularyListScreen.kt`
* Ajout du composant de sélection de liste (`DropdownMenu`) avec la liste des `VocabularyListEntity`.
* Mise à jour du State `editListId`.
* Lors de l'enregistrement (`onUpdateWord`), la copie de l'entité `VocabularyEntity` est soumise avec le nouveau `listId` et `listName`.

---

## 4. Matrice de Validation
1. Ouvrir le dialogue de modification d'un mot existant.
2. Vérifier que la liste actuelle du mot est affichée dans un menu déroulant.
3. Sélectionner une autre liste et cliquer sur *"Enregistrer"*.
4. Vérifier que le mot apparaît désormais sous la nouvelle liste sélectionnée.
