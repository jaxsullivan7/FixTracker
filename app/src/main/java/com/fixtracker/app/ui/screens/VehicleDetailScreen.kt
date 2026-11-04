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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.HealthStatus
import com.fixtracker.app.data.PlanItem
import com.fixtracker.app.data.PlanStatus
import com.fixtracker.app.data.ServiceRecord
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.data.Vehicle
import com.fixtracker.app.data.buildMaintenancePlan
import com.fixtracker.app.data.calculateFuelStats
import com.fixtracker.app.data.healthFor
import com.fixtracker.app.data.mileageStatsFor
import com.fixtracker.app.data.spendByTypeFor
import com.fixtracker.app.ui.components.ConfirmDialog
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.KmSparkline
import com.fixtracker.app.ui.components.PillButton
import com.fixtracker.app.ui.components.PrimaryButton
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.SectionHeader
import com.fixtracker.app.ui.components.StatusChip
import com.fixtracker.app.ui.components.ThinProgress
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography
import com.fixtracker.app.util.ReportPdf
import com.fixtracker.app.util.daysUntil
import com.fixtracker.app.util.dueDateLabel
import com.fixtracker.app.util.formatConsumption
import com.fixtracker.app.util.formatDate
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatLiters
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.relativePastLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun VehicleDetailScreen(
    state: AppState,
    vehicleId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAddRecord: (String) -> Unit,
    onOpenRecord: (String) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
    onSetActive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenPlan: (String) -> Unit,
    onOpenFuel: (String) -> Unit,
    onAddFuel: (String) -> Unit,
) {
    val vehicle = state.vehicle(vehicleId)
    if (vehicle == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Vehículo no encontrado",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val records = state.recordsFor(vehicle.id)
    val reminders = state.remindersFor(vehicle.id)
    val fuel = state.fuelFor(vehicle.id)
    val fuelStats = calculateFuelStats(fuel)
    val mileage = state.mileageStatsFor(vehicle.id)
    val plan = buildMaintenancePlan(state, vehicle)
    val health = state.healthFor(vehicle.id)
    val isActive = state.activeVehicleId == vehicle.id
    val year = LocalDate.now().year
    val yearFrom = LocalDate.of(year, 1, 1)
    val yearTo = yearFrom.plusYears(1)
    val yearSpend = state.spendBetween(vehicle.id, yearFrom, yearTo)
    val totalSpend = state.spendFor(vehicle.id)
    var confirmDelete by remember { mutableStateOf(false) }
    var generatingReport by remember { mutableStateOf(false) }

    fun shareReport() {
        if (generatingReport) return
        scope.launch {
            generatingReport = true
            val file = withContext(Dispatchers.IO) {
                runCatching { ReportPdf.generate(context, state, vehicle) }.getOrNull()
            }
            generatingReport = false
            if (file != null) {
                runCatching { ReportPdf.share(context, file) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = "Ficha del vehículo",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { onEdit(vehicle.id) }) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Editar vehículo",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            DetailHero(vehicle, health)
        }

        item {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatTile(
                    label = "GASTO TOTAL",
                    value = formatMoney(totalSpend, state.settings.currency),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "SERVICIOS",
                    value = records.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "MEDIA",
                    value = mileage.kmPerMonth?.let { "${formatKm(it.roundToInt())} km/mes" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (!isActive) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                        .clickable { onSetActive(vehicle.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Establecer como vehículo activo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kilometraje",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = vehicle.kmUpdatedAt?.let { "Actualizado ${relativePastLabel(it).lowercase()}" }
                                ?: "Sin actualizar",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatKm(vehicle.km),
                            style = FixTypography.headlineLarge.copy(fontSize = 30.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        mileage.kmPerMonth?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = FixTheme.palette.copper,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${formatKm(it.roundToInt())} km/mes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FixTheme.palette.copper,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    KmSparkline(
                        values = mileage.points.map { it.second }.takeLast(30),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    )
                    if (mileage.points.size < 2) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Añade servicios o repostajes para estimar tu uso mensual.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(title = "Documentación")
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                DocumentRow(
                    icon = Icons.AutoMirrored.Outlined.FactCheck,
                    title = "Inspección técnica",
                    date = vehicle.itvUntil,
                    accent = FixTheme.palette.gold,
                )
                Hairline(Modifier.padding(horizontal = 14.dp))
                DocumentRow(
                    icon = Icons.Outlined.Shield,
                    title = "Seguro",
                    date = vehicle.insuranceUntil,
                    accent = FixTheme.palette.blue,
                )
                Hairline(Modifier.padding(horizontal = 14.dp))
                DocumentRow(
                    icon = Icons.AutoMirrored.Outlined.FactCheck,
                    title = "Impuesto de circulación",
                    date = vehicle.roadTaxUntil,
                    accent = FixTheme.palette.positive,
                )
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(
                title = "Plan de mantenimiento",
                action = "Ver plan",
                onAction = { onOpenPlan(vehicle.id) },
            )
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                val upcoming = plan.take(3)
                upcoming.forEachIndexed { index, item ->
                    PlanPreviewRow(
                        item = item,
                        vehicle = vehicle,
                        onClick = { onAddRecord(vehicle.id) },
                    )
                    if (index != upcoming.lastIndex) Hairline(Modifier.padding(horizontal = 14.dp))
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(
                title = "Combustible",
                action = if (fuel.isEmpty()) null else "Ver todo",
                onAction = { onOpenFuel(vehicle.id) },
            )
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "CONSUMO MEDIO",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatConsumption(fuelStats.averageConsumption),
                                style = FixTypography.headlineLarge.copy(fontSize = 26.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = fuelStats.lastConsumption?.let { "Último: ${formatConsumption(it)}" }
                                    ?: "Sin depósitos completos",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatMoney(fuelStats.yearSpend, state.settings.currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "este año · ${formatLiters(fuelStats.yearLiters)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onAddFuel(vehicle.id) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.LocalGasStation,
                                    contentDescription = null,
                                    tint = FixTheme.palette.copper,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Repostar",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                                    color = FixTheme.palette.copper,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(title = "Gastos del año")
            Spacer(Modifier.height(10.dp))
            SpendingBreakdown(
                state = state,
                vehicle = vehicle,
                year = year,
                total = yearSpend,
                budget = state.settings.annualBudget,
            )
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(
                title = "Avisos",
                action = if (reminders.isNotEmpty()) "Ver todo" else null,
                onAction = onOpenReminders,
            )
            Spacer(Modifier.height(10.dp))
            if (reminders.isEmpty()) {
                FixCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Sin avisos pendientes para este vehículo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                FixCard(modifier = Modifier.fillMaxWidth()) {
                    reminders.take(3).forEachIndexed { index, reminder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenReminders)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TypeGlyph(reminder.type, size = 38.dp, iconSize = 17.dp)
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val subtitle = buildString {
                                    reminder.dueDate?.let { append(dueDateLabel(it)) }
                                    reminder.dueKm?.let {
                                        if (isNotEmpty()) append("  ·  ")
                                        append("${formatKm(it)} km")
                                    }
                                }
                                if (subtitle.isNotEmpty()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        if (index != reminders.take(3).lastIndex) Hairline(Modifier.padding(horizontal = 14.dp))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(
                title = "Historial",
                action = if (records.isNotEmpty()) "Ver todo" else null,
                onAction = onOpenHistory,
            )
            Spacer(Modifier.height(10.dp))
            if (records.isEmpty()) {
                FixCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Todavía no hay servicios registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                FixCard(modifier = Modifier.fillMaxWidth()) {
                    records.take(4).forEachIndexed { index, record ->
                        DetailRecordRow(
                            record = record,
                            currency = state.settings.currency,
                            onClick = { onOpenRecord(record.id) },
                        )
                        if (index != records.take(4).lastIndex) Hairline(Modifier.padding(horizontal = 14.dp))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = "Registrar servicio",
                icon = Icons.Outlined.Build,
                onClick = { onAddRecord(vehicle.id) },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                PillButton(
                    text = "Añadir repostaje",
                    icon = Icons.Outlined.LocalGasStation,
                    onClick = { onAddFuel(vehicle.id) },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = if (generatingReport) "Generando…" else "Informe PDF",
                    icon = Icons.Outlined.PictureAsPdf,
                    onClick = { shareReport() },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
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
                    text = "Eliminar vehículo",
                    style = MaterialTheme.typography.labelMedium,
                    color = FixTheme.palette.danger,
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "¿Eliminar ${vehicle.fullName}?",
            message = "Se borrarán también todos sus servicios, repostajes, fotos y avisos. Esta acción no se puede deshacer.",
            onConfirm = {
                confirmDelete = false
                onDelete(vehicle.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DetailHero(vehicle: Vehicle, health: HealthStatus) {
    val palette = FixTheme.palette
    val days = vehicle.daysToService()
    val kmLeft = vehicle.kmToService()
    val progress = kmLeft?.let {
        1f - (it.toFloat() / vehicle.serviceKmInterval).coerceIn(0f, 1f)
    } ?: 0f
    val healthLabel = when (health) {
        HealthStatus.OVERDUE -> "Vencido"
        HealthStatus.SOON -> "Próximo"
        HealthStatus.OK -> "Al día"
    }
    val healthColor = when (health) {
        HealthStatus.OVERDUE -> palette.danger
        HealthStatus.SOON -> palette.copper
        HealthStatus.OK -> palette.positive
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd))),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = vehicle.type.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = Color.White.copy(alpha = 0.55f),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(healthColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = healthLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp),
                        color = Color.White,
                    )
                }
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
            Spacer(Modifier.height(13.dp))
            Text(
                text = vehicle.fullName,
                style = FixTypography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(vehicle.year.toString(), vehicle.fuel.label, vehicle.colorName)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "KILOMETRAJE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${formatKm(vehicle.km)} km",
                        style = FixTypography.headlineLarge.copy(fontSize = 28.sp),
                        color = Color.White,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PRÓXIMO SERVICIO",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = days?.let { dueDateLabel(LocalDate.now().plusDays(it)) } ?: "Sin fecha",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.copper),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = kmLeft?.let { "Aceite a ${formatKm(it)} km" } ?: "Sin plan de servicio",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))
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
private fun DocumentRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    date: LocalDate?,
    accent: Color,
) {
    val palette = FixTheme.palette
    val days = date?.let { daysUntil(it) }
    val (chipText, chipColor, chipBg) = when {
        date == null -> Triple("Sin fecha", MaterialTheme.colorScheme.onSurfaceVariant, palette.surfaceAlt)
        days != null && days < 0 -> Triple("Vencido", palette.danger, palette.dangerSoft)
        days != null && days <= 30 -> Triple(dueDateLabel(date), palette.copper, palette.copperSoft)
        else -> Triple("Vigente", palette.positive, palette.positiveSoft)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (palette.dark) 0.2f else 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = date?.let { formatDate(it) } ?: "Añade una fecha",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusChip(text = chipText, color = chipColor, background = chipBg)
    }
}

@Composable
private fun DetailRecordRow(
    record: ServiceRecord,
    currency: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeGlyph(record.type, size = 38.dp, iconSize = 17.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${relativePastLabel(record.date)}  ·  ${formatKm(record.km)} km",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatMoney(record.cost, currency),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PlanPreviewRow(item: PlanItem, vehicle: Vehicle, onClick: () -> Unit) {
    val palette = FixTheme.palette
    val status = item.status(vehicle)
    val label = when (status) {
        PlanStatus.OVERDUE -> "Vencido"
        PlanStatus.SOON -> "Próximo"
        PlanStatus.OK -> "Al día"
        PlanStatus.UNKNOWN -> "Sin datos"
    }
    val color = when (status) {
        PlanStatus.OVERDUE -> palette.danger
        PlanStatus.SOON -> palette.copper
        PlanStatus.OK -> palette.positive
        PlanStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bg = when (status) {
        PlanStatus.OVERDUE -> palette.dangerSoft
        PlanStatus.SOON -> palette.copperSoft
        PlanStatus.OK -> palette.positiveSoft
        PlanStatus.UNKNOWN -> palette.surfaceAlt
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeGlyph(item.type, size = 38.dp, iconSize = 17.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildString {
                    item.dueDate?.let { append(dueDateLabel(it)) }
                    item.dueKm?.let {
                        if (isNotEmpty()) append("  ·  ")
                        val left = it - vehicle.km
                        append(if (left > 0) "en ${formatKm(left)} km" else "km superados")
                    }
                }.ifBlank { "Sin próximo servicio" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusChip(text = label, color = color, background = bg)
    }
}

@Composable
private fun SpendingBreakdown(
    state: AppState,
    vehicle: Vehicle,
    year: Int,
    total: Double,
    budget: Double?,
) {
    val fuel = state.fuelFor(vehicle.id).filter { it.date.year == year }.sumOf { it.totalCost }
    val byType = state.spendByTypeFor(vehicle.id, year)
    val maintenance = listOf(
        ServiceType.ACEITE,
        ServiceType.FILTROS,
        ServiceType.FRENOS,
        ServiceType.NEUMATICOS,
        ServiceType.REVISION,
        ServiceType.REPARACION,
    ).sumOf { byType[it] ?: 0.0 }
    val rows = listOf(
        "Combustible" to fuel,
        "Mantenimiento" to maintenance,
        "Seguro" to (byType[ServiceType.SEGURO] ?: 0.0),
        "ITV" to (byType[ServiceType.ITV] ?: 0.0),
        "Otros" to (byType[ServiceType.OTRO] ?: 0.0),
    ).filter { it.second > 0.0 }.sortedByDescending { it.second }

    FixCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL ${year}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatMoney(total, state.settings.currency),
                        style = FixTypography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Servicios + combustible",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rows.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Aún no hay gastos este año.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val max = rows.maxOf { it.second }
                Spacer(Modifier.height(16.dp))
                rows.take(5).forEachIndexed { index, (label, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(100.dp),
                        )
                        ThinProgress(
                            progress = (amount / max).toFloat(),
                            color = when (index) {
                                0 -> FixTheme.palette.copper
                                1 -> MaterialTheme.colorScheme.primary
                                else -> FixTheme.palette.blue
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = formatMoney(amount, state.settings.currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(70.dp),
                        )
                    }
                    if (index != rows.take(5).lastIndex) Spacer(Modifier.height(9.dp))
                }
            }

            if (budget != null && budget > 0) {
                Spacer(Modifier.height(14.dp))
                Hairline()
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Presupuesto anual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatMoney(budget, state.settings.currency),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(7.dp))
                ThinProgress(
                    progress = (total / budget).toFloat().coerceIn(0f, 1f),
                    color = if (total > budget) FixTheme.palette.danger else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (total > budget) {
                        "Superado por ${formatMoney(total - budget, state.settings.currency)}"
                    } else {
                        "Te quedan ${formatMoney(budget - total, state.settings.currency)}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = if (total > budget) FixTheme.palette.danger
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
