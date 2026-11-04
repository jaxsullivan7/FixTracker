package com.fixtracker.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

class FixRepository(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "fixtracker.json")
    private val backupFile = File(appContext.filesDir, "fixtracker.backup.json")

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun load(): AppState {
        if (!file.exists()) return AppState()
        val parsed = runCatching {
            JSONObject(file.readText()).toState().sanitized()
        }.getOrElse {
            runCatching {
                if (backupFile.exists()) {
                    JSONObject(backupFile.readText()).toState().sanitized()
                } else {
                    null
                }
            }.getOrNull()
        }
        return parsed?.copy(settings = parsed.settings.copy(onboarded = true)) ?: AppState()
    }

    private fun persist(state: AppState) {
        runCatching {
            val target = file
            if (target.exists()) {
                target.copyTo(backupFile, overwrite = true)
            }
            val tmp = File(appContext.filesDir, "fixtracker.tmp")
            tmp.writeText(state.toJson().toString(2))
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }
    }

    private fun mutate(block: (AppState) -> AppState) {
        val next = block(_state.value).sanitized()
        _state.value = next
        persist(next)
    }

    private fun commit(state: AppState) {
        val next = state.sanitized()
        _state.value = next
        persist(next)
    }

    // ------------------------------------------------------------ onboarding

    fun completeOnboarding(name: String, withDemo: Boolean) {
        val base = if (withDemo) SeedData.create() else AppState()
        commit(
            base.copy(
                ownerName = name.trim(),
                settings = base.settings.copy(onboarded = true),
            )
        )
    }

    // ------------------------------------------------------------ vehicles

    fun setActiveVehicle(vehicleId: String) {
        mutate { it.copy(activeVehicleId = vehicleId) }
    }

    fun updateKm(vehicleId: String, km: Int, date: LocalDate = LocalDate.now()) {
        if (km <= 0) return
        mutate { state ->
            state.copy(vehicles = state.vehicles.map {
                if (it.id == vehicleId) it.copy(km = km, kmUpdatedAt = date) else it
            })
        }
    }

    fun addVehicle(vehicle: Vehicle) {
        mutate { state ->
            val vehicleWithKm = if (vehicle.kmUpdatedAt == null) {
                vehicle.copy(kmUpdatedAt = LocalDate.now())
            } else vehicle
            state.copy(
                vehicles = state.vehicles + vehicleWithKm,
                reminders = state.reminders.syncDocumentReminders(vehicleWithKm),
                activeVehicleId = vehicleWithKm.id,
            )
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        mutate { state ->
            state.copy(
                vehicles = state.vehicles.map { if (it.id == vehicle.id) vehicle else it },
                reminders = state.reminders.syncDocumentReminders(vehicle),
            )
        }
    }

    fun deleteVehicle(vehicleId: String) {
        mutate { state ->
            val remaining = state.vehicles.filterNot { it.id == vehicleId }
            state.copy(
                vehicles = remaining,
                records = state.records.filterNot { it.vehicleId == vehicleId },
                fuelEntries = state.fuelEntries.filterNot { it.vehicleId == vehicleId },
                reminders = state.reminders.filterNot { it.vehicleId == vehicleId },
                activeVehicleId = when {
                    state.activeVehicleId != vehicleId -> state.activeVehicleId
                    else -> remaining.firstOrNull()?.id.orEmpty()
                },
            )
        }
    }

    // ------------------------------------------------------------ services

    fun addRecord(record: ServiceRecord) {
        mutate { state ->
            val vehicle = state.vehicle(record.vehicleId) ?: return@mutate state
            val vehicles = state.vehicles.map {
                if (it.id == vehicle.id) {
                    it.copy(
                        km = maxOf(it.km, record.km),
                        kmUpdatedAt = if (record.km >= it.km) record.date else it.kmUpdatedAt,
                    )
                } else it
            }
            state.copy(records = state.records + record, vehicles = vehicles).withDerivedData()
        }
    }

    fun updateRecord(record: ServiceRecord) {
        mutate { state ->
            if (state.records.none { it.id == record.id }) return@mutate state
            val vehicles = state.vehicles.map {
                if (it.id == record.vehicleId) {
                    it.copy(
                        km = maxOf(it.km, record.km),
                        kmUpdatedAt = if (record.km >= it.km) record.date else it.kmUpdatedAt,
                    )
                } else it
            }
            state.copy(
                records = state.records.map { if (it.id == record.id) record else it },
                vehicles = vehicles,
            ).withDerivedData()
        }
    }

    fun deleteRecord(recordId: String) {
        mutate { state ->
            state.copy(records = state.records.filterNot { it.id == recordId }).withDerivedData()
        }
    }

    fun restoreRecord(record: ServiceRecord) {
        mutate { state ->
            if (state.records.any { it.id == record.id }) return@mutate state
            state.copy(records = state.records + record).withDerivedData()
        }
    }

    // ------------------------------------------------------------ fuel

    fun addFuelEntry(entry: FuelEntry) {
        mutate { state ->
            val vehicles = state.vehicles.map {
                if (it.id == entry.vehicleId) {
                    it.copy(
                        km = maxOf(it.km, entry.km),
                        kmUpdatedAt = if (entry.km >= it.km) entry.date else it.kmUpdatedAt,
                    )
                } else it
            }
            state.copy(fuelEntries = state.fuelEntries + entry, vehicles = vehicles)
        }
    }

    fun updateFuelEntry(entry: FuelEntry) {
        mutate { state ->
            if (state.fuelEntries.none { it.id == entry.id }) return@mutate state
            val vehicles = state.vehicles.map {
                if (it.id == entry.vehicleId) {
                    it.copy(
                        km = maxOf(it.km, entry.km),
                        kmUpdatedAt = if (entry.km >= it.km) entry.date else it.kmUpdatedAt,
                    )
                } else it
            }
            state.copy(
                fuelEntries = state.fuelEntries.map { if (it.id == entry.id) entry else it },
                vehicles = vehicles,
            )
        }
    }

    fun deleteFuelEntry(entryId: String) {
        mutate { state -> state.copy(fuelEntries = state.fuelEntries.filterNot { it.id == entryId }) }
    }

    // ------------------------------------------------------------ reminders

    fun toggleReminder(reminderId: String) {
        mutate { state ->
            state.copy(reminders = state.reminders.map {
                if (it.id == reminderId) {
                    it.copy(done = !it.done, snoozedUntil = if (it.done) it.snoozedUntil else null)
                } else it
            })
        }
    }

    fun addReminder(reminder: Reminder) {
        mutate { state -> state.copy(reminders = state.reminders.upsert(reminder)) }
    }

    fun snoozeReminder(reminderId: String, until: LocalDate?) {
        mutate { state ->
            state.copy(reminders = state.reminders.map {
                if (it.id == reminderId) it.copy(snoozedUntil = until) else it
            })
        }
    }

    fun deleteReminder(reminderId: String) {
        mutate { state -> state.copy(reminders = state.reminders.filterNot { it.id == reminderId }) }
    }

    // ------------------------------------------------------------ settings & data

    fun updateSettings(settings: Settings) {
        mutate { state -> state.copy(settings = settings) }
    }

    fun updateOwnerName(name: String) {
        mutate { state -> state.copy(ownerName = name.trim()) }
    }

    fun resetDemo() {
        val current = _state.value
        val next = SeedData.create().copy(
            ownerName = current.ownerName,
            settings = current.settings.copy(onboarded = true),
        )
        commit(next)
    }

    fun clearAll() {
        val current = _state.value
        commit(
            AppState(
                ownerName = current.ownerName,
                activeVehicleId = "",
                settings = current.settings.copy(onboarded = true),
            )
        )
    }

    fun exportJson(): String = _state.value.toJson().toString(2)

    fun parseBackup(text: String): AppState? = runCatching {
        val json = JSONObject(text)
        json.toState().sanitized().copy(
            settings = json.optJSONObject("settings")?.toSettings()?.copy(onboarded = true)
                ?: Settings(onboarded = true),
        )
    }.getOrNull()

    fun replaceState(imported: AppState) {
        commit(imported.copy(settings = imported.settings.copy(onboarded = true)))
    }

    // ------------------------------------------------------------ derived data

    private fun AppState.withDerivedData(): AppState {
        var result = this
        vehicles.forEach { vehicle ->
            val oil = latestRecord(vehicle.id, ServiceType.ACEITE)
            val itv = latestRecord(vehicle.id, ServiceType.ITV)
            val insurance = latestRecord(vehicle.id, ServiceType.SEGURO)

            var updated = vehicle
            if (oil != null) {
                updated = updated.copy(
                    nextServiceKm = oil.km + vehicle.serviceKmInterval,
                    nextServiceDate = oil.date.plusYears(1),
                )
            }
            if (itv != null) updated = updated.copy(itvUntil = itv.date.plusYears(1))
            if (insurance != null) updated = updated.copy(insuranceUntil = insurance.date.plusYears(1))

            var reminders = result.reminders.syncDocumentReminders(updated)
            if (oil != null) {
                reminders = reminders.upsert(
                    Reminder(
                        id = "oil-${vehicle.id}",
                        vehicleId = vehicle.id,
                        title = "Cambio de aceite",
                        type = ServiceType.ACEITE,
                        dueDate = oil.date.plusYears(1),
                        dueKm = oil.km + vehicle.serviceKmInterval,
                        note = "Intervalo recomendado: ${formatInterval(vehicle.serviceKmInterval)} km o 1 año.",
                    )
                )
            }
            result = result.copy(
                vehicles = result.vehicles.map { if (it.id == vehicle.id) updated else it },
                reminders = reminders,
            )
        }
        return result
    }

    private fun formatInterval(km: Int): String =
        String.format(java.util.Locale.forLanguageTag("es-ES"), "%,d", km)
}

