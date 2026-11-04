package com.fixtracker.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.PlanStatus
import com.fixtracker.app.data.Vehicle
import com.fixtracker.app.data.buildMaintenancePlan
import com.fixtracker.app.data.calculateFuelStats
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReportPdf {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val RIGHT = PAGE_WIDTH - MARGIN
    private const val CONTENT_BOTTOM = 770f

    private const val FOREST = 0xFF1E4A3D.toInt()
    private const val COPPER = 0xFFB8733D.toInt()
    private const val INK = 0xFF171B1A.toInt()
    private const val MUTED = 0xFF5E6662.toInt()
    private const val BONE = 0xFFF5F3EE.toInt()
    private const val HAIRLINE = 0xFFE3DED4.toInt()
    private const val POSITIVE = 0xFF4E7A5E.toInt()
    private const val DANGER = 0xFFA94B3F.toInt()

    private val sans = Typeface.create("sans-serif", Typeface.NORMAL)
    private val sansBold = Typeface.create("sans-serif", Typeface.BOLD)
    private val serifBold = Typeface.create("serif", Typeface.BOLD)
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.forLanguageTag("es-ES"))

    fun generate(context: Context, state: AppState, vehicle: Vehicle, today: LocalDate = LocalDate.now()): File {
        val document = PdfDocument()
        val reports = File(context.cacheDir, "reports").apply { mkdirs() }
        val safePlate = vehicle.plate.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { vehicle.id.take(8) }
        val file = File(reports, "FixTracker_${safePlate}_${today}.pdf")

        val body = paint(INK, 9f, sans)
        val small = paint(MUTED, 7.5f, sans)
        val smallBold = paint(MUTED, 7.5f, sansBold)
        val white = paint(Color.WHITE, 9f, sans)
        val whiteSmall = paint(Color.WHITE, 7.5f, sans)
        val title = paint(Color.WHITE, 24f, serifBold)
        val section = paint(INK, 15f, serifBold)
        val value = paint(INK, 16f, serifBold)
        val moneyPaint = paint(INK, 10f, sansBold).apply { textAlign = Paint.Align.RIGHT }

        var canvas: Canvas? = null
        var currentPage: PdfDocument.Page? = null
        var pageNumber = 0
        var y = 0f

        fun drawChrome(pageCanvas: Canvas, number: Int) {
            pageCanvas.drawText("FIXTRACKER", MARGIN, 30f, smallBold)
            pageCanvas.drawText("Informe de mantenimiento", RIGHT, 30f, small.apply { textAlign = Paint.Align.RIGHT })
            small.apply { textAlign = Paint.Align.LEFT }
            pageCanvas.drawLine(MARGIN, 38f, RIGHT, 38f, paint(HAIRLINE, 0.8f, sans))
            pageCanvas.drawLine(MARGIN, 804f, RIGHT, 804f, paint(HAIRLINE, 0.8f, sans))
            pageCanvas.drawText(
                "FixTracker · ${formatDate(today)}",
                MARGIN,
                818f,
                small,
            )
            pageCanvas.drawText(
                "Página $number",
                RIGHT,
                818f,
                small.apply { textAlign = Paint.Align.RIGHT },
            )
            small.apply { textAlign = Paint.Align.LEFT }
        }

        fun startPage() {
            pageNumber++
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            currentPage = page
            canvas = page.canvas
            drawChrome(page.canvas, pageNumber)
            y = 58f
        }

        fun ensure(space: Float) {
            if (y + space > CONTENT_BOTTOM) {
                currentPage?.let { document.finishPage(it) }
                startPage()
            }
        }

        fun sectionTitle(text: String) {
            ensure(42f)
            y += 16f
            canvas?.drawText(text, MARGIN, y, section)
            y += 7f
            canvas?.drawLine(MARGIN, y, RIGHT, y, paint(HAIRLINE, 1f, sans))
            y += 15f
        }

        startPage()

        // Hero
        val c = canvas!!
        val hero = RectF(MARGIN, y, RIGHT, y + 112f)
        c.drawRoundRect(hero, 18f, 18f, paint(FOREST, 1f, sans))
        c.drawText("VEHÍCULO", MARGIN + 18f, y + 24f, whiteSmall)
        c.drawText(vehicle.fullName, MARGIN + 18f, y + 55f, title)
        c.drawText(
            listOf(vehicle.plate, vehicle.year.toString(), vehicle.fuel.label)
                .filter { it.isNotBlank() }.joinToString("  ·  "),
            MARGIN + 18f,
            y + 75f,
            whiteSmall,
        )
        c.drawText("KILOMETRAJE", MARGIN + 18f, y + 96f, whiteSmall)
        c.drawText("${formatKm(vehicle.km)} km", MARGIN + 78f, y + 96f, paint(Color.WHITE, 10f, sansBold))
        c.drawText(
            "Servicios: ${state.recordsFor(vehicle.id).size}",
            RIGHT - 18f,
            y + 96f,
            whiteSmall.apply { textAlign = Paint.Align.RIGHT },
        )
        whiteSmall.textAlign = Paint.Align.LEFT
        y += 128f

        // Stats
        val fuel = state.fuelFor(vehicle.id)
        val fuelStats = calculateFuelStats(fuel)
        val stats = listOf(
            "GASTO TOTAL" to formatMoney(state.spendFor(vehicle.id), state.settings.currency),
            "SERVICIOS" to state.recordsFor(vehicle.id).size.toString(),
            "CONSUMO MEDIO" to (fuelStats.averageConsumption?.let { oneDecimal(it) + " L/100" } ?: "—"),
        )
        val gap = 8f
        val boxWidth = (RIGHT - MARGIN - gap * 2) / 3f
        stats.forEachIndexed { index, stat ->
            val left = MARGIN + index * (boxWidth + gap)
            val box = RectF(left, y, left + boxWidth, y + 52f)
            c.drawRoundRect(box, 10f, 10f, paint(BONE, 1f, sans))
            c.drawText(stat.first, left + 10f, y + 17f, small)
            c.drawText(stat.second, left + 10f, y + 38f, value)
        }
        y += 72f

        // Documents
        sectionTitle("Documentación")
        val documents = listOf(
            "ITV" to vehicle.itvUntil,
            "Seguro" to vehicle.insuranceUntil,
            "Impuesto" to vehicle.roadTaxUntil,
        )
        documents.forEach { (name, date) ->
            ensure(24f)
            val status = documentStatus(date, today)
            val cc = canvas!!
            cc.drawText(name, MARGIN, y, body)
            cc.drawText(date?.let { formatDate(it) } ?: "Sin fecha", MARGIN + 92f, y, body)
            cc.drawText(status.first, RIGHT, y, paint(status.second, 8.5f, sansBold).apply { textAlign = Paint.Align.RIGHT })
            y += 20f
        }

        // Plan
        sectionTitle("Plan de mantenimiento")
        val plan = buildMaintenancePlan(state, vehicle, today)
        plan.take(6).forEach { item ->
            ensure(24f)
            val cc = canvas!!
            val status = item.status(vehicle, today)
            cc.drawText(item.title, MARGIN, y, body)
            val due = buildString {
                item.dueDate?.let { append(formatDate(it)) }
                item.dueKm?.let {
                    if (isNotEmpty()) append(" · ")
                    append(formatKm(it)).append(" km")
                }
            }.ifBlank { "Sin registro" }
            cc.drawText(due, MARGIN + 150f, y, small)
            cc.drawText(
                when (status) {
                    PlanStatus.OVERDUE -> "Vencido"
                    PlanStatus.SOON -> "Próximo"
                    PlanStatus.OK -> "Al día"
                    PlanStatus.UNKNOWN -> "—"
                },
                RIGHT,
                y,
                paint(
                    when (status) {
                        PlanStatus.OVERDUE -> DANGER
                        PlanStatus.SOON -> COPPER
                        PlanStatus.OK -> POSITIVE
                        PlanStatus.UNKNOWN -> MUTED
                    },
                    8.5f,
                    sansBold,
                ).apply { textAlign = Paint.Align.RIGHT },
            )
            y += 19f
        }

        // History
        sectionTitle("Historial de servicios")
        fun drawTableHeader() {
            val cc = canvas!!
            cc.drawText("FECHA", MARGIN, y, smallBold)
            cc.drawText("SERVICIO", MARGIN + 54f, y, smallBold)
            cc.drawText("KM", RIGHT - 104f, y, smallBold.apply { textAlign = Paint.Align.RIGHT })
            cc.drawText("COSTE", RIGHT, y, smallBold.apply { textAlign = Paint.Align.RIGHT })
            smallBold.textAlign = Paint.Align.LEFT
            y += 7f
            cc.drawLine(MARGIN, y, RIGHT, y, paint(HAIRLINE, 0.8f, sans))
            y += 13f
        }
        drawTableHeader()

        val records = state.recordsFor(vehicle.id)
        if (records.isEmpty()) {
            ensure(20f)
            canvas!!.drawText("Sin servicios registrados.", MARGIN, y, small)
            y += 18f
        } else {
            records.forEach { record ->
                if (y + 18f > CONTENT_BOTTOM) {
                    currentPage?.let { document.finishPage(it) }
                    startPage()
                    drawTableHeader()
                }
                val cc = canvas!!
                cc.drawText(record.date.format(dateFormat), MARGIN, y, body)
                cc.drawText(fit(record.title, body, 270f), MARGIN + 54f, y, body)
                cc.drawText(formatKm(record.km), RIGHT - 104f, y, paint(MUTED, 8.5f, sans).apply { textAlign = Paint.Align.RIGHT })
                cc.drawText(
                    formatMoney(record.cost, state.settings.currency),
                    RIGHT,
                    y,
                    moneyPaint,
                )
                y += 17f
                cc.drawLine(MARGIN, y - 4f, RIGHT, y - 4f, paint(HAIRLINE, 0.4f, sans))
            }
            y += 6f
        }

        // Fuel
        if (fuel.isNotEmpty()) {
            sectionTitle("Combustible")
            val summary = buildString {
                append("${fuel.size} repostajes")
                fuelStats.averageConsumption?.let { append("  ·  media ${oneDecimal(it)} L/100 km") }
                fuelStats.yearSpend.takeIf { it > 0 }?.let {
                    append("  ·  ${formatMoney(it, state.settings.currency)} este año")
                }
            }
            ensure(24f)
            canvas!!.drawText(summary, MARGIN, y, body)
            y += 20f
            fuel.take(6).forEach { entry ->
                ensure(18f)
                val cc = canvas!!
                cc.drawText(entry.date.format(dateFormat), MARGIN, y, small)
                cc.drawText("${oneDecimal(entry.liters)} L", MARGIN + 76f, y, small)
                cc.drawText(formatKm(entry.km) + " km", MARGIN + 138f, y, small)
                cc.drawText(formatMoney(entry.totalCost, state.settings.currency), RIGHT, y, moneyPaint)
                y += 16f
            }
        }

        currentPage?.let { document.finishPage(it) }
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Compartir informe").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun documentStatus(date: LocalDate?, today: LocalDate): Pair<String, Int> {
        if (date == null) return "Sin fecha" to MUTED
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
        return when {
            days < 0 -> "Vencido" to DANGER
            days <= 30 -> "Próximo" to COPPER
            else -> "Vigente" to POSITIVE
        }
    }

    private fun paint(color: Int, size: Float, typeface: Typeface): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            this.typeface = typeface
        }

    private fun formatMoney(value: Double, currency: String): String = com.fixtracker.app.util.formatMoney(value, currency)

    private fun formatKm(value: Int): String = com.fixtracker.app.util.formatKm(value)

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-ES")))

    private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end).trimEnd() + "…"
    }
}
