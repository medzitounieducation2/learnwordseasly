# SPEC-5 : Exportation et Importation de Listes au Format CSV (Sauvegarde & Import Massif)

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Nouvelle fonctionnalité (Exportation, Importation CSV & Mass Upload)  
**Statut :** Spécifié & En cours d'implémentation  

---

## 1. Vue d'ensemble & Objectifs

L'application stocke les listes et les mots de vocabulaire dans une base de données locale Room. Afin de prémunir l'utilisateur contre les pertes de données (désinstallation, changement d'appareil, suppression accidentelle) et d'offrir la possibilité d'ajouter rapidement des centaines de mots pré-préparés (sur PC ou par partage entre utilisateurs), cette spécification introduit :

1. **L'exportation d'une liste au format CSV (Download) :** Permet de générer un fichier `.csv` contenant les métadonnées de la liste et l'ensemble de ses mots, sauvegardable localement sur le téléphone ou sur un stockage cloud/USB.
2. **L'importation d'un fichier CSV (Upload / Mass Upload) :** Permet d'importer une nouvelle liste complète ou de fusionner des mots dans une liste existante depuis un fichier `.csv`.
3. **Téléchargement d'un modèle CSV (Template) :** Permet de générer un fichier modèle exemple prêt à être complété manuellement sur PC ou smartphone.

---

## 2. Spécifications du Format de Fichier CSV

Le fichier CSV utilise le point-virgule (`;`) comme séparateur de champs pour une compatibilité directe avec Microsoft Excel et les tableurs francophones.

```csv
Nom: Someone Like You
Description: Vocabulaire extrait du livre de Roald Dahl
Langues: en-fr
mot;traduction;contexte;date_creation
yearning;Désir ardent;He stared out the window with an aching yearning.;1700000000000
ineffable;Ineffable;Il contemplait l'ineffable clarté du devoir.;1700000000000
```

### Règles de structure :
* **Ligne 1 :** `Nom: <Nom de la liste>`
* **Ligne 2 :** `Description: <Description de la liste>`
* **Ligne 3 :** `Langues: <langueSource>-<langueCible>` (ex: `fr-en`, `en-fr`, `es-fr`)
* **Ligne 4 :** En-têtes fixes : `mot;traduction;contexte;date_creation`
* **Lignes 5+ :** Données des mots.
* **Absence d'ID :** Les identifiants internes de base de données ne sont pas inclus. La colonne `mot` sert de clé d'unicité naturelle au sein d'une liste.

---

## 3. Spécifications Fonctionnelles & Ergonomie UI

### 3.1. Exportation d'une liste (Download CSV)
* **Emplacement dans l'IHM :** Un bouton avec icône de téléchargement *"Exporter en CSV"* est présent dans le dialogue *"Modifier la liste"*, ainsi qu'un bouton rapide d'exportation dans l'en-tête de la liste active (`VocabularyListScreen`).
* **Comportement :**
  1. L'utilisateur clique sur *"Exporter en CSV"*.
  2. L'explorateur de fichiers natif d'Android (`CreateDocument`) s'ouvre avec un nom de fichier suggéré : `<Nom_de_la_liste>.csv`.
  3. L'utilisateur sélectionne le dossier de destination (Téléchargements, Drive, Carte SD, etc.).
  4. L'application écrit le fichier et affiche un message de confirmation Toast/Snackbar : *"Liste « [Nom] » exportée avec succès !"*.

### 3.2. Importation d'un fichier CSV (Upload)
* **Emplacement dans l'IHM :**
  * Dans le dialogue *"Créer une liste"* : Bouton *"Importer un fichier CSV"*.
  * Dans le dialogue *"Modifier la liste"* : Bouton *"Importer / Fusionner depuis un CSV"*.
* **Comportement :**
  1. L'utilisateur clique sur *"Importer un fichier CSV"*.
  2. L'explorateur de fichiers natif (`OpenDocument` / `GetContent`) s'ouvre pour sélectionner un fichier `.csv`.
  3. L'application lit et valide le fichier.

* **Règles de traitement & Validation lors de l'import :**
  * **Cas 1 : Import depuis "Créer une liste"**
    * Une nouvelle liste est créée avec le nom, la description et le couple de langues définis dans l'en-tête du fichier CSV.
    * Tous les mots valides du fichier sont insérés dans la nouvelle liste.
  * **Cas 2 : Import / Fusion dans une liste existante ("Modifier la liste")**
    * Si un mot du CSV existe déjà strictement dans la liste actuelle (`mot == csvMot`), ses champs `traduction`, `contexte` et `date_creation` sont mis à jour avec les valeurs du fichier.
    * Si le mot n'existe pas, il est ajouté à la liste.
  * **Validation des lignes :**
    * Une ligne est ignorée si le champ `mot` est vide ou absent.
    * Le champ `contexte` et `date_creation` sont optionnels (`contexte` vaut `""` par défaut si absent).
  * **Notification de succès :**
    * Affiche un message Toast/Snackbar résumant l'action : *"Import réussi : X mots ajoutés, Y mots mis à jour !"*.

### 3.3. Modèle Exemple (Template CSV)
* Un bouton *"Modèle CSV exemple"* est fourni dans le dialogue de création/import pour permettre à l'utilisateur de télécharger un fichier modèle propre (`modele_vocabulaire.csv`) pré-rempli avec des exemples de structure.

---

## 4. Spécifications Techniques

### 4.1. Gestionnaire d'Import/Export (`CsvManager`)
* Utilitaire Kotlin singleton `CsvManager` gérant :
  * `exportListToCsv(context, uri, list, words)` : Génération du flux CSV.
  * `importListFromCsv(context, uri)` : Parsing du flux d'entrée CSV en objet `ParsedCsvData(name, description, sourceLang, targetLang, words)`.
  * `generateTemplateCsv(context, uri)` : Génération du fichier exemple.

### 4.2. Contrats Android & Permissions
* Emploi de `ActivityResultContracts.CreateDocument("text/csv")` et `ActivityResultContracts.OpenDocument()`.
* Utilisation du `ContentResolver` pour lire et écrire dans les URIs Android sans nécessiter de permissions de stockage obsolètes (`WRITE_EXTERNAL_STORAGE`).
