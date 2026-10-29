package com.fixtracker.app.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.Settings
import com.fixtracker.app.data.ThemeMode
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.ConfirmDialog
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.OptionDialog
import com.fixtracker.app.ui.components.SectionHeader
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.initialOf

@Composable
fun SettingsScreen(
    state: AppState,
    onUpdateSettings: (Settings) -> Unit,
    onUpdateName: (String) -> Unit,
    onReset: () -> Unit,
    exportJson: () -> String,
) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var currencyDialog by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
    ) {
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Haz FixTracker tuyo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(18.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editName = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.ownerName.isBlank()) "FT" else initialOf(state.ownerName),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.ownerName.ifBlank { "Tu perfil" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Toca para cambiar tu nombre",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(19.dp),
                    )
                }
                Hairline(Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MiniStat("${state.vehicles.size}", "vehículos")
                    MiniStat("${state.records.size}", "servicios")
                    MiniStat(
                        formatMoney(state.records.sumOf { it.cost }, state.settings.currency),
                        "invertidos",
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader(title = "Preferencias")
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Tema",
                    value = state.settings.theme.label,
                    onClick = { themeDialog = true },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Payments,
                    title = "Moneda",
                    value = state.settings.currency,
                    onClick = { currencyDialog = true },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Download,
                    title = "Exportar datos",
                    value = "JSON",
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Copia de seguridad de FixTracker")
                            putExtra(Intent.EXTRA_TEXT, exportJson())
                        }
                        context.startActivity(Intent.createChooser(send, "Exportar datos de FixTracker"))
                    },
                )
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader(title = "Datos")
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Restablecer datos de ejemplo",
                    value = "",
                    tint = FixTheme.palette.danger,
                    onClick = { confirmReset = true },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Sobre la privacidad",
                    value = "",
                    onClick = { aboutDialog = true },
                )
            }
        }

        item {
            Spacer(Modifier.height(36.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FixTheme.palette.copper)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "FIXTRACKER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Mantenimiento sin ruido.",
                    style = FixTypography.headlineSmall.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Versión 0.1.0 · Primer boceto",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (editName) {
        var name by remember { mutableStateOf(state.ownerName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Tu nombre", style = MaterialTheme.typography.headlineSmall) },
            text = {
                AppTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = "Nombre",
                    placeholder = "Cómo quieres que te salude",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateName(name)
                    editName = false
                }) { Text("Guardar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (themeDialog) {
        OptionDialog(
            title = "Tema",
            options = ThemeMode.entries.map { it.name to it.label },
            selected = state.settings.theme.name,
            onDismiss = { themeDialog = false },
            onSelect = { key ->
                ThemeMode.entries.firstOrNull { it.name == key }?.let {
                    onUpdateSettings(state.settings.copy(theme = it))
                }
                themeDialog = false
            },
        )
    }

    if (currencyDialog) {
        OptionDialog(
            title = "Moneda",
            options = listOf(
                "EUR" to "Euro (€)",
                "USD" to "Dólar ($)",
                "GBP" to "Libra (£)",
            ),
            selected = state.settings.currency,
            onDismiss = { currencyDialog = false },
            onSelect = {
                onUpdateSettings(state.settings.copy(currency = it))
                currencyDialog = false
            },
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "¿Restablecer los datos?",
            message = "Se borrarán tus vehículos y servicios actuales y se cargarán de nuevo los datos de ejemplo.",
            confirmLabel = "Restablecer",
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onDismiss = { confirmReset = false },
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Privacidad", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    text = "FixTracker guarda todos tus datos en este dispositivo. No se envía ningún dato a internet ni se comparte con terceros. Puedes exportar una copia en JSON desde los ajustes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { aboutDialog = false }) {
                    Text("Entendido", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(FixTheme.palette.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint != null) tint else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
