# 📚 Vocabulaire Lecture (Learn Words Easily)

Une application Android native puissante et moderne d'extraction, de traduction, d'organisation et de révision avancée de vocabulaire et d'expressions littéraires extraits directement de pages de livres (physiques ou numériques) via la reconnaissance optique de caractères (OCR).

---

## 🌟 1. Fonctionnalités Principales

### 📸 OCR Studio & Sélection Tactile ("Tap-to-Select")
* **Capture Caméra & Galerie :** Intégration de CameraX pour prendre en photo n'importe quelle page de livre ou charger une image.
* **Détection Optique de Mots (ML Kit) :** Reconnaissance optique du texte à la volée.
* **Extraction du Contexte Multi-lignes (SPEC-16) :** Lorsqu'un utilisateur touche un mot, l'application extrait automatiquement la phrase complète de contexte (y compris si la phrase s'étend sur plusieurs lignes ou le bloc précédent/suivant).
* **Pages Modèles Intégrées :** Possibilité de tester immédiatement l'OCR avec des pages d'exemple pré-chargées (*Le Petit Prince*, etc.).

### 📖 Carnets & Listes de Vocabulaire Personnalisées
* **Organisation par Ouvrage :** Regroupement des mots par listes/livres (ex: *Le Petit Prince*, *Les Misérables*, *Someone Like You*, *Dualed*...).
* **Paires de Langues :** Définition de la langue d'origine et de la langue cible pour chaque liste (Français, Anglais, Espagnol, Arabe, Allemand, Italien...).
* **Sauvegarde & Import/Export CSV (SPEC-5, SPEC-8, SPEC-10) :** Exportation et importation de listes complètes en fichier CSV avec encodage UTF-8 (support de l'Arabe et des caractères spéciaux) et sérialisation JSON des exemples IA multi-lignes.
* **Modification de la Liste d'un Mot (SPEC-15) :** Réassignation facile de la liste rattachée à un mot directement depuis le dialogue de modification.

### 🤖 Traduction IA & Exemples d'Utilisation
* **Traduction Google ML Kit On-Device :** Traduction instantanée hors-ligne.
* **Génération d'Exemples IA via ChatGPT (`gpt-4o-mini`) (SPEC-7) :** Obtention d'une traduction contextuelle ultra-précise et de jusqu'à 2 courtes phrases d'exemples d'utilisation réelles avec leurs traductions.

### 🎧 Révision Avancée & Immersion Contextuelle (SPEC-14, SPEC-17, SPEC-18, SPEC-19, SPEC-20)
* **Onglet Dédié "Révision" (Milieu) :**
  * **Interface Épurée :** Sans champ de recherche ni description de liste.
  * **Sélecteur de Liste Persistant :** Mémorisation automatique de la dernière liste sélectionnée dans SharedPreferences.
  * **Mode "Exemples Seuls" (Immersion) :** Affichage pur des phrases et expressions sans le mot isolé, avec mise en valeur du mot cible en **gras**.
  * **Masquage des Traductions :** Interrupteur pour afficher ou masquer les traductions.
  * **Mode Devinette (Fill-in-the-blank) :** Masquage du mot cible dans la phrase (`[ ... ]`), déclenchable au toucher.
  * **Lecteur Audio Continual "Play All" en Boucle (SPEC-17, SPEC-20) :** Lecture séquentielle infinie en boucle de toutes les phrases d'exemples avec défilement automatique (*Auto-scroll*) et mise en surbrillance visuelle de la phrase active.
  * **Boutons-Compteurs de Révision 2-en-1 (SPEC-18, SPEC-19) :**
    * `[ 🎧 countListen ]` : Bouton cliquable pour écouter la phrase isolée. Incrémente automatiquement le compteur d'écoutes audio à la fin de la prononciation.
    * `[ 👁️ countRead + ]` : Bouton cliquable pour incrémenter manuellement le compteur de lectures visuelles.
  * **Barre de Contrôles Audio Complète :** Play/Pause, Stop, Précédent, Suivant, Replay.

### 🎴 Flashcards & Auto-Évaluation (SPEC-9, SPEC-12)
* Dialogue interactif de cartes mémos avec retournement (Face A / Face B).
* Boutons d'auto-évaluation **Vert** (Reconnu) / **Rouge** (Non-reconnu).
* Suivi des révisions (`nbrRevisions`, `lastRevisionDate`, `translationRecognized`, `translationNotRecognized`).

---

## 🛠️ 2. Frameworks & Technologies

| Domaine | Technologie / Bibliothèque | Usage |
| :--- | :--- | :--- |
| **Langage** | Kotlin 2.x | Développement Android Native |
| **Interface Utilisateur** | Jetpack Compose (BOM 2024.09.00) + Material Design 3 | Composants d'interface réactifs & thématiques |
| **Architecture** | MVVM (Model-View-ViewModel) + StateFlow | Flux de données unidirectionnels & réactifs |
| **Base de Données** | Room Database V9 (`AppDatabase.kt`) | Persistence locale SQLite avec scripts de migration |
| **Reconnaissance Optique** | Google ML Kit Text Recognition (`com.google.mlkit:text-recognition`) | Détection OCR on-device |
| **Traduction** | Google ML Kit Translate + OkHttp3 / OpenAI API (`gpt-4o-mini`) | Traduction hors-ligne & génération d'exemples IA |
| **Caméra** | CameraX (`androidx.camera:camera-view`) | Capture photo dans l'écran OCR Studio |
| **Synthèse Vocale** | Android Native `TextToSpeech` (`TtsManager.kt`) | Prononciation audio dynamique & rappels d'écoutes |
| **Tests** | Robolectric, JUnit 4, AndroidX Test | Tests unitaires & de composants sans appareil physique |

---

## 📑 3. Méthodologie "Spec-First" (Règles pour les Agents IA)

Le projet suit rigoureusement la méthodologie **Spec-First**. Tout développement ou correctif **DOIT** respecter les étapes suivantes :

### 📋 Étape 1 : Rédaction de la Spécification
Avant d'écrire ou de modifier du code, créer un fichier Markdown dans le dossier `/specs` nommé `spec-N_<nom_de_la_fonctionnalite>.md` (où `N` est le numéro d'ordre séquentiel).

Le fichier de spec doit comporter :
1. **En-tête :** Titre, date, type, statut.
2. **Vue d'ensemble & Objectifs :** Description fonctionnelle et besoins métier.
3. **Spécifications UX / Ergonomie :** Maquettes textuelles et comportements attendus.
4. **Spécifications Techniques :** Modifications de base de données (version Room, migrations), états ViewModel, composables Jetpack Compose.
5. **Matrice de Validation :** Cas de tests fonctionnels à vérifier.

### 💻 Étape 2 : Implémentation du Code
* Modifier/créer les fichiers Kotlin selon la spec.
* Utiliser les outils d'édition ciblée (ex: `replace_file_content`).
* Si la base de données Room évolue, incrémenter la version dans `AppDatabase.kt` et rédiger la migration `MIGRATION_A_B`.

### 🧪 Étape 3 : Tests Unitaires & Validation Build
* Rédiger/mettre à jour les tests Robolectric dans `app/src/test/java/com/example/ExampleRobolectricTest.kt`.
* Valider que tous les tests passent : `./gradlew app:testDebugUnitTest`.
* Valider que le build s'assemble sans erreur : `./gradlew app:assembleDebug`.

---

## 📁 4. Structure du Projet

```text
learnwordseasly/
├── README.md                           # Documentation générale du projet
├── build.gradle.kts                    # Configuration Gradle racine
├── settings.gradle.kts
├── specs/                              # Fichiers de spécifications (Spec-First)
│   ├── spec-0_fonctionnalites_de_base.md
│   ├── ...
│   ├── spec-14_page_revision.md
│   ├── spec-15_modifier_liste_du_mot.md
│   ├── spec-16_ameliorer_contexte_mot_extrait.md
│   ├── spec-17_play_all_audio.md
│   ├── spec-18_compter_revisions_exemples.md
│   ├── spec-19_fusion_boutons_compteurs.md
│   └── spec-20_bugfix_play_audios_boucle_et_unite.md
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/example/
        │       ├── MainActivity.kt               # Point d'entrée principal & Navigation Scaffold
        │       ├── ai/
        │       │   └── AiTranslationManager.kt   # Appels API OpenAI (gpt-4o-mini) & parsing JSON
        │       ├── data/
        │       │   ├── AppDatabase.kt            # Room Database V9 & Migrations
        │       │   ├── VocabularyEntity.kt       # Entité Mot de vocabulaire & compteurs
        │       │   ├── VocabularyListEntity.kt   # Entité Liste de vocabulaire
        │       │   ├── VocabularyDao.kt          # Interface d'accès aux données Room
        │       │   ├── VocabularyRepository.kt   # Repository d'abstraction
        │       │   └── CsvManager.kt             # Import/Export CSV UTF-8
        │       ├── ocr/
        │       │   ├── OcrProcessor.kt           # Moteur OCR ML Kit & extraction de contexte multi-lignes
        │       │   ├── DetectedWord.kt           # Modèle de mot détecté
        │       │   └── SampleBookPages.kt        # Rendu de pages d'exemples
        │       ├── translation/
        │       │   └── TranslationManager.kt     # Google ML Kit Translate on-device
        │       └── ui/
        │           ├── MainViewModel.kt          # ViewModel central & gestionnaires d'état
        │           ├── TtsManager.kt             # Synthèse vocale Android TextToSpeech & callbacks
        │           ├── components/               # Composants UI réutilisables (WordSaveDialog, badges...)
        │           └── screens/                  # Écrans principaux (VocabularyListScreen, AdvancedRevisionScreen, OcrInteractiveScreen...)
        └── test/java/com/example/
            └── ExampleRobolectricTest.kt        # Suite de tests unitaires & Robolectric (25 tests)
```

---

## ⚡ 5. Commandes Utiles

* **Lancer les tests unitaires :**
  ```bash
  ./gradlew app:testDebugUnitTest
  ```
* **Assembler l'APK de Debug :**
  ```bash
  ./gradlew app:assembleDebug
  ```
