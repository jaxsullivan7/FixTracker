package com.fixtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.Vehicle
import com.fixtracker.app.ui.components.EmptyState
import com.fixtracker.app.ui.components.StatusChip
import com.fixtracker.app.ui.components.ThinProgress
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.dueDateLabel
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.initialOf

@Composable
fun VehiclesScreen(
    state: AppState,
    onOpenVehicle: (String) -> Unit,
    onAddVehicle: () -> Unit,
) {
    val totalKm = state.vehicles.sumOf { it.km }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Tu garaje",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(state.vehicles.size)
                            append(if (state.vehicles.size == 1) " vehículo" else " vehículos")
                            if (totalKm > 0) {
                                append("  ·  ")
                                append(formatKm(totalKm))
                                append(" km en total")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAddVehicle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Añadir vehículo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }

        if (state.vehicles.isEmpty()) {
            item {
                Spacer(Modifier.height(60.dp))
                EmptyState(
                    icon = Icons.Outlined.DirectionsCar,
                    title = "Aún no hay vehículos",
                    message = "Añade tu coche o moto para llevar al día su mantenimiento.",
                )
            }
        }

        items(state.vehicles.size) { index ->
            val vehicle = state.vehicles[index]
            Spacer(Modifier.height(14.dp))
            VehicleCard(
                vehicle = vehicle,
                isActive = vehicle.id == state.activeVehicleId,
                totalSpend = state.spendFor(vehicle.id),
                currency = state.settings.currency,
                onClick = { onOpenVehicle(vehicle.id) },
            )
        }

        item {
            Spacer(Modifier.height(14.dp))
            AddVehicleCard(onClick = onAddVehicle)
        }

        item {
            Spacer(Modifier.height(26.dp))
            Text(
                text = "Tus datos se guardan únicamente en este dispositivo.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    isActive: Boolean,
    totalSpend: Double,
    currency: String,
    onClick: () -> Unit,
) {
    val palette = FixTheme.palette
    val days = vehicle.daysToService()
    val kmLeft = vehicle.kmToService()
    val shape = RoundedCornerShape(24.dp)
    val serviceProgress = kmLeft?.let {
        1f - (it.toFloat() / vehicle.serviceKmInterval).coerceIn(0f, 1f)
    } ?: 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                else palette.hairline,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(palette.heroStart, palette.heroEnd)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialOf(vehicle.brand),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = vehicle.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${vehicle.plate}  ·  ${vehicle.year}  ·  ${vehicle.fuel.label}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isActive) {
                StatusChip(
                    text = "Activo",
                    color = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                )
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = palette.hairline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric(
                label = "Kilometraje",
                value = "${formatKm(vehicle.km)} km",
                modifier = Modifier.weight(1f),
            )
            Metric(
                label = "Próximo",
                value = days?.let { dueDateLabel(java.time.LocalDate.now().plusDays(it)) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            Metric(
                label = "Invertido",
                value = formatMoney(totalSpend, currency),
                modifier = Modifier.weight(1f),
            )
        }

        if (kmLeft != null) {
            Spacer(Modifier.height(15.dp))
            ThinProgress(
                progress = serviceProgress,
                color = if (serviceProgress > 0.85f) palette.copper
                else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (kmLeft > 0) "Próximo servicio en ${formatKm(kmLeft)} km"
                else "Servicio pendiente",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddVehicleCard(onClick: () -> Unit) {
    val palette = FixTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(22.dp))
            .drawBehind {
                drawRoundRect(
                    color = palette.hairline,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f)),
                    ),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Añadir vehículo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Coche, moto o furgoneta",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
