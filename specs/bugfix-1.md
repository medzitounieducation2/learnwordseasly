# BUGFIX-1 : Suppression des mentions « Titre de l'ouvrage » et « Ouvrage »

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Refactoring & Simplification UX/Data  
**Statut :** Spécifié & À implémenter  

---

## 1. Contexte et Problématique

Dans la version initiale, chaque mot disposait d'un champ individuel `bookTitle` (*« Titre de l'ouvrage »*).
Depuis la mise en place des listes personnalisées (ex: *« Rome tome 1 »*, *« Ma voisine me plaît »*), les mots sont rattachés directement à ces listes. Comme les utilisateurs nomment généralement leurs listes d'après leurs ouvrages ou lectures en cours, le champ « Titre de l'ouvrage » constitue une redondance inutile à la fois dans l'interface de saisie et dans la base de données.

De plus, l'utilisation du terme *« ouvrage »* dans les libellés (ex. *« Contexte dans l'ouvrage : »*, *« Nouvelle liste d'ouvrage »*, etc.) restreint sémantiquement l'application alors que l'utilisateur manipule des **listes**.

---

## 2. Spécifications Fonctionnelles

### 2.1. Dialogue d'Enregistrement (`WordSaveDialog`)
* **Suppression du champ de saisie :** Retirer entièrement le champ de formulaire `Titre de l'ouvrage (optionnel)` et son icône de livre.
* **Simplification du contexte :** Remplacer le libellé `Contexte dans l'ouvrage :` par `Contexte :` (conserve le comportement inline d'édition au clic et sortie au clic extérieur).
* **Sélection de liste unique :** Le mot est affecté à sa liste via le sélecteur `Ajouter à la liste :`.

### 2.2. Cartes Mémo Réversibles (`FlashcardsDialog`)
* Remplacer l'affichage `Ouvrage : <bookTitle>` au verso par `Liste : <listName>` si renseigné.

### 2.3. Gestionnaire de Vocabulaire (`VocabularyListScreen`)
* **En-tête :** Remplacer le sous-titre `... dans cet ouvrage` par `... dans cette liste`.
* **Boîte de création de liste :**
  * Titre : `Nouvelle liste` (au lieu de `Nouvelle liste d'ouvrage`).
  * Champ : `Nom de la liste` (au lieu de `Nom de la liste / ouvrage`).
* **Boîte de modification de mot :** Retirer le champ de texte `Ouvrage`.
* **Fiches de vocabulaire (`VocabularyCard`) :** Retirer l'affichage de l'ancien `bookTitle`. Le badge de liste (`listName`) reste affiché en vue globale pour identifier la provenance du mot.

---

## 3. Spécifications Techniques & Données

### 3.1. Modèle de Données (`VocabularyEntity`)
* Retirer ou déprécier `bookTitle` du modèle `VocabularyEntity`.
* Le rattachement contextuel repose exclusivement sur `listId` (clé étrangère) et `listName` (dénomination de la liste).

### 3.2. DAO (`VocabularyDao`)
* Mettre à jour la requête de recherche plein texte pour rechercher sur `word`, `definition`, `contextSentence` et `listName` sans faire référence à `bookTitle`.

### 3.3. ViewModel & Écrans
* Nettoyer les signatures de méthodes (`saveWord`, `processImage`, `OcrInteractiveScreen`, `WordSaveDialog`) pour supprimer le paramètre obsolète `bookTitle`.
