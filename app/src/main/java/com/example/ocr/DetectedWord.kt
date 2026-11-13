package com.example.ocr

import android.graphics.Rect

/**
 * Modèle représentant un mot détecté par ML Kit avec sa boîte englobante
 * et son contexte de phrase.
 */
data class DetectedWord(
    val rawText: String,
    val cleanWord: String,
    val boundingBox: Rect,
    val lineText: String = "",
    val blockIndex: Int = 0
)

data class OcrScanResult(
    val words: List<DetectedWord>,
    val fullText: String,
    val imageWidth: Int,
    val imageHeight: Int
)
