package com.fixtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.FuelEntry
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.ConfirmDialog
import com.fixtracker.app.ui.components.DateField
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.PillButton
import com.fixtracker.app.ui.components.PrimaryButton
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatPricePerLiter
import java.time.LocalDate
import java.util.UUID

@Composable
fun FuelFormScreen(
    state: AppState,
    initialVehicleId: String?,
    editingEntryId: String? = null,
    onClose: () -> Unit,
    onSave: (FuelEntry) -> Unit,
    onDelete: (String) -> Unit,
) {
    val existing = editingEntryId?.let { id -> state.fuelEntries.firstOrNull { it.id == id } }
    val defaultVehicle = state.vehicle(existing?.vehicleId ?: initialVehicleId ?: state.activeVehicleId)
        ?: state.activeVehicle

    var vehicleId by remember(existing?.id) {
        mutableStateOf(defaultVehicle?.id ?: state.vehicles.firstOrNull()?.id.orEmpty())
    }
    val vehicle = state.vehicle(vehicleId)
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var km by remember(existing?.id) {
        mutableStateOf(existing?.km?.toString() ?: vehicle?.km?.toString().orEmpty())
    }
    var liters by remember(existing?.id) {
        mutableStateOf(existing?.liters?.let { formatDecimal(it) }.orEmpty())
    }
    var totalCost by remember(existing?.id) {
        mutableStateOf(existing?.totalCost?.let { formatDecimal(it) }.orEmpty())
    }
    var station by remember(existing?.id) { mutableStateOf(existing?.station.orEmpty()) }
    var fullTank by remember(existing?.id) { mutableStateOf(existing?.fullTank ?: true) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val litersValue = liters.replace(',', '.').toDoubleOrNull() ?: 0.0
    val costValue = totalCost.replace(',', '.').toDoubleOrNull() ?: 0.0
    val pricePerLiter = if (litersValue > 0 && costValue > 0) costValue / litersValue else null
    val canSave = vehicle != null &&
        (km.toIntOrNull() ?: 0) > 0 &&
        litersValue > 0 &&
        costValue > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = if (existing == null) "Nuevo repostaje" else "Editar repostaje",
                onBack = onClose,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (existing == null) "Añade combustible" else "Corrige el repostaje",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Con cada depósito lleno calculamos el consumo real y el coste por kilómetro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.vehicles.size > 1) {
            item {
                Spacer(Modifier.height(22.dp))
                Label("VEHÍCULO")
                Spacer(Modifier.height(9.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.vehicles, key = { it.id }) { v ->
                        PillButton(
                            text = v.fullName,
                            selected = v.id == vehicleId,
                            onClick = {
                                if (v.id != vehicleId) {
                                    vehicleId = v.id
                                    if (existing == null) km = v.km.toString()
                                }
                            },
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField(
                    label = "Fecha",
                    date = date,
                    onDateSelected = { date = it ?: LocalDate.now() },
                    allowClear = false,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = km,
                    onValueChange = { km = it.filter { c -> c.isDigit() }.take(7) },
                    label = "Kilometraje",
                    keyboardType = KeyboardType.Number,
                    suffix = "km",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = liters,
                    onValueChange = { input ->
                        liters = input.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
                    },
                    label = "Litros",
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "0,0",
                    suffix = "L",
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = totalCost,
                    onValueChange = { input ->
                        totalCost = input.filter { it.isDigit() || it == ',' || it == '.' }.take(8)
                    },
                    label = "Coste total",
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "0,00",
                    suffix = "€",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FixTheme.palette.surfaceAlt)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.LocalGasStation,
                    contentDescription = null,
                    tint = FixTheme.palette.copper,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Precio por litro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatPricePerLiter(pricePerLiter),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = station,
                onValueChange = { station = it },
                label = "Gasolinera",
                placeholder = "Opcional",
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FixTheme.palette.surfaceAlt)
                    .clickable { fullTank = !fullTank }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Depósito lleno",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Necesario para calcular el consumo entre repostajes.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = fullTank,
                    onCheckedChange = { fullTank = it },
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notas",
                placeholder = "Opcional",
                singleLine = false,
                minLines = 2,
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (existing == null) "Guardar repostaje" else "Guardar cambios",
                enabled = canSave,
                onClick = {
                    val current = state.vehicle(vehicleId) ?: return@PrimaryButton
                    onSave(
                        FuelEntry(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            vehicleId = current.id,
                            date = date,
                            km = km.toIntOrNull() ?: current.km,
                            liters = litersValue,
                            totalCost = costValue,
                            station = station.trim(),
                            fullTank = fullTank,
                            notes = notes.trim(),
                        )
                    )
                },
            )
            if (vehicle != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Kilometraje actual: ${formatKm(vehicle.km)} km",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (existing != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { confirmDelete = true }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = FixTheme.palette.danger,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "Eliminar repostaje",
                        style = MaterialTheme.typography.labelMedium,
                        color = FixTheme.palette.danger,
                    )
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "¿Eliminar este repostaje?",
            message = "El repostaje desaparecerá del historial y de las estadísticas de consumo.",
            onConfirm = {
                confirmDelete = false
                onDelete(existing.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(java.util.Locale.US, "%.2f", value).replace('.', ',')
