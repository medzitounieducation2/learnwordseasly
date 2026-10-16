package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ocr.DetectedWord
import kotlin.math.max
import kotlin.math.min

/**
 * Composant d'affichage interactif de l'image de la page de livre.
 * Calcule avec précision la projection des coordonnées tactiles vers les coordonnées
 * réelles du Bitmap, affiche les boîtes englobantes (BoundingBoxes) et permet le zoom/pan.
 */
@Composable
fun InteractiveOcrCanvas(
    bitmap: Bitmap,
    detectedWords: List<DetectedWord>,
    selectedWord: DetectedWord?,
    showBoundingBoxes: Boolean,
    onTapCoordinates: (imageX: Float, imageY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    // Support de zoom et déplacement (pan & zoom)
    var zoomScale by remember(bitmap) { mutableFloatStateOf(1f) }
    var panOffsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var panOffsetY by remember(bitmap) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .testTag("ocr_interactive_canvas_box")
            .fillMaxSize()
            .pointerInput(bitmap) {
                // Détection de pincement pour zoomer et glisser
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                    if (zoomScale == 1f) {
                        panOffsetX = 0f
                        panOffsetY = 0f
                    } else {
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                }
            }
            .pointerInput(bitmap, zoomScale, panOffsetX, panOffsetY) {
                // Détection du toucher tactile simple pour la sélection de mot
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap pour réinitialiser le zoom
                        zoomScale = if (zoomScale > 1.2f) 1f else 2f
                        panOffsetX = 0f
                        panOffsetY = 0f
                    },
                    onTap = { tapOffset ->
                        val canvasW = size.width.toFloat()
                        val canvasH = size.height.toFloat()
                        val bmpW = bitmap.width.toFloat()
                        val bmpH = bitmap.height.toFloat()

                        if (bmpW <= 0 || bmpH <= 0 || canvasW <= 0 || canvasH <= 0) return@detectTapGestures

                        // Échelle de base "Fit"
                        val baseScale = min(canvasW / bmpW, canvasH / bmpH)
                        val totalScale = baseScale * zoomScale

                        val renderedW = bmpW * totalScale
                        val renderedH = bmpH * totalScale

                        val baseOffsetX = (canvasW - (bmpW * baseScale)) / 2f
                        val baseOffsetY = (canvasH - (bmpH * baseScale)) / 2f

                        // Calcul du décalage avec le zoom centré
                        val currentOffsetX = (canvasW - renderedW) / 2f + panOffsetX
                        val currentOffsetY = (canvasH - renderedH) / 2f + panOffsetY

                        val touchX = tapOffset.x
                        val touchY = tapOffset.y

                        // Vérifier si le tap se situe dans la zone rendue de l'image
                        if (touchX in currentOffsetX..(currentOffsetX + renderedW) &&
                            touchY in currentOffsetY..(currentOffsetY + renderedH)
                        ) {
                            val imgX = (touchX - currentOffsetX) / totalScale
                            val imgY = (touchY - currentOffsetY) / totalScale
                            onTapCoordinates(imgX.coerceIn(0f, bmpW), imgY.coerceIn(0f, bmpH))
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("ocr_canvas")) {
            val canvasW = size.width
            val canvasH = size.height
            val bmpW = bitmap.width.toFloat()
            val bmpH = bitmap.height.toFloat()

            if (bmpW <= 0 || bmpH <= 0 || canvasW <= 0 || canvasH <= 0) return@Canvas

            val baseScale = min(canvasW / bmpW, canvasH / bmpH)
            val totalScale = baseScale * zoomScale

            val renderedW = bmpW * totalScale
            val renderedH = bmpH * totalScale

            val currentOffsetX = (canvasW - renderedW) / 2f + panOffsetX
            val currentOffsetY = (canvasH - renderedH) / 2f + panOffsetY

            // 1. Dessin de l'image Bitmap
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(currentOffsetX.toInt(), currentOffsetY.toInt()),
                dstSize = IntSize(renderedW.toInt(), renderedH.toInt())
            )

            // 2. Dessin des rectangles BoundingBox des mots détectés
            if (showBoundingBoxes) {
                for (word in detectedWords) {
                    val box = word.boundingBox
                    val boxX = currentOffsetX + (box.left * totalScale)
                    val boxY = currentOffsetY + (box.top * totalScale)
                    val boxW = box.width() * totalScale
                    val boxH = box.height() * totalScale

                    // Ne dessiner que si dans l'écran
                    if (boxX + boxW < 0 || boxX > canvasW || boxY + boxH < 0 || boxY > canvasH) continue

                    val isSelected = selectedWord?.rawText == word.rawText &&
                            selectedWord.boundingBox == word.boundingBox

                    if (isSelected) {
                        // Surlignage marqué pour le mot sélectionné (or / ambre chaud)
                        drawRoundRect(
                            color = Color(0x66F59E0B),
                            topLeft = Offset(boxX - 2f, boxY - 2f),
                            size = Size(boxW + 4f, boxH + 4f),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFFD97706),
                            topLeft = Offset(boxX - 2f, boxY - 2f),
                            size = Size(boxW + 4f, boxH + 4f),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    } else {
                        // Boîte délicate pour indiquer les mots cliquables
                        drawRoundRect(
                            color = Color(0x1A2563EB),
                            topLeft = Offset(boxX, boxY),
                            size = Size(boxW, boxH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0x403B82F6),
                            topLeft = Offset(boxX, boxY),
                            size = Size(boxW, boxH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                    }
                }
            } else if (selectedWord != null) {
                // Si les boîtes générales sont masquées, toujours surligner le mot sélectionné
                val box = selectedWord.boundingBox
                val boxX = currentOffsetX + (box.left * totalScale)
                val boxY = currentOffsetY + (box.top * totalScale)
                val boxW = box.width() * totalScale
                val boxH = box.height() * totalScale

                drawRoundRect(
                    color = Color(0x66F59E0B),
                    topLeft = Offset(boxX - 2f, boxY - 2f),
                    size = Size(boxW + 4f, boxH + 4f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(boxX - 2f, boxY - 2f),
                    size = Size(boxW + 4f, boxH + 4f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }
    }
}
