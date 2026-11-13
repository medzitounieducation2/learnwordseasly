# SPEC-7 : Traduction par IA (ChatGPT) & Exemples d'Utilisation en Contexte

**Application :** Vocabulaire Lecture (Android Native)  
**Méthodologie :** Spec-First  
**Date :** 17 Octobre 2026  
**Type :** Intégration IA / ChatGPT API & Amélioration Ergonomique  
**Statut :** Spécifié & Implémenté  

---

## 1. Vue d'ensemble & Objectifs

Google ML Kit Translate offre une traduction locale rapide sur l'appareil mais échoue parfois sur des mots complexes, fléchis ou peu fréquents (par exemple, *"recroqueveillé"* du français vers l'arabe ne renvoie aucun résultat).  
De plus, les apprenants de langues ont besoin d'exemples concrets de phrases illustrant l'utilisation du mot en contexte.

**Objectifs principaux :**
1. Permettre l'utilisation de l'API ChatGPT (`gpt-4o-mini`) comme solution de traduction alternative intelligente.
2. Récupérer automatiquement la traduction exacte ainsi que **maximum 2 exemples d'utilisation** (phrases sources avec leurs traductions respectives).
3. Optimiser strictement la consommation de tokens en formulant des requêtes JSON structurées ultra-courtes.
4. Positionner le bouton d'action IA et la carte des exemples d'utilisation **en dessous du champ Contexte**, juste au-dessus des boutons de validation finale ("Annuler" / "Enregistrer").

---

## 2. Spécifications Techniques & API IA

### 2.1. Gestionnaire d'IA (`AiTranslationManager.kt`)
* **Service API :** OpenAI Chat Completions (`https://api.openai.com/v1/chat/completions`)
* **Modèle :** `gpt-4o-mini` (rapide, ultra-précis et économique) avec `response_format = { "type": "json_object" }`
* **Format de Réponse JSON Strict :**
```json
{
  "traduction": "...",
  "exemples": [
    { "text_origine": "...", "traduction": "..." },
    { "text_origine": "...", "traduction": "..." }
  ]
}
```
* **Contrainte Token :** Prompt minimaliste restreint à 2 exemples au maximum.

---

## 3. Spécifications UX / UI & Disposition dans les Dialogues

### 3.1. Structure Générale des Dialogues ("Mot extrait" & "Modifier l'entrée")
La disposition verticale dans la colonne défilable (`verticalScroll`) respecte l'ordre suivant :
1. **Mot & Prononciation**
2. **Choix de la Liste & Langues**
3. **Champ Traduction** (avec bouton "Traduire" local ML Kit)
4. **Champ Contexte**
5. **Section IA (Juste en dessous du Contexte, avant les boutons de validation)** :
   * **Bouton IA** *"Générer exemples & traduction via IA"* (avec l'icône `AutoAwesome`).
   * **Carte d'Exemples IA** (dès réception de la réponse de l'IA), affichant jusqu'à 2 phrases exemples et leurs traductions.
6. **Boutons d'Action Inférieurs** : *"Annuler"* et *"Enregistrer"*.

### 3.2. Comportement des Dialogues
1. Lors du clic sur le bouton IA :
   * Affichage d'un spinner de chargement dans le bouton IA.
   * Mise à jour du champ Traduction avec la traduction de l'IA (si le champ était vide ou incomplet).
   * Affichage de la carte d'exemples directement en dessous du bouton IA.
2. Le conteneur possède une hauteur maximale flexible et défilable (`verticalScroll`) pour accueillir les exemples confortablement.

---

## 4. Sécurité & Modèles de Données
* Clef API stockée dans `AiTranslationManager.kt`.
* Classes de données : `UsageExample(val textOrigine: String, val traduction: String)` et `AiTranslationResult(val traduction: String, val exemples: List<UsageExample>)`.
