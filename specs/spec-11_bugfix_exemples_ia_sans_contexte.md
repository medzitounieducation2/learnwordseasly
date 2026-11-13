# SPEC-11 : Bugfix - Génération d'Exemples IA Originaux (Exclusion du Contexte)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Bugfix & Optimisation des Prompts IA  
**Statut :** Implémenté & Validé

---

## 1. Description du Problème & Objectif

Lorsqu'un utilisateur demande des exemples d'utilisation via l'IA (ChatGPT `gpt-4o-mini`), la requête incluait jusqu'à présent la phrase de contexte d'origine extraite lors de la lecture.  
Conséquence observée : comme l'IA recevait la phrase du livre, elle la répétait ou la paraphrasait au lieu de fournir des exemples généraux d'utilisation du mot.

**Objectif** : Retirer la phrase de contexte de la requête envoyée à l'IA. Sans le contexte du livre, l'IA fournit naturellement deux exemples d'utilisation simples du mot avec leurs traductions.

---

## 2. Spécifications Techniques

### 2.1. Modification du Prompt OpenAI (`AiTranslationManager.kt`)
* Retrait de la phrase de contexte (`contextSentence`) dans le prompt envoyé à l'API ChatGPT.
* La requête transmise à l'IA ne contient plus que :
  * Le mot / l'expression à traduire.
  * La langue source et la langue cible.
  * L'instruction de fournir au maximum 2 courtes phrases d'exemples d'utilisation avec leurs traductions.

### 2.2. Signature et Rétrocompatibilité
* Le paramètre `contextSentence: String = ""` est conservé dans la signature de `translateAndGetExamples` pour des raisons de rétrocompatibilité d'appel, mais il n'est plus transmis dans le prompt OpenAI.
