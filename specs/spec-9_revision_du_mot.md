# SPEC-9 : Révision d'un Mot (Comptage, Horodatage & Auto-évaluation)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Suivi des Révisions & Auto-évaluation  
**Statut :** Implémenté & Validé

---

## 1. Vue d'ensemble & Objectifs

Actuellement, l'utilisateur révise ses mots de vocabulaire en ouvrant le dialogue de révision par cartes mémo (Flashcards). Lors du clic sur la carte, la traduction est révélée et des flèches permettent de naviguer entre les mots.

Cette spécification constitue la première étape du système de révision intelligente :
1. **Comptage & Horodatage des Révisions** :
   * À chaque consultation de la traduction (face arrière de la carte), le nombre de révisions `nbrRevisions` est incrémenté de 1 et la date `lastRevisionDate` est mise à jour avec le timestamp actuel.
2. **Auto-évaluation (Reconnu / Non-reconnu)** :
   * Sur la page de traduction (face arrière), deux petites icônes/boutons de couleur (sans texte) sont ajoutés :
     * **Vert** (icône de validation) : si l'utilisateur a reconnu la traduction. Incrémente le champ `translationRecognized`.
     * **Rouge** (icône de refus/erreur) : si l'utilisateur n'a pas reconnu la traduction. Incrémente le champ `translationNotRecognized`.
3. **Mise à jour Backup CSV (Export / Import)** :
   * Ajout des 4 nouveaux champs dans le fichier CSV.
   * Structure CSV (9 colonnes) :  
     `mot;traduction;contexte;exemples;nbr_revisions;derniere_revision;traduction_reconnue;traduction_non_reconnue;date_creation`.
   * Maintien de la rétrocompatibilité avec les anciens formats CSV (4 et 5 colonnes).

---

## 2. Spécifications Techniques

### 2.1. Base de Données Room (Version 8)
Ajout des 4 champs suivants dans `VocabularyEntity` :
* `nbrRevisions: Int = 0`
* `lastRevisionDate: Long = 0L`
* `translationRecognized: Int = 0`
* `translationNotRecognized: Int = 0`

Création de la migration `MIGRATION_7_8` dans `AppDatabase.kt` :
```sql
ALTER TABLE vocabulary ADD COLUMN nbrRevisions INTEGER NOT NULL DEFAULT 0;
ALTER TABLE vocabulary ADD COLUMN lastRevisionDate INTEGER NOT NULL DEFAULT 0;
ALTER TABLE vocabulary ADD COLUMN translationRecognized INTEGER NOT NULL DEFAULT 0;
ALTER TABLE vocabulary ADD COLUMN translationNotRecognized INTEGER NOT NULL DEFAULT 0;
```

### 2.2. Dialogue de Révision (`FlashcardsDialog.kt`)
* **Révélation de la traduction** :
  Lors du retournement de la carte vers la face B (traduction), une révision est comptabilisée (`nbrRevisions += 1`, `lastRevisionDate = System.currentTimeMillis()`) et sauvegardée.
* **Boutons Vert / Rouge** :
  Affichés sous la traduction (icônes `Check` et `Close` sans libellé textuel).
  * Clic Vert : Incrémente `translationRecognized += 1` et enregistre le mot.
  * Clic Rouge : Incrémente `translationNotRecognized += 1` et enregistre le mot.
  * **Désactivation après choix** : Dès que l'utilisateur clique sur l'un des deux boutons (Vert ou Rouge) pour la carte courante, les deux boutons passent en état désactivé (`enabled = false` avec transparence visuelle) afin d'éviter les doubles comptages accidentels. Les boutons se réactivent lorsque l'utilisateur passe à une autre carte.

### 2.3. Gestionnaire CSV (`CsvManager.kt`)
* Structure 9 colonnes : `mot;traduction;contexte;exemples;nbr_revisions;derniere_revision;traduction_reconnue;traduction_non_reconnue;date_creation`.
* Maintien du support de lecture pour les fichiers à 4, 5 et 9 colonnes.
