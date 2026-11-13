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
     * Extrait la phrase complète de contexte contenant le mot cible,
     * en assemblant les lignes physiques consécutives d'un bloc de texte
     * et en recherchant les limites de la phrase (début et fin de phrase).
     *
     * Permet de résoudre le problème où un mot se trouve au début ou à la fin d'une ligne
     * alors que la phrase s'étend sur la ligne précédente ou suivante.
     */
    fun extractContextSentence(
        lines: List<String>,
        targetLineIndex: Int,
        targetElementText: String
    ): String {
        if (lines.isEmpty() || targetLineIndex !in lines.indices) {
            return targetElementText
        }

        // Assembler le texte du bloc en conservant les décalages d'index par ligne
        val lineStartIndices = IntArray(lines.size)
        val blockBuilder = StringBuilder()

        for (i in lines.indices) {
            lineStartIndices[i] = blockBuilder.length
            val trimmedLine = lines[i].trim()
            blockBuilder.append(trimmedLine)
            if (i < lines.size - 1) {
                blockBuilder.append(" ")
            }
        }

        val blockText = blockBuilder.toString()
        if (blockText.isBlank()) return targetElementText

        val lineText = lines[targetLineIndex]
        val wordOffsetInLine = lineText.indexOf(targetElementText)
        val wordOffsetInBlock = lineStartIndices[targetLineIndex] + if (wordOffsetInLine >= 0) wordOffsetInLine else 0

        // Bornes de ponctuation de fin de phrase
        val sentenceEndChars = setOf('.', '!', '?', '…')
        val closingQuotesAndBrackets = setOf('»', '"', '’', '\'', ')', ']', '}')

        // 1. Recherche du début de la phrase (vers la gauche depuis wordOffsetInBlock)
        var startIdx = 0
        var searchStart = wordOffsetInBlock - 1
        while (searchStart >= 0) {
            val charAt = blockText[searchStart]
            if (charAt in sentenceEndChars) {
                var candidateStart = searchStart + 1
                while (candidateStart < blockText.length) {
                    val nextChar = blockText[candidateStart]
                    if (nextChar in closingQuotesAndBrackets) {
                        candidateStart++
                    } else if (nextChar.isWhitespace() && candidateStart + 1 < blockText.length && blockText[candidateStart + 1] in closingQuotesAndBrackets) {
                        candidateStart++
                    } else if (nextChar.isWhitespace()) {
                        candidateStart++
                    } else {
                        break
                    }
                }
                if (candidateStart <= wordOffsetInBlock) {
                    startIdx = candidateStart
                }
                break
            }
            searchStart--
        }

        // 2. Recherche de la fin de la phrase (vers la droite depuis wordOffsetInBlock)
        var endIdx = blockText.length
        var searchEnd = wordOffsetInBlock
        while (searchEnd < blockText.length) {
            val charAt = blockText[searchEnd]
            if (charAt in sentenceEndChars) {
                var candidateEnd = searchEnd + 1
                while (candidateEnd < blockText.length) {
                    val nextChar = blockText[candidateEnd]
                    if (nextChar in closingQuotesAndBrackets) {
                        candidateEnd++
                    } else if (nextChar.isWhitespace() && candidateEnd + 1 < blockText.length && blockText[candidateEnd + 1] in closingQuotesAndBrackets) {
                        candidateEnd++
                    } else {
                        break
                    }
                }
                endIdx = candidateEnd
                break
            }
            searchEnd++
        }

        val extracted = blockText.substring(startIdx, endIdx).trim()
        val normalized = extracted.replace("\\s+".toRegex(), " ")

        return if (normalized.isNotBlank()) {
            normalized
        } else {
            lines[targetLineIndex].trim()
        }
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
                    val blockLines = block.lines
                    val blockLineTexts = blockLines.map { it.text }

                    for ((lineIdx, line) in blockLines.withIndex()) {
                        for (element in line.elements) {
                            val box = element.boundingBox ?: continue
                            val raw = element.text
                            val clean = sanitizeWord(raw)

                            if (clean.isNotBlank()) {
                                val contextSentence = extractContextSentence(
                                    lines = blockLineTexts,
                                    targetLineIndex = lineIdx,
                                    targetElementText = raw
                                )

                                detectedWords.add(
                                    DetectedWord(
                                        rawText = raw,
                                        cleanWord = clean,
                                        boundingBox = box,
                                        lineText = contextSentence,
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
