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
    val nextServiceKm: Int? = null,
    val nextServiceDate: LocalDate? = null,
) {
    val fullName: String get() = "$brand $model".trim()

    val serviceKmInterval: Int
        get() = if (type == VehicleType.MOTORCYCLE) 5_000 else 15_000

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
)

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val title: String,
    val type: ServiceType,
    val dueDate: LocalDate? = null,
    val dueKm: Int? = null,
    val note: String = "",
    val done: Boolean = false,
)

data class Settings(
    val currency: String = "EUR",
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

data class AppState(
    val ownerName: String = "",
    val activeVehicleId: String = "",
    val vehicles: List<Vehicle> = emptyList(),
    val records: List<ServiceRecord> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val settings: Settings = Settings(),
) {
    val activeVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == activeVehicleId } ?: vehicles.firstOrNull()

    fun vehicle(id: String): Vehicle? = vehicles.firstOrNull { it.id == id }

    fun recordsFor(vehicleId: String): List<ServiceRecord> =
        records.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }

    fun remindersFor(vehicleId: String): List<Reminder> =
        reminders.filter { it.vehicleId == vehicleId && !it.done }

    val totalSpend: Double get() = records.sumOf { it.cost }

    fun spendFor(vehicleId: String): Double =
        records.filter { it.vehicleId == vehicleId }.sumOf { it.cost }

    fun spendBetween(vehicleId: String, from: LocalDate, to: LocalDate): Double =
        records.filter { it.vehicleId == vehicleId && !it.date.isBefore(from) && it.date.isBefore(to) }
            .sumOf { it.cost }
}
