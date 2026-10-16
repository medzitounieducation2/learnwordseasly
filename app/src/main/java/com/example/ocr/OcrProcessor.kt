package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.hypot

object OcrProcessor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Nettoie un mot brut extrait par l'OCR :
     * retire la ponctuation en bordure (virgules, points, guillemets, parenthèses),
     * gère les élisions françaises (ex: "l'éphémère" -> "éphémère", "d'ailleurs" -> "ailleurs").
     */
    fun sanitizeWord(raw: String): String {
        // Enlève ponctuation de début et de fin
        var cleaned = raw.trim()
            .trimStart { it in ".,;:!?\"«»'’()[]{}—–*…#§" }
            .trimEnd { it in ".,;:!?\"«»'’()[]{}—–*…#§" }

        // Gérer les contractions françaises courantes au début: l', d', c', j', m', t', s', n', qu'
        val lower = cleaned.lowercase()
        val frenchElisions = listOf("l'", "d'", "c'", "j'", "m'", "t'", "s'", "n'", "l’", "d’", "c’", "j’", "m’", "t’", "s’", "n’", "qu'", "qu’")
        for (elision in frenchElisions) {
            if (lower.startsWith(elision) && cleaned.length > elision.length) {
                cleaned = cleaned.substring(elision.length)
                break
            }
        }

        return cleaned.trim()
    }

    /**
     * Analyse un Bitmap avec ML Kit Text Recognition et retourne
     * la liste ordonnée des mots avec leurs coordonnées BoundingBox.
     */
    suspend fun processImage(bitmap: Bitmap): OcrScanResult = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val detectedWords = mutableListOf<DetectedWord>()
                var blockIdx = 0

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val lineStr = line.text
                        for (element in line.elements) {
                            val box = element.boundingBox ?: continue
                            val raw = element.text
                            val clean = sanitizeWord(raw)

                            if (clean.isNotBlank()) {
                                detectedWords.add(
                                    DetectedWord(
                                        rawText = raw,
                                        cleanWord = clean,
                                        boundingBox = box,
                                        lineText = lineStr,
                                        blockIndex = blockIdx
                                    )
                                )
                            }
                        }
                    }
                    blockIdx++
                }

                continuation.resume(
                    OcrScanResult(
                        words = detectedWords,
                        fullText = visionText.text,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height
                    )
                )
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    /**
     * Recherche le mot situé aux coordonnées (imageX, imageY) de l'image.
     * Si aucun rectangle n'englobe strictement le point, recherche le mot le plus
     * proche dans un rayon de tolérance de [tolerancePixels].
     */
    fun findWordAtCoordinates(
        imageX: Float,
        imageY: Float,
        words: List<DetectedWord>,
        tolerancePixels: Float = 28f
    ): DetectedWord? {
        val x = imageX.toInt()
        val y = imageY.toInt()

        // 1. Correspondance exacte dans la BoundingBox
        val directMatch = words.firstOrNull { it.boundingBox.contains(x, y) }
        if (directMatch != null) return directMatch

        // 2. Recherche avec tolérance (marge d'erreur au toucher tactile)
        var closestWord: DetectedWord? = null
        var minDistance = Float.MAX_VALUE

        for (word in words) {
            val rect = word.boundingBox
            // Distance du point au rectangle
            val dx = when {
                imageX < rect.left -> rect.left - imageX
                imageX > rect.right -> imageX - rect.right
                else -> 0f
            }
            val dy = when {
                imageY < rect.top -> rect.top - imageY
                imageY > rect.bottom -> imageY - rect.bottom
                else -> 0f
            }
            val dist = hypot(dx, dy)

            if (dist <= tolerancePixels && dist < minDistance) {
                minDistance = dist
                closestWord = word
            }
        }

        return closestWord
    }
}
