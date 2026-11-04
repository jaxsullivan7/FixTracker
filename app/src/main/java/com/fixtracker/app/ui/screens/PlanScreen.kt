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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.PlanItem
import com.fixtracker.app.data.PlanStatus
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.data.buildMaintenancePlan
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.StatusChip
import com.fixtracker.app.ui.components.ThinProgress
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.dueDateLabel
import com.fixtracker.app.util.formatDate
import com.fixtracker.app.util.formatKm
import java.time.LocalDate

@Composable
fun PlanScreen(
    state: AppState,
    vehicleId: String,
    onBack: () -> Unit,
    onAddRecord: (ServiceType) -> Unit,
) {
    val vehicle = state.vehicle(vehicleId)
    if (vehicle == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vehículo no encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val today = LocalDate.now()
    val plan = buildMaintenancePlan(state, vehicle, today)
    val overdue = plan.count { it.status(vehicle, today) == PlanStatus.OVERDUE }
    val soon = plan.count { it.status(vehicle, today) == PlanStatus.SOON }
    val ok = plan.count { it.status(vehicle, today) == PlanStatus.OK }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(title = "Plan de mantenimiento", onBack = onBack)
            Spacer(Modifier.height(10.dp))
            Text(
                text = vehicle.fullName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Una guía orientativa según el tipo de vehículo y tu último servicio. Toca cualquier partida para registrar el trabajo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(18.dp))
            PlanSummary(
                overdue = overdue,
                soon = soon,
                ok = ok,
                total = plan.size,
            )
        }

        item {
            Spacer(Modifier.height(20.dp))
            plan.forEachIndexed { index, item ->
                PlanCard(
                    item = item,
                    vehicle = vehicle,
                    today = today,
                    onClick = { onAddRecord(item.type) },
                )
                if (index != plan.lastIndex) Spacer(Modifier.height(9.dp))
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Los intervalos son una referencia. Ajusta siempre al manual del fabricante y al uso real del vehículo.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanSummary(overdue: Int, soon: Int, ok: Int, total: Int) {
    val palette = FixTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "ESTADO DEL PLAN",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = Color.White.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    overdue > 0 -> "$overdue vencidos"
                    soon > 0 -> "$soon próximos"
                    else -> "Todo al día"
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = Color.White,
            )
            Text(
                text = "$total partidas · $ok al día",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            SummaryDot("Vencido", overdue, palette.danger)
            Spacer(Modifier.height(4.dp))
            SummaryDot("Próximo", soon, palette.copper)
            Spacer(Modifier.height(4.dp))
            SummaryDot("Al día", ok, palette.positive)
        }
    }
}

@Composable
private fun SummaryDot(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PlanCard(
    item: PlanItem,
    vehicle: com.fixtracker.app.data.Vehicle,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val palette = FixTheme.palette
    val status = item.status(vehicle, today)
    val statusColor = when (status) {
        PlanStatus.OVERDUE -> palette.danger
        PlanStatus.SOON -> palette.copper
        PlanStatus.OK -> palette.positive
        PlanStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusBg = when (status) {
        PlanStatus.OVERDUE -> palette.dangerSoft
        PlanStatus.SOON -> palette.copperSoft
        PlanStatus.OK -> palette.positiveSoft
        PlanStatus.UNKNOWN -> palette.surfaceAlt
    }
    val statusLabel = when (status) {
        PlanStatus.OVERDUE -> "Vencido"
        PlanStatus.SOON -> "Próximo"
        PlanStatus.OK -> "Al día"
        PlanStatus.UNKNOWN -> "Sin datos"
    }
    val progress = item.progress(vehicle, today)

    FixCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeGlyph(item.type, size = 42.dp, iconSize = 19.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildDueLabel(item, vehicle.km, today),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(text = statusLabel, color = statusColor, background = statusBg)
        }
        if (status != PlanStatus.UNKNOWN) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                ThinProgress(
                    progress = progress,
                    color = statusColor,
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        text = lastServiceLabel(item),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

private fun buildDueLabel(item: PlanItem, vehicleKm: Int, today: LocalDate): String {
    val parts = mutableListOf<String>()
    item.dueDate?.let { parts += dueDateLabel(it, today) }
    item.dueKm?.let {
        val left = it - vehicleKm
        parts += if (left > 0) "en ${formatKm(left)} km" else "km superados"
    }
    return if (parts.isEmpty()) "Sin próximo servicio registrado" else parts.joinToString("  ·  ")
}

private fun lastServiceLabel(item: PlanItem): String {
    val date = item.lastDate
    val km = item.lastKm
    return when {
        date != null && km != null -> "Último: ${formatDate(date)} · ${formatKm(km)} km"
        date != null -> "Último: ${formatDate(date)}"
        km != null -> "Último: ${formatKm(km)} km"
        else -> "Sin registro previo"
    }
}
