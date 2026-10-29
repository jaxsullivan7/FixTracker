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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.NotificationsNone
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.Reminder
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.DateField
import com.fixtracker.app.ui.components.EmptyState
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.PillButton
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.StatusChip
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.daysUntil
import com.fixtracker.app.util.dueDateLabel
import com.fixtracker.app.util.formatKm
import java.time.LocalDate
import java.util.UUID

@Composable
fun RemindersScreen(
    state: AppState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onAdd: (Reminder) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }

    val active = state.reminders.filterNot { it.done }
    val done = state.reminders.filter { it.done }
    val overdue = active.filter { isOverdue(it, state) }
    val soon = active.filter { !isOverdue(it, state) && daysLeft(it) != null && daysLeft(it)!! <= 30 }
    val later = active.filter { !isOverdue(it, state) && (daysLeft(it) == null || daysLeft(it)!! > 30) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = "Recordatorios",
                onBack = onBack,
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { showAdd = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Nuevo recordatorio",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = when {
                    active.isEmpty() -> "Todo al día"
                    overdue.isNotEmpty() -> "${overdue.size} ${if (overdue.size == 1) "aviso vencido" else "avisos vencidos"}"
                    else -> "${active.size} ${if (active.size == 1) "aviso pendiente" else "avisos pendientes"}"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Mantenimiento, documentación y fechas clave de tu garaje.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.reminders.isEmpty()) {
            item {
                Spacer(Modifier.height(40.dp))
                EmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Sin recordatorios",
                    message = "Crea un aviso para no olvidar la próxima revisión o el seguro.",
                )
            }
        }

        reminderSection("Vencidos", overdue, state, onToggle)
        reminderSection("Próximos 30 días", soon, state, onToggle)
        reminderSection("Más adelante", later, state, onToggle)
        reminderSection("Completados", done, state, onToggle)
    }

    if (showAdd) {
        AddReminderDialog(
            state = state,
            onDismiss = { showAdd = false },
            onSave = {
                onAdd(it)
                showAdd = false
            },
        )
    }
}

private fun daysLeft(reminder: Reminder): Long? = reminder.dueDate?.let { daysUntil(it) }

private fun isOverdue(reminder: Reminder, state: AppState): Boolean {
    val dateOverdue = reminder.dueDate?.let { daysUntil(it) < 0 } ?: false
    val kmOverdue = reminder.dueKm?.let { due ->
        val km = state.vehicle(reminder.vehicleId)?.km ?: 0
        due <= km
    } ?: false
    return dateOverdue || kmOverdue
}

private fun androidx.compose.foundation.lazy.LazyListScope.reminderSection(
    title: String,
    itemsList: List<Reminder>,
    state: AppState,
    onToggle: (String) -> Unit,
) {
    if (itemsList.isEmpty()) return
    item(key = "section-$title") {
        Spacer(Modifier.height(26.dp))
        Label(title.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(9.dp))
    }
    items(itemsList, key = { it.id }) { reminder ->
        val vehicle = state.vehicle(reminder.vehicleId)
        val overdue = isOverdue(reminder, state)
        val palette = FixTheme.palette
        val chipColor = when {
            reminder.done -> MaterialTheme.colorScheme.onSurfaceVariant
            overdue -> palette.danger
            else -> palette.copper
        }
        val chipBg = when {
            reminder.done -> palette.surfaceAlt
            overdue -> palette.dangerSoft
            else -> palette.copperSoft
        }
        val chipText = when {
            reminder.done -> "Completado"
            overdue -> "Vencido"
            reminder.dueDate != null -> dueDateLabel(reminder.dueDate)
            else -> "Pendiente"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeGlyph(reminder.type, size = 40.dp, iconSize = 18.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (reminder.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (reminder.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        vehicle?.fullName?.let { append(it) }
                        reminder.dueKm?.let {
                            if (isNotEmpty()) append("  ·  ")
                            append("${formatKm(it)} km")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                StatusChip(text = chipText, color = chipColor, background = chipBg)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (reminder.done) MaterialTheme.colorScheme.primary else FixTheme.palette.surfaceAlt)
                    .border(
                        1.dp,
                        if (reminder.done) MaterialTheme.colorScheme.primary else FixTheme.palette.hairline,
                        CircleShape,
                    )
                    .clickable { onToggle(reminder.id) },
                contentAlignment = Alignment.Center,
            ) {
                if (reminder.done) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Marcar como pendiente",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddReminderDialog(
    state: AppState,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var vehicleId by remember {
        mutableStateOf(state.activeVehicle?.id ?: state.vehicles.firstOrNull()?.id.orEmpty())
    }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now().plusMonths(1)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Nuevo recordatorio", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Título",
                    placeholder = "Ej. Renovar seguro",
                )
                if (state.vehicles.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    Label("VEHÍCULO")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(state.vehicles, key = { it.id }) { vehicle ->
                            PillButton(
                                text = vehicle.fullName,
                                selected = vehicle.id == vehicleId,
                                onClick = { vehicleId = vehicle.id },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                DateField(
                    label = "Fecha",
                    date = date,
                    onDateSelected = { date = it },
                    allowClear = false,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No se enviarán notificaciones: los avisos aparecen dentro de la app.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val current = state.vehicle(vehicleId) ?: return@TextButton
                    onSave(
                        Reminder(
                            id = UUID.randomUUID().toString(),
                            vehicleId = current.id,
                            title = title.ifBlank { "Recordatorio" },
                            type = ServiceType.OTRO,
                            dueDate = date,
                        )
                    )
                },
                enabled = vehicleId.isNotBlank(),
            ) {
                Text("Guardar", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
