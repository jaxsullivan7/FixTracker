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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalGasStation
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
import com.fixtracker.app.data.FuelEntry
import com.fixtracker.app.data.calculateFuelStats
import com.fixtracker.app.ui.components.EmptyState
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.SectionHeader
import com.fixtracker.app.ui.components.SpendBars
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography
import com.fixtracker.app.util.formatConsumption
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatLiters
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.formatMonth
import com.fixtracker.app.util.formatPricePerLiter
import com.fixtracker.app.util.monthShort
import com.fixtracker.app.util.relativePastLabel
import java.time.YearMonth

@Composable
fun FuelScreen(
    state: AppState,
    vehicleId: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val vehicle = state.vehicle(vehicleId)
    if (vehicle == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vehículo no encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val entries = state.fuelFor(vehicleId)
    val stats = calculateFuelStats(entries)
    val now = YearMonth.now()
    val months = (5 downTo 0).map { now.minusMonths(it.toLong()) }
    val monthly = months.map { month ->
        entries.filter { YearMonth.from(it.date) == month }.sumOf { it.totalCost }
    }
    val labels = months.map { monthShort(it) }
    val grouped = entries.groupBy { YearMonth.from(it.date) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = "Combustible",
                onBack = onBack,
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Nuevo repostaje",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = vehicle.fullName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${entries.size} repostajes · ${formatLiters(stats.totalLiters)} · ${formatMoney(stats.totalSpend, state.settings.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(18.dp))
            FuelHero(stats.averageConsumption, stats.lastConsumption, stats.averagePricePerLiter)
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FuelMetric(
                    label = "GASTO ESTE AÑO",
                    value = formatMoney(stats.yearSpend, state.settings.currency),
                    modifier = Modifier.weight(1f),
                )
                FuelMetric(
                    label = "LITROS",
                    value = formatLiters(stats.yearLiters),
                    modifier = Modifier.weight(1f),
                )
                FuelMetric(
                    label = "COSTE / KM",
                    value = stats.costPerKm?.let { String.format(java.util.Locale.forLanguageTag("es-ES"), "%.2f €", it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gasto en combustible",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "6 meses",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    SpendBars(values = monthly, labels = labels)
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                Spacer(Modifier.height(30.dp))
                EmptyState(
                    icon = Icons.Outlined.LocalGasStation,
                    title = "Sin repostajes",
                    message = "Añade el primer repostaje para calcular consumo y coste por kilómetro.",
                )
            }
        } else {
            item {
                Spacer(Modifier.height(28.dp))
                SectionHeader(
                    title = "Repostajes",
                    action = "Añadir",
                    onAction = onAdd,
                )
            }
            grouped.forEach { (month, monthEntries) ->
                item(key = "fuel-month-$month") {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatMonth(month.atDay(1)),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatMoney(monthEntries.sumOf { it.totalCost }, state.settings.currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                }
                item(key = "fuel-card-$month") {
                    FixCard(modifier = Modifier.fillMaxWidth()) {
                        monthEntries.forEachIndexed { index, entry ->
                            FuelRow(
                                entry = entry,
                                currency = state.settings.currency,
                                onClick = { onEdit(entry.id) },
                            )
                            if (index != monthEntries.lastIndex) {
                                Hairline(Modifier.padding(horizontal = 14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelHero(
    average: Double?,
    last: Double?,
    price: Double?,
) {
    val palette = FixTheme.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd))),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalGasStation,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "CONSUMO MEDIO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatConsumption(average),
                style = FixTypography.displaySmall,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Text(
                    text = "Último: ${formatConsumption(last)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.62f),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = formatPricePerLiter(price),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
            if (average == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Marca al menos dos depósitos llenos para calcular el consumo.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun FuelMetric(label: String, value: String, modifier: Modifier = Modifier) {
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
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FuelRow(entry: FuelEntry, currency: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FixTheme.palette.copperSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.LocalGasStation,
                contentDescription = null,
                tint = FixTheme.palette.copper,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.station.ifBlank { "Repostaje" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatLiters(entry.liters)}  ·  ${formatKm(entry.km)} km  ·  ${relativePastLabel(entry.date)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(entry.totalCost, currency),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatPricePerLiter(entry.pricePerLiter),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(17.dp),
        )
    }
}
