package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

data class SampleBook(
    val title: String,
    val author: String,
    val chapter: String,
    val pageNumber: Int,
    val paragraphs: List<String>
)

object SampleBookPages {

    val availableSamples = listOf(
        SampleBook(
            title = "Le Petit Prince",
            author = "Antoine de Saint-Exupéry",
            chapter = "Chapitre XXI — Le Renard",
            pageNumber = 67,
            paragraphs = listOf(
                "— C'est une chose trop oubliée, dit le renard. Ça signifie « créer des liens... »",
                "— Créer des liens ?",
                "— Bien sûr, dit le renard. Tu n'es encore pour moi qu'un petit garçon tout semblable à cent mille petits garçons. Et je n'ai pas besoin de toi. Et tu n'as pas besoin de moi non plus.",
                "— Mais, si tu m'apprivoises, nous aurons besoin l'un de l'autre. Tu seras pour moi unique au monde. Je serai pour toi unique au monde...",
                "— Je commence à comprendre, dit le petit prince. Il y a une fleur... je crois qu'elle m'a apprivoisé.",
                "— Ma vie est monotone. Je chasse les poules, les hommes me chassent. Toutes les poules se ressemblent, et tous les hommes se ressemblent. Mais si tu m'apprivoises, ma vie sera comme ensoleillée.",
                "— Voici mon secret. Il est très simple : on ne voit bien qu'avec le cœur. L'essentiel est invisible pour les yeux. Les fleurs sont si éphémères !"
            )
        ),
        SampleBook(
            title = "Les Misérables",
            author = "Victor Hugo",
            chapter = "Livre Cinquième — Une tempête sous un crâne",
            pageNumber = 245,
            paragraphs = listOf(
                "Il y a un spectacle plus grandiose que la mer, c'est le ciel ; il y a un spectacle plus grandiose que le ciel, c'est l'intérieur de l'âme.",
                "Faire le poème de la conscience humaine, ne fût-ce qu'à propos d'un seul homme, ce serait fondre toutes les épopées dans une épopée supérieure et définitive.",
                "Jean Valjean s'était assis, accablé. Il contemplait dans cette ombre silencieuse l'ineffable clarté du devoir.",
                "Son esprit oscillait entre le sacrifice absolu et la préservation de son existence paisible. Une méditation austère s'empara de son être tout entier.",
                "La sérénité d'un homme juste est une forteresse imprenable contre les tourments du monde."
            )
        )
    )

    /**
     * Génère un Bitmap haute résolution représentant une vraie page de livre
     * prête pour l'analyse OCR et le tap-to-select.
     */
    fun renderPageToBitmap(sample: SampleBook = availableSamples[0], width: Int = 1080, height: Int = 1500): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fond couleur papier livre crème / ivoire chaud
        canvas.drawColor(Color.rgb(250, 247, 239))

        // Bordure subtile de page
        val borderPaint = Paint().apply {
            color = Color.rgb(228, 220, 205)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(RectF(30f, 30f, (width - 30).toFloat(), (height - 30).toFloat()), borderPaint)

        // Paint pour l'en-tête (Auteur & Titre)
        val headerPaint = Paint().apply {
            color = Color.rgb(120, 105, 90)
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${sample.author} — ${sample.title}", width / 2f, 90f, headerPaint)

        // Trait séparateur sous l'en-tête
        val dividerPaint = Paint().apply {
            color = Color.rgb(215, 205, 190)
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawLine(100f, 115f, width - 100f, 115f, dividerPaint)

        // Titre de chapitre
        val chapterPaint = Paint().apply {
            color = Color.rgb(60, 50, 40)
            textSize = 36f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(sample.chapter, width / 2f, 185f, chapterPaint)

        // Corps du texte (Serif élégant, noir encre douce)
        val textPaint = Paint().apply {
            color = Color.rgb(35, 30, 25)
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val marginHorizontal = 80f
        val maxTextWidth = width - (marginHorizontal * 2)
        var currentY = 260f
        val lineSpacing = 48f
        val paragraphSpacing = 32f

        for (paragraph in sample.paragraphs) {
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()

            for (w in words) {
                val testLine = if (currentLine.isEmpty()) w else "$currentLine $w"
                val measured = textPaint.measureText(testLine)

                if (measured > maxTextWidth && currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine.toString(), marginHorizontal, currentY, textPaint)
                    currentY += lineSpacing
                    currentLine = StringBuilder(w)
                } else {
                    currentLine = StringBuilder(testLine)
                }
            }

            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), marginHorizontal, currentY, textPaint)
                currentY += lineSpacing
            }

            currentY += paragraphSpacing
            if (currentY > height - 120f) break
        }

        // Numéro de page en bas centré
        val footerPaint = Paint().apply {
            color = Color.rgb(130, 115, 100)
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${sample.pageNumber}", width / 2f, height - 70f, footerPaint)

        return bitmap
    }
}
