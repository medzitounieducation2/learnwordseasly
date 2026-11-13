# SPEC-0 : Spécifications Fonctionnelles et Techniques de Base

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Version :** 1.0 (Socle Fondamental)  
**Statut :** Implémenté & Validé  

---

## 1. Vue d'ensemble du Produit

*Vocabulaire Lecture* est une application mobile Android native conçue comme un compagnon de lecture littéraire. Elle permet au lecteur de capturer en photo une page d'un ouvrage physique, d'isoler instantanément n'importe quel mot d'intérêt par simple toucher tactile via une reconnaissance optique de caractères (OCR) 100 % locale, et d'enregistrer ce mot enrichi de sa définition et de sa phrase de contexte dans des listes personnalisées par livre (ex. *« Rome tome 1 »*, *« Ma voisine me plaît »*). L'application propose également un mode de mémorisation par cartes mémo (*flashcards*) ciblées par liste.

---

## 2. Architecture Technique et Choix Technologiques

* **Système d'exploitation cible :** Android (SDK min 24, compilation SDK 36).
* **Langage de programmation :** Kotlin (100 % Kotlin idiomatique, Coroutines & Flow).
* **Interface Utilisateur :** Jetpack Compose avec Material Design 3 (thème littéraire Scholastic : bleu nuit, ambre, papier ivoire/parchemin).
* **Architecture applicative :** MVVM (Model-View-ViewModel) + Repository Pattern + Clean Data Layer.
* **Gestion Caméra :** Jetpack CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-core`).
* **Reconnaissance de texte (OCR locale) :** Google ML Kit Text Recognition v2 (`com.google.mlkit:text-recognition:16.0.1`), exécution on-device sans dépendance réseau.
* **Base de données locale :** Room Database (SQLite) avec KSP, requêtes réactives sous forme de `Flow<List<T>>` et opérations asynchrones `suspend`.
* **Synthèse Vocale (Audio) :** Android `TextToSpeech` natif (prononciation française / multilingue).

---

## 3. Schéma des Données (Room Database)

### 3.1. Entité `VocabularyListEntity` (`vocabulary_lists`)
Représente une liste ou un carnet de lecture dédié à un ouvrage précis.
* `id` : Long (Clé primaire auto-générée).
* `name` : String (Nom de la liste, ex. *"Rome tome 1"*, *"Ma voisine me plaît"*).
* `description` : String (Description ou sous-titre de la liste, optionnel).
* `createdAt` : Long (Horodatage de création en millisecondes).

### 3.2. Entité `VocabularyEntity` (`vocabulary`)
Représente un mot de vocabulaire extrait d'une lecture.
* `id` : Long (Clé primaire auto-générée).
* `listId` : Long (Clé étrangère logique liant le mot à sa `VocabularyListEntity`).
* `listName` : String (Nom dénormalisé de la liste pour affichage optimisé).
* `word` : String (Le mot extrait et nettoyé, obligatoire).
* `definition` : String (Définition ou note personnelle saisie par l'utilisateur).
* `contextSentence` : String (Phrase ou fragment de texte entourant le mot sur la page).
* `bookTitle` : String (Titre de l'ouvrage ou de l'auteur, optionnel).
* `timestamp` : Long (Date et heure d'enregistrement).
* `isFavorite` : Boolean (Indicateur coup de cœur / mot favori).

---

## 4. Parcours Utilisateur et Écrans

### 4.1. Écran 1 : Prise de Vue & Importation (`CameraCaptureScreen`)
* **Flux caméra temps réel :** Aperçu fluide en plein écran via CameraX `PreviewView` avec cadre guide de lecture.
* **Déclencheur de capture :** Prise de photo haute résolution, extraction du Bitmap en mémoire avec redressement automatique selon la rotation du capteur (`imageInfo.rotationDegrees`).
* **Bascule d'objectif :** Alternance caméra arrière / caméra avant.
* **Import Galerie :** Intégration du sélecteur sécurisé Android (`ActivityResultContracts.PickVisualMedia`) pour charger une photo existante de page de livre.
* **Générateur de pages de test (Fallback Émulateur) :** Génération procédurale haute résolution de pages littéraires réelles (*Le Petit Prince*, *Les Misérables*) avec typo serif et mise en page soignée, assurant un test fonctionnel immédiat sans caméra physique.
* **Gestion des permissions :** Demande dynamique de `Manifest.permission.CAMERA` avec écran explicatif et repli gracieux.

### 4.2. Écran 2 : Sélection Tactile & Extraction OCR (`OcrInteractiveScreen`)
* **Traitement OCR en arrière-plan :** Analyse locale via ML Kit au chargement du Bitmap, décomposition hiérarchique : Blocs $\rightarrow$ Lignes $\rightarrow$ Mots avec leurs rectangles de délimitation (`BoundingBox`).
* **Algorithme de projection des coordonnées tactiles :**
  * Calcul bidirectionnel prenant en compte le redimensionnement (`ContentScale.Fit`), le zoom et le panoramique (`panOffsetX`, `panOffsetY`) :
    $$\text{scale} = \min\left(\frac{W_{\text{canvas}}}{W_{\text{bitmap}}}, \frac{H_{\text{canvas}}}{H_{\text{bitmap}}}\right) \times \text{zoom}$$
    $$X_{\text{image}} = \frac{X_{\text{touch}} - \text{offset}_X}{\text{scale}}, \quad Y_{\text{image}} = \frac{Y_{\text{touch}} - \text{offset}_Y}{\text{scale}}$$
  * Détection exacte dans la `BoundingBox` avec zone de tolérance tactile supplémentaire (rayon de 28 px) pour faciliter la sélection des mots fins sur écran mobile.
* **Surlignage visuel interactif :**
  * Affichage commutable des contours de tous les mots détectés.
  * Surlignage ambré contrasté et animé sur le mot ciblé.
* **Nettoyage automatique du mot extrait :**
  * Élimination des signes de ponctuation parasites de début et fin (`.,;:!?«»"()[]—`).
  * Détection et suppression des élisions françaises courantes (*l'*, *d'*, *c'*, *j'*, *qu'*, etc. : *"l'éphémère"* $\rightarrow$ *"éphémère"*).
* **Affichage alternatif en tiroir :** Liste horizontale/verticale défilante de tous les mots uniques identifiés sur la page pour sélection rapide par puce.

### 4.3. Boîte de Dialogue d'Enregistrement (`WordSaveDialog`)
* **Sélecteur de liste / carnet :** Menu déroulant listant tous les ouvrages créés, avec mémorisation de la dernière liste active pour les ajouts consécutifs.
* **Création de liste à la volée :** Option directe `+ Créer une nouvelle liste...` au sein même du dialogue (création immédiate sans rompre le flux d'extraction).
* **Mot à enregistrer :** Champ de texte pré-rempli, entièrement éditable.
* **Prononciation Audio :** Bouton haut-parleur déclenchant la synthèse vocale Text-to-Speech (TTS) française.
* **Contexte dans l'ouvrage (Édition Inline sans bouton) :**
  * Affichage de la phrase environnante capturée.
  * **Comportement interactif :** Un simple toucher sur le texte le bascule en mode éditeur pour corriger d'éventuelles coquilles d'espacement de l'OCR (*ex: "payeurdu" $\rightarrow$ "payeur du"*).
  * **Fermeture sans bouton :** Un simple clic à l'extérieur (perte de focus) ou validation clavier valide la correction et rétablit l'affichage texte élégant entre guillemets.
* **Définition & Puces sémantiques :** Champ multi-lignes pour la définition personnelle avec suggestions rapides de tags (*[Nom]*, *[Verbe]*, *[Adjectif]*, *[Figure de style]*).
* **Titre de l'ouvrage :** Champ texte optionnel (pré-rempli selon la page scannée).
* **Bouton d'enregistrement :** Libellé sobre et direct : **« Enregistrer »**.

### 4.4. Écran 3 : Gestionnaire de Vocabulaire & Révision (`VocabularyListScreen`)
* **Barre de sélection des listes :** Carrousel horizontal avec puces :
  * *Toutes* (affiche tous les mots mémorisés).
  * Liste individuelle (*Rome tome 1*, *Ma voisine me plaît*, etc.) avec indicateur numérique du nombre de mots.
  * Puce d'ajout *« Nouvelle »* ouvrant la modale de création d'une liste.
* **Administration de la liste sélectionnée :** Bandeau dédié permettant de renommer la liste, modifier sa description ou la supprimer avec confirmation de sécurité.
* **Recherche temps réel :** Filtrage instantané insusceptible à la casse sur le mot, la définition, le titre du livre ou la phrase de contexte.
* **Filtres secondaires :** *Tous*, *Favoris* (étoile), *Récents* (dernières 24h).
* **Cartes de vocabulaire :**
  * Typographie littéraire mise en avant.
  * Badge indiquant le nom de l'ouvrage (si vue globale active).
  * Bouton d'écoute audio TTS.
  * Bouton de mise en favori instantanée.
  * Affichage en italique de la phrase de contexte corrigée.
  * Date d'enregistrement formatée en français.
  * Bouton de modification de la fiche (mot, contexte, définition, ouvrage).
  * Bouton de suppression avec boîte de confirmation.
* **Mode Révision Flashcards (`FlashcardsDialog`) :**
  * Révision ciblée sur les mots de la liste actuellement filtrée.
  * Cartes 3D retournables avec animation de rotation à 180°.
  * Recto : Mot + Bouton audio TTS + Citation en contexte.
  * Verso : Définition complète + Ouvrage source.
  * Navigation suivante/précédente avec compteur de progression.
* **Exportation / Partage :** Génération et partage d'un carnet textuel formaté via l'Android Sharesheet (`Intent.ACTION_SEND`).

---

## 5. Exigences d'Accessibilité et Design

* Conforme aux directives **Material Design 3 (M3)**.
* Dimensions des cibles tactiles interactives supérieures ou égales à **48 dp x 48 dp**.
* Prise en charge native du mode Edge-to-Edge (`enableEdgeToEdge()`).
* Présence systématique de `Modifier.testTag(...)` sur tous les éléments d'interaction clés pour l'automatisation des tests.
* Tests unitaires locaux JVM via **Robolectric** couvrant les calculs de projection des coordonnées, la DAO Room et le nettoyage des mots.
