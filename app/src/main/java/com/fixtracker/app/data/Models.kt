package com.fixtracker.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class ServiceType(val label: String, val shortLabel: String) {
    ACEITE("Cambio de aceite", "Aceite"),
    FILTROS("Filtros", "Filtros"),
    FRENOS("Frenos", "Frenos"),
    NEUMATICOS("Neumáticos", "Neumáticos"),
    REVISION("Revisión general", "Revisión"),
    ITV("Inspección ITV", "ITV"),
    SEGURO("Seguro", "Seguro"),
    REPARACION("Reparación", "Reparación"),
    OTRO("Otro", "Otro"),
}

enum class Fuel(val label: String) {
    DIESEL("Diésel"),
    GASOLINA("Gasolina"),
    HIBRIDO("Híbrido"),
    ELECTRICO("Eléctrico"),
}

enum class VehicleType(val label: String) {
    CAR("Coche"),
    MOTORCYCLE("Moto"),
    OTHER("Otro"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
}

data class Vehicle(
    val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val plate: String,
    val year: Int,
    val km: Int,
    val fuel: Fuel = Fuel.GASOLINA,
    val type: VehicleType = VehicleType.CAR,
    val colorName: String = "",
    val itvUntil: LocalDate? = null,
    val insuranceUntil: LocalDate? = null,
    val roadTaxUntil: LocalDate? = null,
    val nextServiceKm: Int? = null,
    val nextServiceDate: LocalDate? = null,
    val kmUpdatedAt: LocalDate? = null,
) {
    val fullName: String get() = "$brand $model".trim()

    val serviceKmInterval: Int
        get() = if (type == VehicleType.MOTORCYCLE) 5_000 else 15_000

    val serviceMonthsInterval: Int
        get() = 12

    fun daysToService(today: LocalDate = LocalDate.now()): Long? =
        nextServiceDate?.let { ChronoUnit.DAYS.between(today, it) }

    fun kmToService(): Int? = nextServiceKm?.let { (it - km).coerceAtLeast(0) }
}

data class ServiceRecord(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val type: ServiceType,
    val title: String,
    val date: LocalDate,
    val km: Int,
    val cost: Double,
    val shop: String = "",
    val notes: String = "",
    val attachmentNames: List<String> = emptyList(),
)

data class FuelEntry(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val date: LocalDate,
    val km: Int,
    val liters: Double,
    val totalCost: Double,
    val station: String = "",
    val fullTank: Boolean = true,
    val notes: String = "",
) {
    val pricePerLiter: Double get() = if (liters > 0) totalCost / liters else 0.0
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val title: String,
    val type: ServiceType,
    val dueDate: LocalDate? = null,
    val dueKm: Int? = null,
    val note: String = "",
    val done: Boolean = false,
    val snoozedUntil: LocalDate? = null,
) {
    fun isSnoozed(today: LocalDate = LocalDate.now()): Boolean =
        snoozedUntil?.isAfter(today) == true

    fun effectiveDueDate(): LocalDate? = when {
        snoozedUntil != null && (dueDate == null || snoozedUntil.isAfter(dueDate)) -> snoozedUntil
        else -> dueDate
    }
}

data class Settings(
    val currency: String = "EUR",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val biometricLock: Boolean = false,
    val annualBudget: Double? = null,
)

data class AppState(
    val ownerName: String = "",
    val activeVehicleId: String = "",
    val vehicles: List<Vehicle> = emptyList(),
    val records: List<ServiceRecord> = emptyList(),
    val fuelEntries: List<FuelEntry> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val settings: Settings = Settings(),
) {
    val activeVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == activeVehicleId } ?: vehicles.firstOrNull()

    fun vehicle(id: String): Vehicle? = vehicles.firstOrNull { it.id == id }

    fun recordsFor(vehicleId: String): List<ServiceRecord> =
        records.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }

    fun fuelFor(vehicleId: String): List<FuelEntry> =
        fuelEntries.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }

    fun remindersFor(vehicleId: String): List<Reminder> =
        reminders.filter { it.vehicleId == vehicleId && !it.done }

    fun latestRecord(vehicleId: String, type: ServiceType): ServiceRecord? =
        recordsFor(vehicleId).firstOrNull { it.type == type }

    fun serviceSpendFor(vehicleId: String): Double =
        records.filter { it.vehicleId == vehicleId }.sumOf { it.cost }

    fun fuelSpendFor(vehicleId: String): Double =
        fuelEntries.filter { it.vehicleId == vehicleId }.sumOf { it.totalCost }

    fun spendFor(vehicleId: String): Double =
        serviceSpendFor(vehicleId) + fuelSpendFor(vehicleId)

    val totalSpend: Double
        get() = records.sumOf { it.cost } + fuelEntries.sumOf { it.totalCost }

    fun spendBetween(vehicleId: String, from: LocalDate, to: LocalDate): Double {
        val services = records
            .filter { it.vehicleId == vehicleId && !it.date.isBefore(from) && it.date.isBefore(to) }
            .sumOf { it.cost }
        val fuel = fuelEntries
            .filter { it.vehicleId == vehicleId && !it.date.isBefore(from) && it.date.isBefore(to) }
            .sumOf { it.totalCost }
        return services + fuel
    }

    /** Puntos de kilometraje para estimar el uso mensual: servicios, repostajes y km actual. */
    fun mileagePointsFor(vehicleId: String, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Int>> {
        val points = buildList {
            recordsFor(vehicleId).filter { it.km > 0 }.forEach { add(it.date to it.km) }
            fuelFor(vehicleId).filter { it.km > 0 }.forEach { add(it.date to it.km) }
            vehicle(vehicleId)?.let { add((it.kmUpdatedAt ?: today) to it.km) }
        }
        return points
            .filter { !it.first.isAfter(today) }
            .groupBy { it.first }
            .map { (date, values) -> date to values.maxOf { it.second } }
            .sortedBy { it.first }
    }
}
