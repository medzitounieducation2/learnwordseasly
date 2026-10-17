# SPEC-3 : Configuration des Langues par Liste et Traduction des Mots

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Nouvelle fonctionnalité (Langues par liste & Traduction On-Device)  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

L'application *Vocabulaire Lecture* permet au lecteur d'apprendre du vocabulaire au fil de ses lectures dans différentes langues (ouvrages en français, en anglais, etc.).
Cette spécification introduit :
1. **La configuration des langues source et cible au niveau de chaque liste** (par défaut : Français `fr` vers Anglais `en`).
2. **L'héritage strict des langues par les mots de la liste** : un mot ne configure pas de langues individuelles, il suit la liste à laquelle il est rattaché.
3. **La traduction à la demande via Google ML Kit Translate** (lib Google 100 % gratuite, fonctionnant en local on-device). Un bouton dédié « Traduire » permet de traduire le mot extrait vers la langue cible sans traduction automatique imposée.
4. **La gestion claire des erreurs de traduction** : en cas d'échec (téléchargement du modèle de langue manquant, réseau indisponible pour le pack de langue, etc.), un message descriptif inline est affiché sous le champ.
5. **L'affichage de la traduction en révision (Flashcards)** : lors du retournement de la carte mémo, la traduction est mise en avant pour permettre à l'utilisateur de vérifier s'il a bien reconnu et mémorisé le mot.

---

## 2. Spécifications Fonctionnelles

### 2.1. Langues au niveau des listes de vocabulaire
* Chaque liste (`VocabularyListEntity`) possède désormais :
  * `sourceLanguage` : Code ISO de la langue d'origine du livre (ex: `"fr"`, `"en"`, `"ar"`, `"es"`, `"de"`, `"it"`). Valeur par défaut : `"fr"`.
  * `targetLanguage` : Code ISO de la langue de destination / traduction (ex: `"en"`, `"fr"`, `"ar"`, `"es"`, etc.). Valeur par défaut : `"en"`.
* Lors de la création d'une liste (depuis l'écran de vocabulaire ou depuis le dialogue d'enregistrement) :
  * L'utilisateur peut choisir la langue d'origine et la langue de destination parmi une liste de langues courantes (Français, Anglais, Arabe, Espagnol, Allemand, Italien, etc.).
  * Les valeurs par défaut sont pré-sélectionnées à `fr → en`.
  * Un bouton d'inversion rapide (Swap $\leftrightarrow$) permet de basculer instantanément (ex: `en → fr`).
* Lors de l'édition d'une liste existante :
  * L'utilisateur peut modifier les langues de la liste.
* **Héritage strict :** Les mots d'une liste adoptent automatiquement le couple de langues de leur liste parente. L'utilisateur n'a pas à redéfinir la langue pour chaque mot.

### 2.2. Dialogue d'enregistrement d'un mot (`WordSaveDialog`)
* **Indicateur de langues de la liste active :** Affiche un badge visuel clair de la paire de langues de la liste sélectionnée (ex: `Anglais (EN) → Français (FR)`).
* **Nouveau champ « Traduction » :** Champ de texte dédié pour enregistrer la traduction du mot dans la langue de destination.
* **Bouton « Traduire » :**
  * Situé à côté ou directement associé au champ de traduction.
  * Déclenché manuellement par l'utilisateur (pas de traduction automatique silencieuse).
  * Pendant la traduction : affiche un indicateur de chargement (`CircularProgressIndicator`) discret et désactive le bouton.
  * En cas de succès : pré-remplit le champ de traduction avec le texte traduit renvoyé par ML Kit Translate.
  * En cas d'échec : affiche un message d'erreur explicatif en rouge/ambre sous le champ (ex. : *"Téléchargement du modèle de langue requis ou indisponible"* ou *"Erreur : [détail]"*).
* **Bouton « Enregistrer » :** Sauvegarde le mot, sa traduction dans la langue de destination, sa définition/note, et sa phrase de contexte.

### 2.3. Révision par Cartes Mémo (`FlashcardsDialog`)
* **Face avant (Recto) :**
  * Affiche le mot dans sa langue d'origine, le bouton de prononciation audio, et la phrase de contexte.
* **Face arrière (Verso) :**
  * Affiche la **Traduction** en évidence (en grand, style titre avec badge de la langue cible) pour vérifier immédiatement la compréhension du mot.
  * Affiche la définition / notes personnelles de l'utilisateur si renseignées.
  * Affiche le nom de la liste et les langues associées (ex: `EN → FR`).

### 2.4. Écran de Vocabulaire (`VocabularyListScreen`)
* Affiche les badges de langues sur les listes (ex: `FR → EN`).
* Dans chaque fiche de mot (`VocabularyCard`), affiche la traduction lorsqu'elle est disponible avec un indicateur discret.
* Dans le dialogue de modification d'un mot existant, permet de modifier la traduction et de relancer la traduction au besoin.

---

## 3. Spécifications Techniques

### 3.1. Dépendance Google ML Kit Translate
* Bibliothèque : `com.google.mlkit:translate:17.0.3`
* 100 % gratuite, fournie par Google, traite la traduction on-device après téléchargement du modèle léger de la paire de langues.
* Classe utilitaire : `TranslationManager` (ou `MlKitTranslator`) encapsulant la configuration des `TranslatorOptions`, le téléchargement sécurisé du modèle (`downloadModelIfNeeded`) et la traduction asynchrone (`translate`) avec gestion propre des exceptions et libération des ressources.

### 3.2. Évolution du Schéma de Données (Room v5)
* **Table `vocabulary_lists` :**
  * Ajout de `sourceLanguage TEXT NOT NULL DEFAULT 'fr'`
  * Ajout de `targetLanguage TEXT NOT NULL DEFAULT 'en'`
* **Table `vocabulary` :**
  * Ajout de `translation TEXT NOT NULL DEFAULT ''`
* **Migration Room `MIGRATION_4_5` :**
  * `ALTER TABLE vocabulary_lists ADD COLUMN sourceLanguage TEXT NOT NULL DEFAULT 'fr'`
  * `ALTER TABLE vocabulary_lists ADD COLUMN targetLanguage TEXT NOT NULL DEFAULT 'en'`
  * `ALTER TABLE vocabulary ADD COLUMN translation TEXT NOT NULL DEFAULT ''`
* Migration complète rétrocompatible (1→5, 2→5, 3→5, 4→5) et `fallbackToDestructiveMigration` maintenu pour la sécurité locale.

### 3.3. Langues supportées (codes BCP-47 / ML Kit)
* `fr` : Français
* `en` : Anglais
* `ar` : Arabe
* `es` : Espagnol
* `de` : Allemand
* `it` : Italien
* `pt` : Portugais
* `ru` : Russe
* `zh` : Chinois
* `ja` : Japonais
