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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.ServiceRecord
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.ui.components.EmptyState
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.PillButton
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.formatMonth
import com.fixtracker.app.util.relativePastLabel
import java.time.YearMonth

@Composable
fun HistoryScreen(
    state: AppState,
    onOpenRecord: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<ServiceType?>(null) }

    val allRecords = state.records.sortedByDescending { it.date }
    val filtered = allRecords.filter { record ->
        val matchesType = typeFilter == null || record.type == typeFilter
        val vehicle = state.vehicle(record.vehicleId)
        val haystack = "${record.title} ${record.shop} ${record.notes} ${vehicle?.fullName.orEmpty()} ${vehicle?.plate.orEmpty()}"
        matchesType && (query.isBlank() || haystack.contains(query.trim(), ignoreCase = true))
    }
    val grouped = filtered.groupBy { YearMonth.from(it.date) }
    val total = filtered.sumOf { it.cost }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
    ) {
        item {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${allRecords.size} servicios  ·  ${formatMoney(state.records.sumOf { it.cost }, state.settings.currency)} invertidos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Buscar servicio, taller o vehículo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                },
                trailingIcon = if (query.isNotBlank()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Borrar búsqueda",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = FixTheme.palette.hairline,
                ),
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    PillButton(
                        text = "Todo",
                        onClick = { typeFilter = null },
                        selected = typeFilter == null,
                    )
                }
                items(ServiceType.entries.toList()) { type ->
                    PillButton(
                        text = type.shortLabel,
                        onClick = { typeFilter = if (typeFilter == type) null else type },
                        selected = typeFilter == type,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Spacer(Modifier.height(40.dp))
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "Sin resultados",
                    message = "No hay servicios que coincidan con tu búsqueda.",
                )
            }
        }

        grouped.forEach { (month, records) ->
            item(key = "header-$month") {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatMonth(month.atDay(1)),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatMoney(records.sumOf { it.cost }, state.settings.currency),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            item(key = "card-$month") {
                FixCard(modifier = Modifier.fillMaxWidth()) {
                    records.forEachIndexed { index, record ->
                        HistoryRow(
                            record = record,
                            vehicleName = state.vehicle(record.vehicleId)?.fullName.orEmpty(),
                            currency = state.settings.currency,
                            onClick = { onOpenRecord(record.id) },
                        )
                        if (index != records.lastIndex) {
                            Hairline(Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    record: ServiceRecord,
    vehicleName: String,
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
                    if (vehicleName.isNotBlank()) {
                        append(vehicleName)
                        append("  ·  ")
                    }
                    append(relativePastLabel(record.date))
                    if (record.km > 0) {
                        append("  ·  ")
                        append(formatKm(record.km))
                        append(" km")
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatMoney(record.cost, currency),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
