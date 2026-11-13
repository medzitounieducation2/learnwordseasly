# SPEC-18 : Suivi & Comptage des Révisions des Phrases (Écoute Audio & Lecture Visuelle)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Statistiques de Révision & Modèle de Données  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Actuellement, les révisions globales du mot sont enregistrées. Cependant, un mot de vocabulaire est utilisé dans plusieurs phrases distinctes :
* La **phrase de contexte** d'origine de l'ouvrage (optionnelle).
* Les **phrases d'exemples IA** (jusqu'à 2 exemples, optionnelles).

**Objectif :**
Ajouter des indicateurs précis de suivi de révision **par phrase** (contexte et exemples IA) distinguant :
1. **Suivi Écoute Audio (`countListen`) :** Incrémenté automatiquement à la fin de la prononciation audio de la phrase.
2. **Suivi Lecture Visuelle (`countRead`) :** Incrémenté manuellement via un bouton dédié à côté de chaque phrase lors de la révision visuelle.

---

## 2. Spécifications du Modèle de Données

### 2.1. Structure pour un Mot de Vocabulaire
Chaque phrase rattachée à un mot possède son propre couple de compteurs `(countListen, countRead)` :
* **Phrase de Contexte :** `contextCountListen: Int`, `contextCountRead: Int`
* **Exemple IA 1 :** `countListen: Int`, `countRead: Int` (dans le JSON `aiExamplesJson`)
* **Exemple IA 2 :** `countListen: Int`, `countRead: Int` (dans le JSON `aiExamplesJson`)

### 2.2. Base de Données Room (Version 9)
Ajout des colonnes suivantes dans `VocabularyEntity` :
* `contextCountListen: Int = 0`
* `contextCountRead: Int = 0`

Migration `MIGRATION_8_9` dans `AppDatabase.kt` :
```sql
ALTER TABLE vocabulary ADD COLUMN contextCountListen INTEGER NOT NULL DEFAULT 0;
ALTER TABLE vocabulary ADD COLUMN contextCountRead INTEGER NOT NULL DEFAULT 0;
```

### 2.3. Sérialisation JSON `UsageExample` (`AiTranslationManager.kt`)
Extension de `UsageExample` :
```kotlin
data class UsageExample(
    val textOrigine: String,
    val traduction: String,
    val countListen: Int = 0,
    val countRead: Int = 0
)
```
Gestion de la rétrocompatibilité lors du parsing JSON pour initialiser `countListen` et `countRead` à 0 si absents.

---

## 3. Spécifications Ergonomiques & UX

### 3.1. Affichage des Compteurs dans les Cartes (`AdvancedRevisionScreen.kt` & `VocabularyListScreen.kt`)
Pour chaque phrase (contexte ou exemple IA) :
* **Indicateur Écoute Audio :** Un badge compact `🎧 X` (ou `🔊 X`) affichant le nombre d'écoutes audio terminées.
* **Bouton / Compteur de Lecture Visuelle :** Un bouton interactif `👁️ Y` (ou `📖 Y`) affichant le nombre de lectures visuelles.
  * Clic sur le bouton de lecture : incrémente `countRead` de +1 immédiatement pour cette phrase spécifique.

### 3.2. Incrémentation Automatique de l'Audio
* Lorsque l'audio d'une phrase se termine (que ce soit via l'icône audio individuelle ou pendant la lecture séquentielle "Play All") :
  * Le callback `onDone` déclenche l'incrémentation de `countListen` (+1) pour cette phrase.

---

## 4. Matrice de Validation
1. Lancer la lecture audio d'une phrase (individuelle ou Play All) jusqu'à la fin et vérifier l'incrémentation automatique du compteur audio `🎧`.
2. Cliquer sur le bouton de lecture visuelle `👁️` d'une phrase et vérifier l'incrémentation immédiate du compteur `countRead`.
3. Vérifier la persistance des compteurs après fermeture et réouverture de l'application.
4. Exécuter la migration Room V8 -> V9 et valider les tests unitaires Robolectric.