private fun List<Reminder>.upsert(reminder: Reminder): List<Reminder> =
    filterNot { it.id == reminder.id } + reminder

/** Mantiene los avisos de ITV, seguro e impuesto alineados con las fechas de la ficha. */
private fun List<Reminder>.syncDocumentReminders(vehicle: Vehicle): List<Reminder> {
    var result = this
    val documents = listOf(
        Reminder(
            id = "itv-${vehicle.id}",
            vehicleId = vehicle.id,
            title = "Inspección ITV",
            type = ServiceType.ITV,
            dueDate = vehicle.itvUntil,
        ),
        Reminder(
            id = "insurance-${vehicle.id}",
            vehicleId = vehicle.id,
            title = "Renovación de seguro",
            type = ServiceType.SEGURO,
            dueDate = vehicle.insuranceUntil,
        ),
        Reminder(
            id = "tax-${vehicle.id}",
            vehicleId = vehicle.id,
            title = "Impuesto de circulación",
            type = ServiceType.OTRO,
            dueDate = vehicle.roadTaxUntil,
        ),
    )

    documents.forEach { document ->
        val date = document.dueDate
        result = if (date == null) {
            result.filterNot { it.id == document.id }
        } else {
            val existing = result.firstOrNull { it.id == document.id }
            if (existing != null && existing.dueDate == date) {
                result.map {
                    if (it.id == document.id) {
                        it.copy(title = document.title, type = document.type)
                    } else it
                }
            } else {
                result.upsert(document)
            }
        }
    }
    return result
}

