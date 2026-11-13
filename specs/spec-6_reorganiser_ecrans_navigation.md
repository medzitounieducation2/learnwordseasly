# SPEC-6 : Réorganisation de la Navigation, Onglet Par Défaut et Caméra Intégrée

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Refonte Ergonomique & Architecture de Navigation  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

Pour simplifier l'ergonomie de l'application et offrir un accès direct au vocabulaire tout en gardant une capture photo rapide :
1. **Suppression de l'onglet "Scanner" de la barre de navigation inférieure.**
2. **Re-positionnement de l'onglet "Vocabulaire" en première position (complètement à gauche) et définition comme écran d'accueil par défaut au lancement.**
3. **Accès direct à la caméra depuis l'écran "OCR Studio" :**
   * Ajout d'un bouton d'action Caméra ("Scanner une page") directement accessible dans la barre supérieure de l'écran "OCR Studio".
   * Déclenchement de la capture d'image (`CameraCaptureScreen`) sous forme d'overlay/plein écran sans nécessiter d'onglet dédié.

---

## 2. Spécifications Fonctionnelles & UX

### 2.1. Nouvelle Barre de Navigation Inférieure (`NavigationBar`)
* **Onglet 1 (complètement à gauche, par défaut) :** **Vocabulaire** (Icône `MenuBook`)
  * Écran principal affiché au lancement de l'application.
  * Donnes d'accès direct au vocabulaire, à la recherche, au filtrage, aux listes et à la révision par Flashcards.
* **Onglet 2 :** **OCR Studio** (Icône `TouchApp`)
  * Permet la sélection tactile interactive des mots sur la page de livre capturée.
  * Intègre un bouton Caméra pour prendre en photo une nouvelle page à tout moment.

### 2.2. Intégration de la Caméra dans "OCR Studio" (`OcrInteractiveScreen`)
* **Emplacement du bouton Caméra :**
  * Un bouton *"Scanner"* avec icône `CameraAlt` est placé de façon très lisible dans la barre supérieure d'OCR Studio.
  * Dans l'état vide (aucune photo capturée), le bouton central permet d'ouvrir immédiatement la caméra.
* **Comportement de capture :**
  1. Un clic sur *"Scanner"* ou *"Ouvrir la caméra"* affiche la caméra (`CameraCaptureScreen`) en overlay.
  2. L'utilisateur prend en photo la page (ou charge un exemple).
  3. L'overlay se ferme et l'image est instantanément analysée par OCR puis affichée dans le canevas interactif pour toucher les mots.

### 2.3. Gestion du Bouton Retour Physique
* Appuyer sur le bouton de retour physique natif depuis l'onglet "OCR Studio" ramène l'utilisateur vers l'onglet par défaut "Vocabulaire".

---

## 3. Spécifications Techniques

### 3.1. Modification de `AppTab` (`MainViewModel.kt`)
* Suppression de `AppTab.SCANNER`.
* L'enum contient : `VOCABULARY` et `OCR_STUDIO`.
* `_currentTab` est initialisé à `AppTab.VOCABULARY`.

### 3.2. Overlay Caméra dans `OcrInteractiveScreen.kt`
* État Compose `showCameraModal: Boolean` contrôlant l'affichage de `CameraCaptureScreen`.
* Lorsque `onImageCaptured(bitmap)` est invoqué, fermeture de la modale et appel à `viewModel.processImage(bitmap)`.
