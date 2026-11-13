# SPEC-19 : Fusion des Boutons d'Action et des Compteurs de Révision (Audio & Lecture)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Refonte Ergonomique & UI/UX  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Pour gagner de l'espace sur les cartes de révision et offrir un design plus fluide et moderne, nous supprimons le bouton d'écoute audio autonome `🔊` séparé.

Le badge du compteur d'écoutes audio devient lui-même le **bouton interactif d'action audio**.

**Résultat attendu :**
Chaque phrase (contexte ou exemple IA) possède exactement **2 boutons/boutons-compteurs interactifs** :
1. **Bouton Audio (`🎧 countListen`) :** Bouton cliquable qui lance l'écoute de la phrase et incrémente automatiquement le compteur à la fin de la synthèse vocale.
2. **Bouton Lecture Visuelle (`👁️ countRead +`) :** Bouton cliquable qui incrémente manuellement le compteur de lecture de la phrase.

---

## 2. Spécifications Ergonomiques & UX

### 2.1. Nouvelle Mise en Page des Cartes (`AdvancedRevisionScreen.kt`)
* Suppression de l'icône `IconButton` `VolumeUp` séparée.
* Présentation sous la forme de 2 boutons-puces compacts (`Surface` cliquables) alignés à droite :

```
┌────────────────────────────────────────────────────────────┐
│ « Sentence text with target word highlighted... »          │
│                                                            │
│                           [ 🎧 countListen ] [ 👁️ countRead + ] │
└────────────────────────────────────────────────────────────┘
```

### 2.2. Comportement des Boutons
1. **Clic sur `[ 🎧 countListen ]` :**
   * Déclenche la lecture audio de la phrase.
   * L'icône passe temporairement de `Headphones` à `VolumeUp` avec un fond surligné pendant la lecture.
   * À la fin de l'audio (`onDone`), le compteur `countListen` est incrémenté de +1 en base de données.
2. **Clic sur `[ 👁️ countRead + ]` :**
   * Incrémente immédiatement le compteur de lecture visuelle `countRead` de +1.

---

## 3. Matrice de Validation
1. Vérifier la présence de seulement 2 boutons d'action sur chaque carte de phrase.
2. Cliquer sur `🎧 X` : vérifier que la synthèse vocale se déclenche et que `X` passe à `X+1` à la fin de la lecture.
3. Cliquer sur `👁️ Y +` : vérifier que `Y` passe immédiatement à `Y+1`.
