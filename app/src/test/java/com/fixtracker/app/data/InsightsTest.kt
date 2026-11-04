package com.fixtracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsTest {

    private val today = LocalDate.of(2026, 11, 4)

    private fun vehicle(km: Int = 2_000): Vehicle = Vehicle(
        id = "v1",
        brand = "Alfa Romeo",
        model = "Giulietta",
        plate = "9284 KTR",
        year = 2018,
        km = km,
        kmUpdatedAt = today,
    )

    @Test
    fun `consumption is calculated between full tanks`() {
        val entries = listOf(
            FuelEntry(
                id = "1", vehicleId = "v1", date = today.minusDays(30), km = 1_000,
                liters = 40.0, totalCost = 60.0, fullTank = true,
            ),
            FuelEntry(
                id = "2", vehicleId = "v1", date = today.minusDays(15), km = 1_500,
                liters = 40.0, totalCost = 60.0, fullTank = true,
            ),
            FuelEntry(
                id = "3", vehicleId = "v1", date = today, km = 2_000,
                liters = 40.0, totalCost = 60.0, fullTank = true,
            ),
        )

        val stats = calculateFuelStats(entries, today.year)

        assertNotNull(stats.lastConsumption)
        assertEquals(8.0, stats.lastConsumption!!, 0.001)
        assertEquals(8.0, stats.averageConsumption!!, 0.001)
        assertEquals(0.18, stats.costPerKm!!, 0.001)
    }

    @Test
    fun `partial refuels accumulate until the next full tank`() {
        val entries = listOf(
            FuelEntry(
                id = "1", vehicleId = "v1", date = today.minusDays(30), km = 1_000,
                liters = 40.0, totalCost = 60.0, fullTank = true,
            ),
            FuelEntry(
                id = "2", vehicleId = "v1", date = today.minusDays(20), km = 1_250,
                liters = 5.0, totalCost = 7.5, fullTank = false,
            ),
            FuelEntry(
                id = "3", vehicleId = "v1", date = today, km = 1_500,
                liters = 30.0, totalCost = 45.0, fullTank = true,
            ),
        )

        val stats = calculateFuelStats(entries, today.year)

        assertEquals(7.0, stats.lastConsumption!!, 0.001)
    }

    @Test
    fun `mileage estimate uses recent readings`() {
        val state = AppState(
            activeVehicleId = "v1",
            vehicles = listOf(vehicle(2_000)),
            fuelEntries = listOf(
                FuelEntry(
                    id = "1", vehicleId = "v1", date = today.minusDays(30), km = 1_000,
                    liters = 40.0, totalCost = 60.0,
                ),
            ),
        )

        val mileage = state.mileageStatsFor("v1", today)

        assertNotNull(mileage.kmPerMonth)
        assertTrue(mileage.kmPerMonth!! > 900.0)
        assertTrue(mileage.kmPerMonth!! < 1_100.0)
    }

    @Test
    fun `oil plan is due soon`() {
        val car = vehicle(96_420).copy(
            nextServiceKm = 107_000,
            nextServiceDate = today.plusDays(24),
        )
        val state = AppState(activeVehicleId = car.id, vehicles = listOf(car))

        val plan = buildMaintenancePlan(state, car, today)
        val oil = plan.first { it.type == ServiceType.ACEITE }

        assertEquals(107_000, oil.dueKm)
        assertEquals(PlanStatus.SOON, oil.status(car, today))
        assertEquals(HealthStatus.SOON, state.healthFor(car.id, today))
    }
}
