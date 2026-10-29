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

    private val file = File(context.filesDir, "fixtracker.json")

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        if (!file.exists()) persist(_state.value)
    }

    private fun load(): AppState {
        if (!file.exists()) return SeedData.create()
        return runCatching { JSONObject(file.readText()).toState() }
            .getOrElse { SeedData.create() }
    }

    private fun persist(state: AppState) {
        runCatching { file.writeText(state.toJson().toString(2)) }
    }

    private fun mutate(block: (AppState) -> AppState) {
        val next = block(_state.value)
        _state.value = next
        persist(next)
    }

    fun setActiveVehicle(vehicleId: String) {
        mutate { it.copy(activeVehicleId = vehicleId) }
    }

    fun updateKm(vehicleId: String, km: Int) {
        if (km <= 0) return
        mutate { state ->
            state.copy(vehicles = state.vehicles.map {
                if (it.id == vehicleId && km > it.km) it.copy(km = km) else it
            })
        }
    }

    fun addRecord(record: ServiceRecord) {
        mutate { state ->
            val vehicle = state.vehicle(record.vehicleId) ?: return@mutate state
            var updated = if (record.km > vehicle.km) vehicle.copy(km = record.km) else vehicle
            var reminders = state.reminders

            when (record.type) {
                ServiceType.ACEITE -> {
                    val nextKm = record.km + vehicle.serviceKmInterval
                    updated = updated.copy(
                        nextServiceKm = nextKm,
                        nextServiceDate = record.date.plusYears(1),
                    )
                    reminders = reminders.upsert(
                        Reminder(
                            id = "oil-${vehicle.id}",
                            vehicleId = vehicle.id,
                            title = "Cambio de aceite",
                            type = ServiceType.ACEITE,
                            dueDate = record.date.plusYears(1),
                            dueKm = nextKm,
                            note = "Intervalo recomendado: ${formatInterval(vehicle.serviceKmInterval)} km o 1 año.",
                        )
                    )
                }

                ServiceType.ITV -> {
                    updated = updated.copy(itvUntil = record.date.plusYears(1))
                    reminders = reminders.upsert(
                        Reminder(
                            id = "itv-${vehicle.id}",
                            vehicleId = vehicle.id,
                            title = "Inspección ITV",
                            type = ServiceType.ITV,
                            dueDate = record.date.plusYears(1),
                        )
                    )
                }

                ServiceType.SEGURO -> {
                    updated = updated.copy(insuranceUntil = record.date.plusYears(1))
                    reminders = reminders.upsert(
                        Reminder(
                            id = "insurance-${vehicle.id}",
                            vehicleId = vehicle.id,
                            title = "Renovación de seguro",
                            type = ServiceType.SEGURO,
                            dueDate = record.date.plusYears(1),
                        )
                    )
                }

                else -> Unit
            }

            state.copy(
                vehicles = state.vehicles.map { if (it.id == updated.id) updated else it },
                records = state.records + record,
                reminders = reminders,
            )
        }
    }

    fun deleteRecord(recordId: String) {
        mutate { state -> state.copy(records = state.records.filterNot { it.id == recordId }) }
    }

    fun addVehicle(vehicle: Vehicle) {
        mutate { state ->
            state.copy(
                vehicles = state.vehicles + vehicle,
                activeVehicleId = vehicle.id,
            )
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        mutate { state ->
            state.copy(vehicles = state.vehicles.map { if (it.id == vehicle.id) vehicle else it })
        }
    }

    fun deleteVehicle(vehicleId: String) {
        mutate { state ->
            val remaining = state.vehicles.filterNot { it.id == vehicleId }
            state.copy(
                vehicles = remaining,
                records = state.records.filterNot { it.vehicleId == vehicleId },
                reminders = state.reminders.filterNot { it.vehicleId == vehicleId },
                activeVehicleId = when {
                    state.activeVehicleId != vehicleId -> state.activeVehicleId
                    else -> remaining.firstOrNull()?.id.orEmpty()
                },
            )
        }
    }

    fun toggleReminder(reminderId: String) {
        mutate { state ->
            state.copy(reminders = state.reminders.map {
                if (it.id == reminderId) it.copy(done = !it.done) else it
            })
        }
    }

    fun addReminder(reminder: Reminder) {
        mutate { state -> state.copy(reminders = state.reminders.upsert(reminder)) }
    }

    fun deleteReminder(reminderId: String) {
        mutate { state -> state.copy(reminders = state.reminders.filterNot { it.id == reminderId }) }
    }

    fun updateSettings(settings: Settings) {
        mutate { state -> state.copy(settings = settings) }
    }

    fun updateOwnerName(name: String) {
        mutate { state -> state.copy(ownerName = name.trim()) }
    }

    fun resetDemo() {
        val next = SeedData.create()
        _state.value = next
        persist(next)
    }

    fun clearAll() {
        mutate { current ->
            AppState(
                ownerName = current.ownerName,
                activeVehicleId = "",
                settings = current.settings,
            )
        }
    }

    fun exportJson(): String = _state.value.toJson().toString(2)

    private fun formatInterval(km: Int): String = String.format("%,d", km).replace(',', '.')
}

private fun List<Reminder>.upsert(reminder: Reminder): List<Reminder> =
    filterNot { it.id == reminder.id } + reminder

// ---------------------------------------------------------------- serialization

private fun AppState.toJson(): JSONObject = JSONObject().apply {
    put("version", 1)
    put("ownerName", ownerName)
    put("activeVehicleId", activeVehicleId)
    put("vehicles", JSONArray().apply { vehicles.forEach { put(it.toJson()) } })
    put("records", JSONArray().apply { records.forEach { put(it.toJson()) } })
    put("reminders", JSONArray().apply { reminders.forEach { put(it.toJson()) } })
    put("settings", JSONObject().apply {
        put("currency", settings.currency)
        put("theme", settings.theme.name)
    })
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
    put("nextServiceKm", nextServiceKm ?: JSONObject.NULL)
    put("nextServiceDate", nextServiceDate?.toString() ?: JSONObject.NULL)
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
}

private fun JSONObject.toState(): AppState = AppState(
    ownerName = optString("ownerName", ""),
    activeVehicleId = optString("activeVehicleId", ""),
    vehicles = optJSONArray("vehicles")?.objects()?.mapNotNull { it.toVehicleOrNull() }.orEmpty(),
    records = optJSONArray("records")?.objects()?.mapNotNull { it.toRecordOrNull() }.orEmpty(),
    reminders = optJSONArray("reminders")?.objects()?.mapNotNull { it.toReminderOrNull() }.orEmpty(),
    settings = optJSONObject("settings")?.let {
        Settings(
            currency = it.optString("currency", "EUR"),
            theme = runCatching { ThemeMode.valueOf(it.optString("theme", "SYSTEM")) }
                .getOrDefault(ThemeMode.SYSTEM),
        )
    } ?: Settings(),
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
        nextServiceKm = intOrNull("nextServiceKm"),
        nextServiceDate = dateOrNull("nextServiceDate"),
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
