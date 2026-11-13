# SPEC-20 : Bugfix Audio - Lecture en Boucle ("Play All") & Écoute Unitaire des Phrases

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Bugfix Audio & Comportement de Lecture  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Actuellement, deux comportements d'écoute audio nécessitent un ajustement :
1. **Lecture "Play All" :** Lorsque la lecture séquentielle se termine sur le dernier élément de la liste, elle s'arrêtait. Elle doit désormais tourner **en boucle infinie** et reprendre automatiquement au premier élément.
2. **Écoute Unitaire :** Lors du clic sur le bouton audio `[ 🎧 countListen ]` d'une carte spécifique, la lecture ne doit lire que cette phrase isolée et **s'arrêter à la fin** sans enchaîner sur la phrase suivante.

---

## 2. Spécifications des Cas d'Usage

### 2.1. Cas d'Usage 1 : Lecture "Play All" en Boucle
* Déclencheur : Clic sur le bouton Play de la barre de contrôle *"Play All"*.
* Comportement :
  * Parcourt les phrases séquentiellement de `0` à `N - 1`.
  * Lorsque l'audio de la dernière phrase `N - 1` se termine, le lecteur reprend automatiquement à l'index `0`.
  * Le compteur d'écoute `countListen` de chaque phrase s'incrémente automatiquement à la fin de sa prononciation.

### 2.2. Cas d'Usage 2 : Écoute Unitaire d'une Phrase
* Déclencheur : Clic sur le bouton `[ 🎧 countListen ]` d'une carte individuelle.
* Comportement :
  * Désactive le mode séquentiel (`isAutoPlaying = false`).
  * Prononce uniquement la phrase sélectionnée.
  * À la fin de la prononciation, incrémente le compteur `countListen` (+1) de la phrase et **s'arrête**.

### 2.3. Cas d'Usage 3 : Replay
* Déclencheur : Clic sur le bouton `Replay` de la barre audio ou ré-appui sur la phrase.
* Comportement :
  * Relance immédiatement la prononciation de la phrase courante (`playbackIndex`).

---

## 3. Matrice de Validation
1. Lancer "Play All" sur une liste courte (ex: 2 phrases) et vérifier que la lecture boucle indéfiniment du dernier au premier élément.
2. Cliquer sur le bouton `🎧` d'une phrase isolée et vérifier que seule cette phrase est lue.
3. Tester le bouton `Replay` pour réécouter la phrase active.
