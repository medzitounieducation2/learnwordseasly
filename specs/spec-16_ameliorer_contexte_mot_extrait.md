# SPEC-16 : Amélioration de l'Extraction Automatique du Contexte d'un Mot Extrait

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Amélioration OCR & Extraction du Contexte  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Problématique

Lors de la sélection d'un mot sur une page de livre numérisée dans l'écran *OCR Studio*, l'application extrait automatiquement la phrase de contexte dans laquelle le mot apparaît.

**Problème identifié :**
Jusqu'à présent, le contexte extrait correspondait strictement à la ligne physique unique (`line.text`) renvoyée par le moteur OCR ML Kit.  
Conséquences :
* Si le mot sélectionné se trouve au début d'une nouvelle ligne, la première partie de la phrase située sur la ligne précédente est amputée.
* Si le mot se trouve à la fin d'une ligne, la fin de la phrase située sur la ligne suivante est tronquée.

**Objectif :**
Corriger ce comportement en implémentant un algorithme d'extraction de phrase complète (*Sentence-Level Extraction*) qui agrège les lignes adjacentes du bloc de texte et identifie les limites grammaticales de la phrase (de la majuscule/ponctuation initiale au point/ponctuation finale).

---

## 2. Spécifications Fonctionnelles & Algorithmiques

### 2.1. Extraction Multi-lignes de la Phrase de Contexte (`OcrProcessor.kt`)
Lors de la détection des mots (`processImage`) :
1. Pour chaque bloc de texte (`TextBlock`), regrouper les lignes physiques consécutives.
2. Pour un mot sélectionné sur une ligne donnée :
   * **Recherche vers l'arrière (Début de phrase) :** Parcourir le texte du bloc vers la gauche à partir du mot pour trouver la ponctuation de fin de phrase précédente (`.`, `!`, `?`, `…`). Si aucune ponctuation n'est trouvée, remonter jusqu'au début du bloc de texte.
   * **Recherche vers l'avant (Fin de phrase) :** Parcourir le texte du bloc vers la droite à partir du mot pour trouver la ponctuation de fin de la phrase courante (`.`, `!`, `?`, `…`). Si aucune ponctuation n'est trouvée, poursuivre jusqu'à la fin du bloc de texte.
3. Extraire la sous-chaîne correspondante et nettoyer les espaces/retours à la ligne superflus.
4. Conserver les guillemets et signes de ponctuation encadrant la phrase (ex: `«`, `»`, `"`, `'`).

### 2.2. Scénarios d'Utilisation
* **Scénario A (Mot en début de ligne) :**
  * Ligne 1 : *"C'est dans cette forêt sombre et mystérieuse que le petit garçon découvrit"*
  * Ligne 2 : *"une clé dorée cachée sous des feuilles mortes."*
  * Clic sur le mot *"une"* ou *"clé"* (début de la ligne 2).
  * **Contexte extrait :** *"C'est dans cette forêt sombre et mystérieuse que le petit garçon découvrit une clé dorée cachée sous des feuilles mortes."*
* **Scénario B (Mot en fin de ligne) :**
  * Clic sur le mot *"découvrit"* (fin de la ligne 1).
  * **Contexte extrait :** *"C'est dans cette forêt sombre et mystérieuse que le petit garçon découvrit une clé dorée cachée sous des feuilles mortes."*

---

## 3. Spécifications Techniques

### 3.1. Algorithme `extractContextSentence` (`OcrProcessor.kt`)
```kotlin
fun extractContextSentence(
    lines: List<String>,
    targetLineIndex: Int,
    targetElementText: String
): String
```
* Calcule la position relative du mot dans le texte assemblé du bloc.
* Repère les bornes de début et fin de phrase via des expressions régulières ou recherche d'index de ponctuation.
* Affecte le résultat au champ `lineText` de l'objet `DetectedWord`.

---

## 4. Matrice de Validation
1. Tester la sélection d'un mot situé au début d'une ligne dont la phrase démarre à la ligne précédente.
2. Tester la sélection d'un mot situé à la fin d'une ligne dont la phrase se termine à la ligne suivante.
3. Vérifier la gestion des phrases avec guillemets (`« ... »`) et ponctuation complexe.
4. Exécuter les tests unitaires Robolectric pour valider l'extraction de contexte.
