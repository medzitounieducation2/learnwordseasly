# SPEC-13 : Refactor - Optimisation de l'Espace d'Affichage de la Liste de Vocabulaire

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Refactor UI & Ergonomie Visuelle  
**Statut :** Implémenté & Validé

---

## 1. Description du Problème & Objectifs

Sur les écrans de smartphones de taille moyenne ou petite, la zone d'en-tête (titre, sélecteur de liste, barre de recherche et filtres) occupait une trop grande partie de la hauteur de l'écran.  
Conséquence : un seul mot de vocabulaire était visible à la fois dans la liste, obligeant l'utilisateur à scroller en permanence.

**Objectifs :**
1. **Réduire l'encombrement de l'en-tête** :
   * Regrouper la barre de recherche et les filtres rapides (Tous, Favoris, Récents) de manière compacte.
   * Réduire les paddings et marges inutiles dans la barre supérieure.
2. **Compacter les cartes de vocabulaire (`VocabularyCard`)** :
   * Réduire le padding interne de la carte (de 16dp à 10dp).
   * Réduire l'espacement vertical entre les cartes dans la liste (de 10dp à 6dp).
   * Compacter la mise en page interne (mot, traduction, contexte, exemples IA et boutons).
3. **Résultat attendu** : Afficher au moins **2 à 3 mots complets simultanément sur l'écran** sans avoir à scroller.

---

## 2. Spécifications Techniques

### 2.1. En-tête Compact (`VocabularyListScreen.kt`)
* En-tête principal : paddings réduits à `horizontal = 12.dp, vertical = 6.dp`.
* Titre de la liste et nombre de mots regroupés de façon dense avec les boutons *"Réviser"* et *"Partager"*.
* Barre de recherche compacte intégrée sur une ligne avec filtres ou espaces réduits.
* Réduction de la hauteur des filtres et chips (`FilterChip`).

### 2.2. Cartes de Vocabulaire Compactes (`VocabularyCard`)
* Padding interne des cartes : `10.dp` au lieu de `16.dp`.
* Espacement entre les cartes (`LazyColumn`) : `6.dp` au lieu de `10.dp`.
* Spacers internes réduits à `3.dp` / `4.dp`.
* Blocs de contexte et d'exemples IA compacts.
