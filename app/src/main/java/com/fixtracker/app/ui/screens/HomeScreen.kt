package com.fixtracker.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.Reminder
import com.fixtracker.app.data.ServiceRecord
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.data.Vehicle
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.EmptyState
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.SectionHeader
import com.fixtracker.app.ui.components.SpendBars
import com.fixtracker.app.ui.components.StatusChip
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography
import com.fixtracker.app.util.daysUntil
import com.fixtracker.app.util.dueDateLabel
import com.fixtracker.app.util.formatHeaderDate
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.initialOf
import com.fixtracker.app.util.monthShort
import com.fixtracker.app.util.relativePastLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AppState,
    onOpenVehicle: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onAddRecord: () -> Unit,
    onUpdateKm: (Int) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vehicle = state.activeVehicle
    val reminders = vehicle?.let { v ->
        state.remindersFor(v.id).sortedBy { r ->
            r.dueDate?.let { daysUntil(it) } ?: 999
        }
    }.orEmpty()
    val records = vehicle?.let { state.recordsFor(it.id) }.orEmpty()
    var showKmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
    ) {
        item {
            HomeTopBar(
                state = state,
                alertCount = reminders.size,
                onOpenReminders = onOpenReminders,
                onOpenSettings = onOpenSettings,
            )
        }

        item {
            Spacer(Modifier.height(22.dp))
            Text(
                text = formatHeaderDate(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(7.dp))
            val name = state.ownerName
            Text(
                text = if (name.isBlank()) greetingForNow() else "${greetingForNow()}, $name",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (vehicle != null) {
            item {
                Spacer(Modifier.height(18.dp))
                VehicleHero(
                    vehicle = vehicle,
                    onClick = { onOpenVehicle(vehicle.id) },
                )
            }

            item {
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(
                        label = "Servicio",
                        icon = Icons.Outlined.Build,
                        onClick = onAddRecord,
                        modifier = Modifier.weight(1f),
                    )
                    QuickAction(
                        label = "Kilómetros",
                        icon = Icons.Outlined.Speed,
                        onClick = { showKmDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                    QuickAction(
                        label = "Avisos",
                        icon = Icons.Outlined.NotificationsNone,
                        onClick = onOpenReminders,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
                SectionHeader(
                    title = "Próximos",
                    action = "Ver todo",
                    onAction = onOpenReminders,
                )
            }

            if (reminders.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "Nada pendiente",
                        message = "No tienes avisos para este vehículo.",
                    )
                }
            } else {
                items(reminders.take(3).size) { index ->
                    val reminder = reminders[index]
                    ReminderRow(
                        reminder = reminder,
                        vehicle = vehicle,
                        onClick = onOpenReminders,
                    )
                    if (index != reminders.take(3).lastIndex) Spacer(Modifier.height(9.dp))
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
                SpendCard(state = state, vehicle = vehicle)
            }

            item {
                Spacer(Modifier.height(30.dp))
                SectionHeader(
                    title = "Actividad reciente",
                    action = "Historial",
                    onAction = onOpenHistory,
                )
            }

            if (records.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Build,
                        title = "Sin mantenimientos",
                        message = "Añade tu primer servicio para empezar el historial.",
                    )
                }
            } else {
                item {
                    Spacer(Modifier.height(12.dp))
                    FixCard(modifier = Modifier.fillMaxWidth()) {
                        records.take(3).forEachIndexed { index, record ->
                            RecordRow(
                                record = record,
                                currency = state.settings.currency,
                                onClick = { onOpenRecord(record.id) },
                            )
                            if (index != records.take(3).lastIndex) {
                                Hairline(Modifier.padding(horizontal = 14.dp))
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Spacer(Modifier.height(40.dp))
                EmptyState(
                    icon = Icons.Outlined.Add,
                    title = "Tu garaje está vacío",
                    message = "Añade un vehículo para empezar a registrar su mantenimiento.",
                )
            }
        }
    }

    if (showKmDialog && vehicle != null) {
        UpdateKmDialog(
            currentKm = vehicle.km,
            onDismiss = { showKmDialog = false },
            onConfirm = {
                onUpdateKm(it)
                showKmDialog = false
            },
        )
    }
}

private fun greetingForNow(): String = when (LocalTime.now().hour) {
    in 6..12 -> "Buenos días"
    in 13..20 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
private fun HomeTopBar(
    state: AppState,
    alertCount: Int,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(FixTheme.palette.copper)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = "FIXTRACKER",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.6.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onOpenReminders),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = "Avisos",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp),
                )
            }
            if (alertCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(FixTheme.palette.copper)
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (state.ownerName.isBlank()) "FT" else initialOf(state.ownerName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun VehicleHero(vehicle: Vehicle, onClick: () -> Unit) {
    val palette = FixTheme.palette
    val days = vehicle.daysToService()
    val progress = days?.let { (1f - (it.coerceIn(0, 365) / 365f)).coerceIn(0.06f, 1f) } ?: 0f
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val line = Color.White.copy(alpha = 0.055f)
            drawCircle(
                color = line,
                radius = size.width * 0.38f,
                center = Offset(size.width * 0.92f, size.height * 0.18f),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = line,
                radius = size.width * 0.52f,
                center = Offset(size.width * 1.02f, size.height * 0.12f),
                style = Stroke(width = 1.dp.toPx()),
            )
            val path = Path().apply {
                moveTo(-20f, size.height * 0.86f)
                cubicTo(
                    size.width * 0.28f, size.height * 0.6f,
                    size.width * 0.58f, size.height * 0.98f,
                    size.width + 20f, size.height * 0.62f,
                )
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.07f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VEHÍCULO ACTIVO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = vehicle.plate,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = vehicle.fullName,
                style = FixTypography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = listOf(vehicle.fuel.label, vehicle.year.toString(), vehicle.type.label)
                    .distinct().joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f),
            )

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "KILOMETRAJE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatKm(vehicle.km),
                            style = FixTypography.displayMedium.copy(fontSize = 38.sp),
                            color = Color.White,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "km",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 7.dp),
                        )
                    }
                }
                ServiceGauge(
                    progress = progress,
                    days = days,
                    tint = palette.copper,
                )
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFFD9A277),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Próximo aceite",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = buildString {
                        append(days?.let { dueDateLabel(LocalDate.now().plusDays(it)) } ?: "Sin fecha")
                        vehicle.kmToService()?.let {
                            append("  ·  ")
                            append(formatKm(it))
                            append(" km")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ServiceGauge(progress: Float, days: Long?, tint: Color) {
    Box(
        modifier = Modifier.size(78.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = days?.coerceAtLeast(0)?.toString() ?: "—",
                style = FixTypography.headlineMedium.copy(fontSize = 23.sp),
                color = Color.White,
            )
            Text(
                text = "días",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp, letterSpacing = 0.sp),
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(FixTheme.palette.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    vehicle: Vehicle,
    onClick: () -> Unit,
) {
    val palette = FixTheme.palette
    val (label, color, background) = reminderStatus(reminder, vehicle)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeGlyph(reminder.type, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(text = label, color = color, background = background)
                reminder.dueKm?.let { dueKm ->
                    val remaining = dueKm - vehicle.km
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (remaining > 0) "${formatKm(remaining)} km" else "km superados",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun reminderStatus(reminder: Reminder, vehicle: Vehicle): Triple<String, Color, Color> {
    val p = FixTheme.palette
    val date = reminder.dueDate
    val days = date?.let { daysUntil(it) }
    val kmLeft = reminder.dueKm?.let { it - vehicle.km }

    return when {
        (days != null && days < 0) || (kmLeft != null && kmLeft <= 0) ->
            Triple(date?.let { dueDateLabel(it) } ?: "Vencido", p.danger, p.dangerSoft)
        (days != null && days <= 30) || (kmLeft != null && kmLeft <= 1_500) ->
            Triple(date?.let { dueDateLabel(it) } ?: "Próximo", p.copper, p.copperSoft)
        days != null -> Triple(dueDateLabel(date), p.positive, p.positiveSoft)
        else -> Triple("Próximo", p.positive, p.positiveSoft)
    }
}

@Composable
private fun SpendCard(state: AppState, vehicle: Vehicle) {
    val palette = FixTheme.palette
    val now = YearMonth.now()
    val months = (5 downTo 0).map { now.minusMonths(it.toLong()) }
    val values = months.map { month ->
        state.recordsFor(vehicle.id)
            .filter { YearMonth.from(it.date) == month }
            .sumOf { it.cost }
    }
    val labels = months.map { monthShort(it) }
    val current = values.lastOrNull() ?: 0.0
    val previous = values.getOrNull(values.size - 2) ?: 0.0
    val delta = when {
        previous <= 0.0 -> null
        else -> ((current - previous) / previous * 100).roundToInt()
    }

    FixCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Label("ESTE MES")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatMoney(current, state.settings.currency),
                        style = FixTypography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (delta != null) {
                    StatusChip(
                        text = (if (delta > 0) "+" else "") + "$delta%",
                        color = if (delta > 0) palette.copper else palette.positive,
                        background = if (delta > 0) palette.copperSoft else palette.positiveSoft,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SpendBars(values = values, labels = labels)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Gasto de los últimos 6 meses",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordRow(
    record: ServiceRecord,
    currency: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeGlyph(record.type, size = 40.dp, iconSize = 18.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(relativePastLabel(record.date))
                    if (record.km > 0) {
                        append("  ·  ")
                        append(formatKm(record.km))
                        append(" km")
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = formatMoney(record.cost, currency),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UpdateKmDialog(
    currentKm: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var value by remember { mutableStateOf(currentKm.toString()) }
    val parsed = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Actualizar kilometraje", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    text = "Actualmente tienes ${formatKm(currentKm)} km. Introduce el nuevo kilometraje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                AppTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(7) },
                    label = "Kilómetros",
                    keyboardType = KeyboardType.Number,
                    suffix = "km",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && parsed > currentKm,
            ) {
                Text(
                    "Guardar",
                    color = if (parsed != null && parsed > currentKm) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