private fun AppState.sanitized(): AppState {
    val ids = vehicles.map { it.id }.toSet()
    val clean = copy(
        records = records.filter { it.vehicleId in ids },
        fuelEntries = fuelEntries.filter { it.vehicleId in ids },
        reminders = reminders.filter { it.vehicleId in ids },
    )
    val active = clean.activeVehicleId.takeIf { it in ids }
        ?: clean.vehicles.firstOrNull()?.id.orEmpty()
    return clean.copy(activeVehicleId = active)
}

// ---------------------------------------------------------------- serialization

private fun AppState.toJson(): JSONObject = JSONObject().apply {
    put("version", 2)
    put("ownerName", ownerName)
    put("activeVehicleId", activeVehicleId)
    put("vehicles", JSONArray().apply { vehicles.forEach { put(it.toJson()) } })
    put("records", JSONArray().apply { records.forEach { put(it.toJson()) } })
    put("fuelEntries", JSONArray().apply { fuelEntries.forEach { put(it.toJson()) } })
    put("reminders", JSONArray().apply { reminders.forEach { put(it.toJson()) } })
    put("settings", settings.toJson())
}

private fun Settings.toJson(): JSONObject = JSONObject().apply {
    put("currency", currency)
    put("theme", theme.name)
    put("onboarded", onboarded)
    put("biometricLock", biometricLock)
    put("annualBudget", annualBudget ?: JSONObject.NULL)
}

private fun Vehicle.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("brand", brand)
    put("model", model)
    put("plate", plate)
    put("year", year)
    put("km", km)
    put("fuel", fuel.name)
    put("type", type.name)
    put("colorName", colorName)
    put("itvUntil", itvUntil?.toString() ?: JSONObject.NULL)
    put("insuranceUntil", insuranceUntil?.toString() ?: JSONObject.NULL)
    put("roadTaxUntil", roadTaxUntil?.toString() ?: JSONObject.NULL)
    put("nextServiceKm", nextServiceKm ?: JSONObject.NULL)
    put("nextServiceDate", nextServiceDate?.toString() ?: JSONObject.NULL)
    put("kmUpdatedAt", kmUpdatedAt?.toString() ?: JSONObject.NULL)
}

private fun ServiceRecord.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("vehicleId", vehicleId)
    put("type", type.name)
    put("title", title)
    put("date", date.toString())
    put("km", km)
    put("cost", cost)
    put("shop", shop)
    put("notes", notes)
    put("attachmentNames", JSONArray().apply { attachmentNames.forEach { put(it) } })
}

private fun FuelEntry.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("vehicleId", vehicleId)
    put("date", date.toString())
    put("km", km)
    put("liters", liters)
    put("totalCost", totalCost)
    put("station", station)
    put("fullTank", fullTank)
    put("notes", notes)
}

