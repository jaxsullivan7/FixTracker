package com.fixtracker.app.data

import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit

enum class HealthStatus { OK, SOON, OVERDUE }

enum class PlanStatus { OK, SOON, OVERDUE, UNKNOWN }

data class FuelStats(
    val lastConsumption: Double?,
    val averageConsumption: Double?,
    val costPerKm: Double?,
    val averagePricePerLiter: Double?,
    val yearLiters: Double,
    val yearSpend: Double,
    val totalLiters: Double,
    val totalSpend: Double,
    val entriesCount: Int,
)

data class MileageStats(
    val kmPerMonth: Double?,
    val lastReading: LocalDate?,
    val points: List<Pair<LocalDate, Int>>,
)

data class PlanItem(
    val type: ServiceType,
    val title: String,
    val dueDate: LocalDate? = null,
    val dueKm: Int? = null,
    val lastDate: LocalDate? = null,
    val lastKm: Int? = null,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
) {
    fun kmLeft(vehicle: Vehicle): Int? = dueKm?.let { (it - vehicle.km).coerceAtLeast(0) }

    fun daysLeft(today: LocalDate = LocalDate.now()): Long? =
        dueDate?.let { ChronoUnit.DAYS.between(today, it) }

    fun status(vehicle: Vehicle, today: LocalDate = LocalDate.now()): PlanStatus {
        if (dueDate == null && dueKm == null) return PlanStatus.UNKNOWN
        val days = daysLeft(today)
        val km = kmLeft(vehicle)
        return when {
            (days != null && days < 0) || (km != null && dueKm != null && vehicle.km >= dueKm) ->
                PlanStatus.OVERDUE
            (days != null && days <= 30) || (km != null && km <= 1_500) -> PlanStatus.SOON
            else -> PlanStatus.OK
        }
    }

    fun progress(vehicle: Vehicle, today: LocalDate = LocalDate.now()): Float {
        val byKm = if (intervalKm != null && lastKm != null) {
            ((vehicle.km - lastKm).toFloat() / intervalKm).coerceIn(0f, 1f)
        } else null
        if (byKm != null) return byKm

        val due = dueDate ?: return 0f
        val base = lastDate ?: due.minusMonths(intervalMonths?.toLong() ?: 12)
        val total = ChronoUnit.DAYS.between(base, due).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(base, today)
        return (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }
}

fun AppState.healthFor(vehicleId: String, today: LocalDate = LocalDate.now()): HealthStatus {
    val vehicle = vehicle(vehicleId) ?: return HealthStatus.OK
    var overdue = false
    var soon = false

    fun evaluate(date: LocalDate?, kmDue: Int? = null) {
        if (date != null) {
            val days = ChronoUnit.DAYS.between(today, date)
            if (days < 0) overdue = true else if (days <= 30) soon = true
        }
        if (kmDue != null) {
            val kmLeft = kmDue - vehicle.km
            if (kmLeft <= 0) overdue = true else if (kmLeft <= 1_500) soon = true
        }
    }

    remindersFor(vehicle.id).forEach { reminder ->
        if (reminder.isSnoozed(today)) return@forEach
        evaluate(reminder.effectiveDueDate(), reminder.dueKm)
    }
    evaluate(vehicle.itvUntil)
    evaluate(vehicle.insuranceUntil)
    evaluate(vehicle.roadTaxUntil)
    evaluate(vehicle.nextServiceDate, vehicle.nextServiceKm)

    return when {
        overdue -> HealthStatus.OVERDUE
        soon -> HealthStatus.SOON
        else -> HealthStatus.OK
    }
}

fun calculateFuelStats(entries: List<FuelEntry>, year: Int = Year.now().value): FuelStats {
    val sorted = entries.filter { it.liters > 0 && it.km > 0 }.sortedWith(compareBy({ it.km }, { it.date }))
    val consumptions = mutableListOf<Double>()
    var lastFull: FuelEntry? = null
    var litersSinceLastFull = 0.0

    sorted.forEach { entry ->
        litersSinceLastFull += entry.liters
        if (entry.fullTank) {
            val previous = lastFull
            if (previous != null) {
                val distance = entry.km - previous.km
                if (distance > 0) {
                    consumptions += litersSinceLastFull / distance * 100.0
                }
            }
            lastFull = entry
            litersSinceLastFull = 0.0
        }
    }

    val totalLiters = sorted.sumOf { it.liters }
    val totalSpend = sorted.sumOf { it.totalCost }
    val totalDistance = if (sorted.size >= 2) sorted.last().km - sorted.first().km else 0
    val yearEntries = sorted.filter { it.date.year == year }

    return FuelStats(
        lastConsumption = consumptions.lastOrNull(),
        averageConsumption = consumptions.takeIf { it.isNotEmpty() }?.average(),
        costPerKm = if (totalDistance > 0) totalSpend / totalDistance else null,
        averagePricePerLiter = if (totalLiters > 0) totalSpend / totalLiters else null,
        yearLiters = yearEntries.sumOf { it.liters },
        yearSpend = yearEntries.sumOf { it.totalCost },
        totalLiters = totalLiters,
        totalSpend = totalSpend,
        entriesCount = sorted.size,
    )
}

fun AppState.mileageStatsFor(vehicleId: String, today: LocalDate = LocalDate.now()): MileageStats {
    val points = mileagePointsFor(vehicleId, today)
    val recent = points.filter { ChronoUnit.DAYS.between(it.first, today) <= 365 }
    val spanDays = recent.firstOrNull()?.let { first ->
        recent.lastOrNull()?.let { last -> ChronoUnit.DAYS.between(first.first, last.first) }
    } ?: 0L
    val kmPerMonth = if (recent.size >= 2 && spanDays >= 14) {
        val distance = (recent.last().second - recent.first().second).toDouble()
        if (distance > 0) distance / spanDays.toDouble() * 30.4375 else null
    } else null
    return MileageStats(
        kmPerMonth = kmPerMonth,
        lastReading = points.lastOrNull()?.first,
        points = points,
    )
}

fun buildMaintenancePlan(
    state: AppState,
    vehicle: Vehicle,
    today: LocalDate = LocalDate.now(),
): List<PlanItem> {
    val car = vehicle.type != VehicleType.MOTORCYCLE
    val oilKm = vehicle.serviceKmInterval
    val oilMonths = 12
    val filtersKm = if (car) 30_000 else 10_000
    val filtersMonths = 24
    val brakesKm = if (car) 40_000 else 15_000
    val brakesMonths = 24
    val tiresKm = if (car) 50_000 else 15_000
    val tiresMonths = 48
    val revisionKm = if (car) 15_000 else 5_000
    val revisionMonths = 12

    val oil = state.latestRecord(vehicle.id, ServiceType.ACEITE)
    val filters = state.latestRecord(vehicle.id, ServiceType.FILTROS)
    val brakes = state.latestRecord(vehicle.id, ServiceType.FRENOS)
    val tires = state.latestRecord(vehicle.id, ServiceType.NEUMATICOS)
    val revision = state.latestRecord(vehicle.id, ServiceType.REVISION)

    fun fromRecord(
        type: ServiceType,
        title: String,
        record: ServiceRecord?,
        km: Int,
        months: Int,
    ): PlanItem = PlanItem(
        type = type,
        title = title,
        dueDate = record?.date?.plusMonths(months.toLong()),
        dueKm = record?.let { it.km + km },
        lastDate = record?.date,
        lastKm = record?.km,
        intervalKm = km,
        intervalMonths = months,
    )

    return listOf(
        PlanItem(
            type = ServiceType.ACEITE,
            title = "Cambio de aceite",
            dueDate = vehicle.nextServiceDate,
            dueKm = vehicle.nextServiceKm,
            lastDate = oil?.date,
            lastKm = oil?.km,
            intervalKm = oilKm,
            intervalMonths = oilMonths,
        ),
        fromRecord(ServiceType.FILTROS, "Filtro de aire", filters, filtersKm, filtersMonths),
        fromRecord(ServiceType.FRENOS, "Frenos", brakes, brakesKm, brakesMonths),
        fromRecord(ServiceType.NEUMATICOS, "Neumáticos", tires, tiresKm, tiresMonths),
        fromRecord(ServiceType.REVISION, "Revisión general", revision, revisionKm, revisionMonths),
        PlanItem(
            type = ServiceType.ITV,
            title = "Inspección ITV",
            dueDate = vehicle.itvUntil,
            lastDate = state.latestRecord(vehicle.id, ServiceType.ITV)?.date,
            intervalMonths = 12,
        ),
        PlanItem(
            type = ServiceType.SEGURO,
            title = "Seguro",
            dueDate = vehicle.insuranceUntil,
            lastDate = state.latestRecord(vehicle.id, ServiceType.SEGURO)?.date,
            intervalMonths = 12,
        ),
        PlanItem(
            type = ServiceType.OTRO,
            title = "Impuesto de circulación",
            dueDate = vehicle.roadTaxUntil,
            intervalMonths = 12,
        ),
    ).sortedWith(
        compareBy<PlanItem> { item ->
            when (item.status(vehicle, today)) {
                PlanStatus.OVERDUE -> 0
                PlanStatus.SOON -> 1
                PlanStatus.OK -> 2
                PlanStatus.UNKNOWN -> 3
            }
        }.thenBy { it.daysLeft(today) ?: Long.MAX_VALUE }
    )
}

fun AppState.spendByTypeFor(vehicleId: String, year: Int): Map<ServiceType, Double> {
    val from = LocalDate.of(year, 1, 1)
    val to = from.plusYears(1)
    return records
        .filter { it.vehicleId == vehicleId && !it.date.isBefore(from) && it.date.isBefore(to) }
        .groupBy { it.type }
        .mapValues { (_, list) -> list.sumOf { it.cost } }
}
