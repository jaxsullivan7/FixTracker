package com.fixtracker.app.data

import java.time.LocalDate

object SeedData {

    fun create(today: LocalDate = LocalDate.now()): AppState {
        val alfa = Vehicle(
            id = "veh-alfa",
            brand = "Alfa Romeo",
            model = "Giulietta",
            plate = "9284 KTR",
            year = 2018,
            km = 96_420,
            fuel = Fuel.DIESEL,
            type = VehicleType.CAR,
            colorName = "Grigio Silverstone",
            itvUntil = today.plusDays(16),
            insuranceUntil = today.plusDays(47),
            nextServiceKm = 107_000,
            nextServiceDate = today.plusDays(24),
        )
        val vespa = Vehicle(
            id = "veh-vespa",
            brand = "Vespa",
            model = "Primavera 125",
            plate = "4721 LMC",
            year = 2021,
            km = 12_880,
            fuel = Fuel.GASOLINA,
            type = VehicleType.MOTORCYCLE,
            colorName = "Verde Oliva",
            itvUntil = today.plusDays(120),
            insuranceUntil = today.plusDays(210),
            nextServiceKm = 16_900,
            nextServiceDate = today.plusDays(75),
        )

        fun daysAgo(days: Long): LocalDate = today.minusDays(days)

        val records = listOf(
            ServiceRecord(
                id = "rec-alfa-1", vehicleId = alfa.id, type = ServiceType.ACEITE,
                title = "Cambio de aceite y filtro", date = daysAgo(118), km = 92_480,
                cost = 89.90, shop = "Taller Rivas", notes = "Aceite 5W-30 sintético. Filtro de aceite incluido.",
            ),
            ServiceRecord(
                id = "rec-alfa-2", vehicleId = alfa.id, type = ServiceType.FRENOS,
                title = "Pastillas de freno delanteras", date = daysAgo(95), km = 94_100,
                cost = 145.00, shop = "AutoService Centro", notes = "Pastillas Brembo. Discos revisados.",
            ),
            ServiceRecord(
                id = "rec-alfa-3", vehicleId = alfa.id, type = ServiceType.NEUMATICOS,
                title = "Neumáticos delanteros", date = daysAgo(210), km = 88_000,
                cost = 210.00, shop = "Neumáticos Sur", notes = "Michelin Pilot Sport 4, 225/45 R17.",
            ),
            ServiceRecord(
                id = "rec-alfa-4", vehicleId = alfa.id, type = ServiceType.ITV,
                title = "Inspección técnica", date = daysAgo(350), km = 84_500,
                cost = 42.50, shop = "ITV Campo de Gibraltar", notes = "Favorable. Sin defectos.",
            ),
            ServiceRecord(
                id = "rec-alfa-5", vehicleId = alfa.id, type = ServiceType.FILTROS,
                title = "Filtro de aire", date = daysAgo(118), km = 92_480,
                cost = 32.00, shop = "Taller Rivas",
            ),
            ServiceRecord(
                id = "rec-alfa-6", vehicleId = alfa.id, type = ServiceType.REVISION,
                title = "Revisión de 90.000 km", date = daysAgo(300), km = 86_200,
                cost = 240.00, shop = "Taller Rivas", notes = "Revisión completa: niveles, correas y frenos.",
            ),
            ServiceRecord(
                id = "rec-alfa-7", vehicleId = alfa.id, type = ServiceType.SEGURO,
                title = "Seguro anual a todo riesgo", date = daysAgo(320), km = 84_200,
                cost = 380.00, shop = "Mapfre",
            ),
            ServiceRecord(
                id = "rec-alfa-8", vehicleId = alfa.id, type = ServiceType.REPARACION,
                title = "Sustitución de batería", date = daysAgo(60), km = 95_100,
                cost = 135.00, shop = "Taller Rivas", notes = "Batería Varta 70Ah con 3 años de garantía.",
            ),
            ServiceRecord(
                id = "rec-alfa-9", vehicleId = alfa.id, type = ServiceType.OTRO,
                title = "Alineación de dirección", date = daysAgo(150), km = 90_500,
                cost = 45.00, shop = "Neumáticos Sur",
            ),
            ServiceRecord(
                id = "rec-alfa-10", vehicleId = alfa.id, type = ServiceType.REPARACION,
                title = "Escobillas limpiaparabrisas", date = daysAgo(6), km = 96_100,
                cost = 38.90, shop = "Taller Rivas", notes = "Juego delantero Bosch Aerotwin.",
            ),
            ServiceRecord(
                id = "rec-vespa-1", vehicleId = vespa.id, type = ServiceType.ACEITE,
                title = "Cambio de aceite", date = daysAgo(75), km = 11_900,
                cost = 55.00, shop = "Moto Rivas", notes = "Aceite 10W-40 y filtro.",
            ),
            ServiceRecord(
                id = "rec-vespa-2", vehicleId = vespa.id, type = ServiceType.NEUMATICOS,
                title = "Neumático trasero", date = daysAgo(45), km = 12_300,
                cost = 78.00, shop = "Moto Rivas", notes = "Pirelli Angel Scooter 120/70 R12.",
            ),
            ServiceRecord(
                id = "rec-vespa-3", vehicleId = vespa.id, type = ServiceType.ITV,
                title = "Inspección técnica", date = daysAgo(200), km = 10_100,
                cost = 35.00, shop = "ITV Campo de Gibraltar",
            ),
            ServiceRecord(
                id = "rec-vespa-4", vehicleId = vespa.id, type = ServiceType.FILTROS,
                title = "Filtro de aire", date = daysAgo(75), km = 11_900,
                cost = 18.00, shop = "Moto Rivas",
            ),
            ServiceRecord(
                id = "rec-vespa-5", vehicleId = vespa.id, type = ServiceType.REVISION,
                title = "Revisión de 10.000 km", date = daysAgo(260), km = 9_800,
                cost = 95.00, shop = "Moto Rivas",
            ),
        )

        val reminders = listOf(
            Reminder(
                id = "oil-${alfa.id}", vehicleId = alfa.id, title = "Cambio de aceite",
                type = ServiceType.ACEITE, dueDate = today.plusDays(24), dueKm = 107_000,
                note = "Intervalo recomendado: 15.000 km o 1 año.",
            ),
            Reminder(
                id = "itv-${alfa.id}", vehicleId = alfa.id, title = "Inspección ITV",
                type = ServiceType.ITV, dueDate = today.plusDays(16),
                note = "Cita previa en ITV Campo de Gibraltar.",
            ),
            Reminder(
                id = "insurance-${alfa.id}", vehicleId = alfa.id, title = "Renovación de seguro",
                type = ServiceType.SEGURO, dueDate = today.plusDays(47),
                note = "Póliza a todo riesgo con Mapfre.",
            ),
            Reminder(
                id = "tires-${alfa.id}", vehicleId = alfa.id, title = "Neumáticos traseros",
                type = ServiceType.NEUMATICOS, dueKm = 99_000,
                note = "Revisar desgaste en el próximo cambio de aceite.",
            ),
            Reminder(
                id = "oil-${vespa.id}", vehicleId = vespa.id, title = "Cambio de aceite",
                type = ServiceType.ACEITE, dueDate = today.plusDays(75), dueKm = 16_900,
            ),
            Reminder(
                id = "itv-${vespa.id}", vehicleId = vespa.id, title = "Inspección ITV",
                type = ServiceType.ITV, dueDate = today.plusDays(120),
            ),
            Reminder(
                id = "insurance-${vespa.id}", vehicleId = vespa.id, title = "Renovación de seguro",
                type = ServiceType.SEGURO, dueDate = today.plusDays(210),
            ),
        )

        return AppState(
            ownerName = "",
            activeVehicleId = alfa.id,
            vehicles = listOf(alfa, vespa),
            records = records,
            reminders = reminders,
        )
    }
}