private fun Reminder.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("vehicleId", vehicleId)
    put("title", title)
    put("type", type.name)
    put("dueDate", dueDate?.toString() ?: JSONObject.NULL)
    put("dueKm", dueKm ?: JSONObject.NULL)
    put("note", note)
    put("done", done)
    put("snoozedUntil", snoozedUntil?.toString() ?: JSONObject.NULL)
}

private fun JSONObject.toState(): AppState = AppState(
    ownerName = optString("ownerName", ""),
    activeVehicleId = optString("activeVehicleId", ""),
    vehicles = optJSONArray("vehicles")?.objects()?.mapNotNull { it.toVehicleOrNull() }.orEmpty(),
    records = optJSONArray("records")?.objects()?.mapNotNull { it.toRecordOrNull() }.orEmpty(),
    fuelEntries = optJSONArray("fuelEntries")?.objects()?.mapNotNull { it.toFuelOrNull() }.orEmpty(),
    reminders = optJSONArray("reminders")?.objects()?.mapNotNull { it.toReminderOrNull() }.orEmpty(),
    settings = optJSONObject("settings")?.toSettings() ?: Settings(),
)

private fun JSONObject.toSettings(): Settings = Settings(
    currency = optString("currency", "EUR"),
    theme = runCatching { ThemeMode.valueOf(optString("theme", "SYSTEM")) }
        .getOrDefault(ThemeMode.SYSTEM),
    onboarded = optBoolean("onboarded", false),
    biometricLock = optBoolean("biometricLock", false),
    annualBudget = if (isNull("annualBudget")) null else optDouble("annualBudget"),
)

private fun JSONObject.toVehicleOrNull(): Vehicle? = runCatching {
    Vehicle(
        id = getString("id"),
        brand = optString("brand"),
        model = optString("model"),
        plate = optString("plate"),
        year = optInt("year"),
        km = optInt("km"),
        fuel = runCatching { Fuel.valueOf(optString("fuel", "GASOLINA")) }.getOrDefault(Fuel.GASOLINA),
        type = runCatching { VehicleType.valueOf(optString("type", "CAR")) }.getOrDefault(VehicleType.CAR),
        colorName = optString("colorName"),
        itvUntil = dateOrNull("itvUntil"),
        insuranceUntil = dateOrNull("insuranceUntil"),
        roadTaxUntil = dateOrNull("roadTaxUntil"),
        nextServiceKm = intOrNull("nextServiceKm"),
        nextServiceDate = dateOrNull("nextServiceDate"),
        kmUpdatedAt = dateOrNull("kmUpdatedAt"),
    )
}.getOrNull()

private fun JSONObject.toRecordOrNull(): ServiceRecord? = runCatching {
    ServiceRecord(
        id = getString("id"),
        vehicleId = getString("vehicleId"),
        type = ServiceType.valueOf(optString("type", "OTRO")),
        title = optString("title"),
        date = LocalDate.parse(getString("date")),
        km = optInt("km"),
        cost = optDouble("cost", 0.0),
        shop = optString("shop"),
        notes = optString("notes"),
        attachmentNames = optJSONArray("attachmentNames")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                runCatching { array.getString(index) }.getOrNull()
            }
        }.orEmpty(),
    )
}.getOrNull()

private fun JSONObject.toFuelOrNull(): FuelEntry? = runCatching {
    FuelEntry(
        id = getString("id"),
        vehicleId = getString("vehicleId"),
        date = LocalDate.parse(getString("date")),
        km = optInt("km"),
        liters = optDouble("liters", 0.0),
        totalCost = optDouble("totalCost", 0.0),
        station = optString("station"),
        fullTank = optBoolean("fullTank", true),
        notes = optString("notes"),
    )
}.getOrNull()

private fun JSONObject.toReminderOrNull(): Reminder? = runCatching {
    Reminder(
        id = getString("id"),
        vehicleId = getString("vehicleId"),
        title = optString("title"),
        type = runCatching { ServiceType.valueOf(optString("type", "OTRO")) }
            .getOrDefault(ServiceType.OTRO),
        dueDate = dateOrNull("dueDate"),
        dueKm = intOrNull("dueKm"),
        note = optString("note"),
        done = optBoolean("done", false),
        snoozedUntil = dateOrNull("snoozedUntil"),
    )
}.getOrNull()

private fun JSONArray.objects(): List<JSONObject> =
    (0 until length()).mapNotNull { runCatching { optJSONObject(it) }.getOrNull() }

private fun JSONObject.dateOrNull(key: String): LocalDate? =
    stringOrNull(key)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun JSONObject.stringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.intOrNull(key: String): Int? =
    if (isNull(key) || !has(key)) null else optInt(key)
