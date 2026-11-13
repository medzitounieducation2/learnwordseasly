# SPEC-8 : Enregistrement des Exemples IA en Base de Données & Support CSV (Export/Import)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Persistance de Données & Intégration Backup CSV  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Jusqu'à présent, la fonctionnalité IA (SPEC-7) permettait de récupérer la traduction et des exemples d'utilisation en contexte, mais ces exemples n'étaient qu'affichés temporairement.  
Afin d'éviter d'imposer de nouvelles requêtes IA à chaque consultation d'un mot et d'économiser les tokens d'API, l'application doit désormais **sauvegarder les exemples IA en base de données Room** et **les intégrer dans l'import/export CSV**.

**Principes clés :**
1. **Pas de génération ni sauvegarde automatique** : Les exemples ne sont récupérés que si l'utilisateur clique sur le bouton *"Exemples & Traduction via IA"*, et ne sont sauvegardés que si l'utilisateur clique ensuite sur *"Enregistrer"*.
2. **Ré-ouverture du dialogue ("Modifier l'entrée")** : Si un mot possède déjà des exemples IA enregistrés en base :
   * Les exemples s'affichent automatiquement sous le contexte.
   * **Le bouton IA est masqué** pour éviter de regénérer inutilement les exemples.
3. **Mise à jour du support CSV (Export & Import)** :
   * Les fichiers CSV de listes incluent une nouvelle colonne `exemples`.
   * Structure CSV : `mot;traduction;contexte;exemples;date_creation`.
   * Le contenu de la colonne `exemples` est au format JSON (`{"exemples": [{"text_origine": "...", "traduction": "..."}]}`).
   * Rétrocompatibilité préservée avec les fichiers CSV existants à 4 colonnes.

---

## 2. Spécifications Techniques

### 2.1. Base de Données Room (Version 7)
* Ajout du champ `aiExamplesJson: String = ""` dans l'entité `VocabularyEntity`.
* Création de la migration `MIGRATION_6_7` : `ALTER TABLE vocabulary ADD COLUMN aiExamplesJson TEXT NOT NULL DEFAULT ''`.

### 2.2. Dialogues d'Enregistrement (`WordSaveDialog` & `VocabularyListScreen`)
* **Signature de `saveWord` (`MainViewModel.kt`)** :
  Accepte le paramètre `aiExamplesJson: String = ""`.
* **Affichage conditionnel du bouton IA dans "Modifier l'entrée"** :
  Si `aiExamples.isNotEmpty()` ou `aiExamplesJson.isNotBlank()`, le bouton IA n'est pas affiché, et seuls les exemples existants sont présentés.

### 2.3. Gestionnaire CSV (`CsvManager.kt`)
* Format d'en-tête mis à jour : `mot;traduction;contexte;exemples;date_creation`.
* Export : Sérialisation de `aiExamplesJson`.
* Import : Lecture sécurisée supportant 4 colonnes (ancien format) ou 5 colonnes (nouveau format avec `exemples`).
