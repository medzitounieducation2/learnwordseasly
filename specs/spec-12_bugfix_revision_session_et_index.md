# SPEC-12 : Bugfix - Correction de la Navigation et Gestion de Session de Révision

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Bugfix & Ergonomie Révision  
**Statut :** Implémenté & Validé

---

## 1. Description des Bugs & Objectifs

### Partie 1 : Bug de réinitialisation de l'index lors de la navigation entre cartes
* **Symptôme** : Lorsque l'utilisateur passe au 2ème mot (ou suivant) et clique pour afficher sa traduction, la traduction du 1er mot s'affiche à nouveau et les compteurs du 1er mot sont modifiés.
* **Cause** : `currentIndex` était mémorisé avec `remember(cardList)`. À chaque fois que `cardList` était mis à jour lors de l'enregistrement d'une révision, `remember(cardList)` réinitialisait `currentIndex` à 0.
* **Correction** : Mémoriser `currentIndex` indépendamment de la réassignation de la liste (`remember { mutableIntStateOf(0) }`).

### Partie 2 : Calcul multiple des révisions pour un même mot dans une même session
* **Symptôme** : Si l'utilisateur retourne plusieurs fois la même carte (Face A <-> Face B) au cours d'une même session de révision, le compteur `nbrRevisions` s'incrémentait à chaque retournement.
* **Règle métier** : Dans le cadre d'une même session de révision (une ouverture du dialogue de cartes mémo) :
  * Un mot ne doit compter que pour **une seule révision** (`nbrRevisions += 1` et `lastRevisionDate` calculés lors de la première révélation uniquement).
  * Les choix d'auto-évaluation (boutons vert / rouge) ne peuvent être effectués qu'**une seule fois par mot** au cours de la session.
  * Si l'utilisateur ferme puis rouvre la modale de révision, une nouvelle session débute et les révisions peuvent être à nouveau comptabilisées.

---

## 2. Spécifications Techniques

### 2.1. Correction du State `currentIndex` (`FlashcardsDialog.kt`)
* Modifier la déclaration :  
  `var currentIndex by remember { mutableIntStateOf(0) }` (suppression de la clé `cardList`).

### 2.2. Suivi de Session de Révision (`FlashcardsDialog.kt`)
* Mémoriser au niveau de la modale :
  * `val revisedWordIdsInSession = remember { mutableSetOf<Long>() }`
  * `val evaluatedWordIdsInSession = remember { mutableSetOf<Long>() }`
* **Lors du retournement vers la face B (traduction)** :
  * Déclencher `onRevisionPerformed` uniquement si `currentWord.id` n'est PAS présent dans `revisedWordIdsInSession`.
  * Ajouter `currentWord.id` dans `revisedWordIdsInSession`.
* **Lors du clic sur le bouton Vert ou Rouge** :
  * Autoriser le clic uniquement si `currentWord.id` n'est PAS présent dans `evaluatedWordIdsInSession`.
  * Ajouter `currentWord.id` dans `evaluatedWordIdsInSession`.
  * Griser/désactiver les deux boutons pour ce mot durant la session.
