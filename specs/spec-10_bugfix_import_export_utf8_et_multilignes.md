# SPEC-10 : Correction des Bugs d'Import/Export CSV (Support UTF-8/Arabe et Sauts de Ligne)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Bugfix & Robustesse Persistance CSV  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Description du Problème & Objectifs

Lors de l'exportation et de la ré-importation de listes contenant des mots/traductions en langue Arabe (ou autres alphabets non-latins), ainsi que des exemples IA ou des phrases de contexte :
1. **Incompatibilité d'encodage UTF-8 (ex: Arabe)** : Les fichiers CSV ouverts dans Google Drive, Google Sheets ou Excel affichent des caractères corrompus/spéciaux à la place du texte en arabe.
2. **Décalage et corruption des colonnes à l'importation** : La méthode d'importation utilisait `reader.readLines()`, découpant aveuglément le fichier à chaque saut de ligne (`\n`). Si le champ JSON `aiExamplesJson` ou une phrase de contexte contenait des retour à la ligne, le fichier était découpé en plusieurs lignes CSV invalides. Cela provoquait un décalage complet des colonnes lors de la ré-importation (mots sans contexte, traductions inversées ou mélangées avec les exemples).

---

## 2. Spécifications Techniques

### 2.1. Ajout du BOM UTF-8 (`\uFEFF`) à l'Exportation (`CsvManager.kt`)
* Lors de la création du fichier CSV, écrire le marqueur d'ordre des octets UTF-8 (BOM : `\uFEFF`) au tout début du flux.
* Ce marqueur garantit que Google Drive, Sheets, Excel et les lecteurs de fichiers reconnaissent immédiatement l'encodage UTF-8 pour le texte en arabe, accentué ou multilingue.

### 2.2. Parser CSV Conforme RFC 4180 avec Support Multi-lignes (`CsvManager.kt`)
* Remplacer la lecture par ligne (`readLines()`) par un algorithme d'analyse globale de fichier basé sur une machine à états (State Machine).
* Le parser gère correctement :
  * Les guillemets ouvrants et fermants (`"`).
  * Les chaînes de caractères sur plusieurs lignes (ex: JSON ou phrases avec `\n` ou `\r\n`).
  * L'échappement des guillemets intérieurs (`""` -> `"`).
  * La séparation par point-virgule (`;`).
* Suppression automatique du BOM `\uFEFF` au début du fichier s'il est présent.

### 2.3. Rétrocompatibilité & Robustesse
* Maintien du support des fichiers à 4, 5 et 9 colonnes.
* Préservation intégrale de la structure des exemples IA, des contextes et des traductions multilingues.
