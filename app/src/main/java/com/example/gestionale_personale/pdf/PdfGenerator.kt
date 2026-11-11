package com.example.gestionale_personale.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.gestionale_personale.R
import com.example.gestionale_personale.domain.model.DailySummary
import com.example.gestionale_personale.domain.model.MonthlySummary
import com.example.gestionale_personale.domain.usecase.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.format.TextStyle
import java.util.Locale

class PdfGenerator(private val context: Context) {

    // A4 dimensions at 72 PPI (PostScript points)
    private val PAGE_WIDTH = 595
    private val PAGE_HEIGHT = 842

    private val MARGIN = 50f
    private val CONTENT_START_Y = 200f

    // Configurazione pennelli (Paint) per disegnare testo e linee
    private val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
        color = Color.BLACK
    }

    private val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        color = Color.BLACK
    }

    private val textPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 12f
        color = Color.BLACK
    }

    private val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    suspend fun generateDailyReport(summary: DailySummary): File? = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawHeader(canvas, "Report Giornaliero")

            // Titolo Data
            val dayName = summary.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
            val monthName = summary.date.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
            val dateStr = "${dayName.replaceFirstChar { it.uppercase() }}, ${summary.date.dayOfMonth} ${monthName.replaceFirstChar { it.uppercase() }} ${summary.date.year}"

            canvas.drawText("Data: $dateStr", MARGIN, CONTENT_START_Y, titlePaint)

            var currentY = CONTENT_START_Y + 40f

            // Intestazione Tabella
            canvas.drawText("Dipendente", MARGIN, currentY, headerPaint)
            canvas.drawText("Ore Lavorate", PAGE_WIDTH - MARGIN - 100f, currentY, headerPaint)
            canvas.drawLine(MARGIN, currentY + 5f, PAGE_WIDTH - MARGIN, currentY + 5f, linePaint)

            currentY += 25f

            // Righe Tabella
            summary.entries.forEach { entry ->
                canvas.drawText(entry.employeeName, MARGIN, currentY, textPaint)
                canvas.drawText(TimeUtils.formatMinutesToHoursAndMinutes(entry.workedMinutes), PAGE_WIDTH - MARGIN - 100f, currentY, textPaint)

                if (!entry.notes.isNullOrBlank()) {
                    currentY += 15f
                    canvas.drawText("Note: ${entry.notes}", MARGIN + 20f, currentY, textPaint)
                }

                currentY += 20f
                canvas.drawLine(MARGIN, currentY - 10f, PAGE_WIDTH - MARGIN, currentY - 10f, linePaint)
            }

            // Totale Giornaliero
            currentY += 20f
            canvas.drawText("Totale Aziendale:", MARGIN, currentY, headerPaint)
            canvas.drawText(TimeUtils.formatMinutesToHoursAndMinutes(summary.totalMinutes), PAGE_WIDTH - MARGIN - 100f, currentY, headerPaint)

            document.finishPage(page)

            // Salvataggio su disco (Cache dir)
            val file = File(context.cacheDir, "Riepilogo_Giornaliero_${summary.date}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateMonthlyReport(summary: MonthlySummary): File? = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawHeader(canvas, "Report Mensile")

            // Titolo Mese
            val monthName = summary.month.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
            val monthStr = "${monthName.replaceFirstChar { it.uppercase() }} ${summary.month.year}"

            canvas.drawText("Mese di Riferimento: $monthStr", MARGIN, CONTENT_START_Y, titlePaint)

            var currentY = CONTENT_START_Y + 40f

            // Intestazione Tabella
            canvas.drawText("Dipendente", MARGIN, currentY, headerPaint)
            canvas.drawText("Totale Ore Mese", PAGE_WIDTH - MARGIN - 100f, currentY, headerPaint)
            canvas.drawLine(MARGIN, currentY + 5f, PAGE_WIDTH - MARGIN, currentY + 5f, linePaint)

            currentY += 25f

            // Righe Tabella
            summary.employeeSummaries.forEach { entry ->
                canvas.drawText(entry.employeeName, MARGIN, currentY, textPaint)
                canvas.drawText(TimeUtils.formatMinutesToHoursAndMinutes(entry.totalMinutes), PAGE_WIDTH - MARGIN - 100f, currentY, textPaint)

                currentY += 20f
                canvas.drawLine(MARGIN, currentY - 10f, PAGE_WIDTH - MARGIN, currentY - 10f, linePaint)
            }

            // Totale Aziendale Mensile
            currentY += 20f
            canvas.drawText("Totale Aziendale Mese:", MARGIN, currentY, headerPaint)
            canvas.drawText(TimeUtils.formatMinutesToHoursAndMinutes(summary.totalCompanyMinutes), PAGE_WIDTH - MARGIN - 100f, currentY, headerPaint)

            document.finishPage(page)

            // Salvataggio su disco (Cache dir)
            val file = File(context.cacheDir, "Riepilogo_Mensile_${summary.month}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawHeader(canvas: Canvas, subtitle: String) {
        // Disegna il logo aziendale se presente nelle risorse
        try {
            val originalLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            if (originalLogo != null) {
                // Scala il logo mantenendo le proporzioni
                val targetHeight = 80f
                val ratio = originalLogo.width.toFloat() / originalLogo.height.toFloat()
                val targetWidth = targetHeight * ratio

                val scaledLogo = android.graphics.Bitmap.createScaledBitmap(
                    originalLogo,
                    targetWidth.toInt(),
                    targetHeight.toInt(),
                    true
                )

                // Centra il logo in alto
                val logoX = (PAGE_WIDTH - targetWidth) / 2
                canvas.drawBitmap(scaledLogo, logoX, MARGIN, null)
            }
        } catch (e: Exception) {
            // Logo non trovato o errore nel decoding, ignoriamo e disegniamo solo testo
        }

        // Testo Intestazione Sotto il Logo
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("CIOTTI'S WORKERS", PAGE_WIDTH / 2f, MARGIN + 100f, titlePaint)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(subtitle, PAGE_WIDTH / 2f, MARGIN + 120f, textPaint)

        // Reset alignment
        titlePaint.textAlign = Paint.Align.LEFT
        textPaint.textAlign = Paint.Align.LEFT

        canvas.drawLine(MARGIN, MARGIN + 130f, PAGE_WIDTH - MARGIN, MARGIN + 130f, linePaint)
    }
}
