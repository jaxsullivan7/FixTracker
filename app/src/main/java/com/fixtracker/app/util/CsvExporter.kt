package com.fixtracker.app.util

import com.fixtracker.app.data.AppState
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExporter {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun build(state: AppState, vehicleId: String? = null): String {
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.appendLine("Vehículo;Matrícula;Tipo;Concepto;Fecha;Kilometraje;Coste;Litros;Taller;Notas")

        state.vehicles
            .filter { vehicleId == null || it.id == vehicleId }
            .sortedBy { it.fullName }
            .forEach { vehicle ->
                state.recordsFor(vehicle.id).forEach { record ->
                    builder.appendLine(
                        listOf(
                            vehicle.fullName,
                            vehicle.plate,
                            record.type.label,
                            record.title,
                            record.date.format(dateFormat),
                            record.km.toString(),
                            money(record.cost),
                            "",
                            record.shop,
                            record.notes,
                        ).joinToString(";") { escape(it) }
                    )
                }
                state.fuelFor(vehicle.id).forEach { fuel ->
                    val note = buildString {
                        append(if (fuel.fullTank) "Depósito lleno" else "Repostaje parcial")
                        if (fuel.notes.isNotBlank()) append(". ").append(fuel.notes)
                    }
                    builder.appendLine(
                        listOf(
                            vehicle.fullName,
                            vehicle.plate,
                            "Combustible",
                            "Repostaje",
                            fuel.date.format(dateFormat),
                            fuel.km.toString(),
                            money(fuel.totalCost),
                            liters(fuel.liters),
                            fuel.station,
                            note,
                        ).joinToString(";") { escape(it) }
                    )
                }
            }
        return builder.toString()
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun money(value: Double): String =
        String.format(Locale.US, "%.2f", value).replace('.', ',')

    private fun liters(value: Double): String =
        String.format(Locale.US, "%.2f", value).replace('.', ',')
}
