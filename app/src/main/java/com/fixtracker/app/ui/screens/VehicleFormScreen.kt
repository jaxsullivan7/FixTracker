package com.fixtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.Fuel
import com.fixtracker.app.data.Vehicle
import com.fixtracker.app.data.VehicleType
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.DateField
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.OptionDialog
import com.fixtracker.app.ui.components.PrimaryButton
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.SelectorField
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.formatKm
import java.time.LocalDate
import java.util.UUID

@Composable
fun VehicleFormScreen(
    state: AppState,
    vehicleId: String?,
    onClose: () -> Unit,
    onSave: (Vehicle) -> Unit,
) {
    val existing = vehicleId?.let { state.vehicle(it) }
    var brand by remember(existing?.id) { mutableStateOf(existing?.brand.orEmpty()) }
    var model by remember(existing?.id) { mutableStateOf(existing?.model.orEmpty()) }
    var plate by remember(existing?.id) { mutableStateOf(existing?.plate.orEmpty()) }
    var year by remember(existing?.id) {
        mutableStateOf((existing?.year ?: LocalDate.now().year).toString())
    }
    var km by remember(existing?.id) { mutableStateOf(existing?.km?.toString().orEmpty()) }
    var fuel by remember(existing?.id) { mutableStateOf(existing?.fuel ?: Fuel.GASOLINA) }
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: VehicleType.CAR) }
    var colorName by remember(existing?.id) { mutableStateOf(existing?.colorName.orEmpty()) }
    var itv by remember(existing?.id) { mutableStateOf(existing?.itvUntil) }
    var insurance by remember(existing?.id) { mutableStateOf(existing?.insuranceUntil) }
    var fuelDialog by remember { mutableStateOf(false) }

    val canSave = brand.isNotBlank() && model.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = if (existing == null) "Nuevo vehículo" else "Editar vehículo",
                onBack = onClose,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (existing == null) "Añade un vehículo" else "Datos del vehículo",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Los datos básicos son suficientes para empezar a llevar su mantenimiento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(22.dp))
            Label("IDENTIFICACIÓN")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = "Marca",
                    placeholder = "Alfa Romeo",
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Modelo",
                    placeholder = "Giulietta",
                    modifier = Modifier.weight(1.2f),
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase().take(12) },
                    label = "Matrícula",
                    placeholder = "0000 ABC",
                    modifier = Modifier.weight(1.2f),
                )
                AppTextField(
                    value = year,
                    onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
                    label = "Año",
                    keyboardType = KeyboardType.Number,
                    placeholder = "2020",
                    modifier = Modifier.weight(0.8f),
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = km,
                onValueChange = { km = it.filter { c -> c.isDigit() }.take(7) },
                label = "Kilometraje actual",
                keyboardType = KeyboardType.Number,
                suffix = "km",
                placeholder = "0",
            )
        }

        item {
            Spacer(Modifier.height(22.dp))
            Label("TIPO DE VEHÍCULO")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.forEach { item ->
                    TypeOption(
                        label = item.label,
                        icon = when (item) {
                            VehicleType.CAR -> Icons.Outlined.DirectionsCar
                            VehicleType.MOTORCYCLE -> Icons.Outlined.TwoWheeler
                            VehicleType.OTHER -> Icons.Outlined.MoreHoriz
                        },
                        selected = type == item,
                        onClick = { type = item },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            SelectorField(
                label = "Combustible",
                value = fuel.label,
                onClick = { fuelDialog = true },
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = colorName,
                onValueChange = { colorName = it },
                label = "Color",
                placeholder = "Opcional",
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            Label("DOCUMENTACIÓN")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField(
                    label = "Próxima ITV",
                    date = itv,
                    onDateSelected = { itv = it },
                )
                DateField(
                    label = "Vencimiento del seguro",
                    date = insurance,
                    onDateSelected = { insurance = it },
                )
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            PrimaryButton(
                text = if (existing == null) "Añadir vehículo" else "Guardar cambios",
                enabled = canSave,
                onClick = {
                    onSave(
                        Vehicle(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            brand = brand.trim(),
                            model = model.trim(),
                            plate = plate.trim(),
                            year = year.toIntOrNull() ?: LocalDate.now().year,
                            km = km.toIntOrNull() ?: 0,
                            fuel = fuel,
                            type = type,
                            colorName = colorName.trim(),
                            itvUntil = itv,
                            insuranceUntil = insurance,
                            nextServiceKm = existing?.nextServiceKm,
                            nextServiceDate = existing?.nextServiceDate,
                        )
                    )
                },
            )
            if (existing != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Kilometraje registrado: ${formatKm(existing.km)} km",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (fuelDialog) {
        OptionDialog(
            title = "Combustible",
            options = Fuel.entries.map { it.name to it.label },
            selected = fuel.name,
            onDismiss = { fuelDialog = false },
            onSelect = { key ->
                Fuel.entries.firstOrNull { it.name == key }?.let { fuel = it }
                fuelDialog = false
            },
        )
    }
}

@Composable
private fun TypeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else FixTheme.palette.surfaceAlt
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}
