package com.fixtracker.app.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.BackupManager
import com.fixtracker.app.data.BackupReadResult
import com.fixtracker.app.data.FixRepository
import com.fixtracker.app.data.Settings
import com.fixtracker.app.data.ThemeMode
import com.fixtracker.app.notifications.NotificationHelper
import com.fixtracker.app.notifications.NotificationScheduler
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.ConfirmDialog
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.OptionDialog
import com.fixtracker.app.ui.components.SectionHeader
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography
import com.fixtracker.app.util.CsvExporter
import com.fixtracker.app.util.ReportPdf
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.initialOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    state: AppState,
    repository: FixRepository,
    onUpdateSettings: (Settings) -> Unit,
    onUpdateName: (String) -> Unit,
    onReset: () -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editName by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var currencyDialog by remember { mutableStateOf(false) }
    var budgetDialog by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<BackupReadResult.Ready?>(null) }
    var busy by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(NotificationHelper.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationHelper.canPost(context)
    }

    fun openNotificationSettings() {
        val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = NotificationHelper.canPost(context)
        if (granted) {
            NotificationScheduler.schedule(context)
            infoMessage = "Avisos activados. Te avisaremos de lo importante."
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings()
        }
    }

    fun sendTestNotification() {
        if (NotificationHelper.canPost(context)) {
            NotificationHelper.showTest(context)
            infoMessage = "Aviso de prueba enviado. Si no lo ves, revisa los ajustes del sistema."
        } else {
            requestNotifications()
        }
    }

    val exportZip = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        BackupManager.writeZip(context, repository.exportJson(), output)
                    } ?: error("No se pudo escribir el archivo.")
                }
            }
            busy = false
            infoMessage = result.fold(
                { "Copia completa creada con datos y fotos." },
                { "No se pudo crear la copia: ${it.message}" },
            )
        }
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        BackupManager.writeJson(repository.exportJson(), it)
                    } ?: error("No se pudo escribir el archivo.")
                }
            }
            infoMessage = result.fold(
                { "Datos exportados en JSON." },
                { "No se pudo exportar: ${it.message}" },
            )
        }
    }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(CsvExporter.build(state).toByteArray(Charsets.UTF_8))
                    } ?: error("No se pudo escribir el archivo.")
                }
            }
            infoMessage = result.fold(
                { "Historial exportado en CSV." },
                { "No se pudo exportar: ${it.message}" },
            )
        }
    }

    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                BackupManager.readBackup(context, uri, repository)
            }
            busy = false
            when (result) {
                is BackupReadResult.Ready -> pendingImport = result
                is BackupReadResult.Error -> infoMessage = result.message
            }
        }
    }

    fun writeReport() {
        val vehicle = state.activeVehicle
        if (vehicle == null) {
            infoMessage = "Añade un vehículo para generar un informe."
            return
        }
        scope.launch {
            busy = true
            val file = withContext(Dispatchers.IO) {
                runCatching { ReportPdf.generate(context, state, vehicle) }.getOrNull()
            }
            busy = false
            if (file != null) {
                runCatching { ReportPdf.share(context, file) }
            } else {
                infoMessage = "No se pudo generar el informe."
            }
        }
    }

    val canUseBiometrics = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

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
                    MiniStat("${state.fuelEntries.size}", "repostajes")
                    MiniStat(
                        formatMoney(state.totalSpend, state.settings.currency),
                        "gastos",
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
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
                    icon = Icons.Outlined.Payments,
                    title = "Presupuesto anual",
                    value = state.settings.annualBudget?.let {
                        formatMoney(it, state.settings.currency)
                    } ?: "Sin definir",
                    onClick = { budgetDialog = true },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Avisos",
                    value = if (notificationsEnabled) "Activados" else "Desactivados",
                    onClick = {
                        if (notificationsEnabled) openNotificationSettings() else requestNotifications()
                    },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Enviar aviso de prueba",
                    value = "Probar",
                    onClick = { sendTestNotification() },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = "Informe del vehículo",
                    value = "PDF",
                    onClick = { writeReport() },
                )
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(title = "Datos y copias")
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Download,
                    title = "Copia completa",
                    value = "ZIP con fotos",
                    onClick = { exportZip.launch("FixTracker-copia-${state.vehicles.size}v.zip") },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Upload,
                    title = "Restaurar copia",
                    value = "ZIP o JSON",
                    onClick = {
                        importBackup.launch(
                            arrayOf("application/zip", "application/json", "text/plain", "application/octet-stream")
                        )
                    },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Download,
                    title = "Exportar solo datos",
                    value = "JSON",
                    onClick = { exportJson.launch("FixTracker-datos.json") },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.GridOn,
                    title = "Exportar historial",
                    value = "CSV",
                    onClick = { exportCsv.launch("FixTracker-historial.csv") },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Restablecer datos de ejemplo",
                    value = "",
                    onClick = { confirmReset = true },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Empezar de cero",
                    value = "",
                    tint = FixTheme.palette.danger,
                    onClick = { confirmClear = true },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Cada cambio guarda una copia local automática. Las fotos y los documentos se incluyen en la copia ZIP.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(26.dp))
            SectionHeader(title = "Seguridad")
            Spacer(Modifier.height(10.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Lock,
                    title = "Bloqueo biométrico",
                    subtitle = if (canUseBiometrics) {
                        if (state.settings.biometricLock) "Se pide al abrir la app" else "Desactivado"
                    } else {
                        "No disponible en este dispositivo"
                    },
                    checked = state.settings.biometricLock,
                    enabled = canUseBiometrics,
                    onCheckedChange = { enabled ->
                        onUpdateSettings(state.settings.copy(biometricLock = enabled))
                    },
                )
                Hairline(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Privacidad y datos",
                    value = "",
                    onClick = { aboutDialog = true },
                )
            }
        }

        item {
            Spacer(Modifier.height(34.dp))
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
                    text = "Versión 0.2.0 · Garaje completo",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (busy) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Un momento…", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Preparando el archivo. No cierres la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {},
        )
    }

    if (editName) {
        var name by remember { mutableStateOf(state.ownerName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            shape = RoundedCornerShape(28.dp),
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

    if (budgetDialog) {
        var budgetInput by remember {
            mutableStateOf(
                state.settings.annualBudget?.let {
                    String.format(java.util.Locale.US, "%.0f", it)
                }.orEmpty()
            )
        }
        AlertDialog(
            onDismissRequest = { budgetDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Presupuesto anual", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    Text(
                        text = "Un objetivo orientativo para el gasto del vehículo activo. Incluye servicios y combustible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    AppTextField(
                        value = budgetInput,
                        onValueChange = { input ->
                            budgetInput = input.filter { it.isDigit() || it == ',' || it == '.' }.take(8)
                        },
                        label = "Presupuesto",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        suffix = "€",
                    )
                    if (state.settings.annualBudget != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Quitar presupuesto",
                            style = MaterialTheme.typography.labelMedium,
                            color = FixTheme.palette.copper,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    onUpdateSettings(state.settings.copy(annualBudget = null))
                                    budgetDialog = false
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = budgetInput.replace(',', '.').toDoubleOrNull()
                    onUpdateSettings(
                        state.settings.copy(
                            annualBudget = value?.takeIf { it > 0 },
                        )
                    )
                    budgetDialog = false
                }) { Text("Guardar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { budgetDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "¿Restablecer los datos?",
            message = "Se borrarán tus vehículos, servicios y repostajes actuales y se cargarán de nuevo los datos de ejemplo.",
            confirmLabel = "Restablecer",
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onDismiss = { confirmReset = false },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "¿Empezar de cero?",
            message = "Se borrarán todos los vehículos, servicios, repostajes y avisos de este dispositivo. Podrás añadir tu propio vehículo desde el inicio.",
            confirmLabel = "Borrar todo",
            onConfirm = {
                confirmClear = false
                onClearAll()
            },
            onDismiss = { confirmClear = false },
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Privacidad", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    text = "FixTracker guarda todos tus datos, fotos y copias en este dispositivo. No se envía nada a internet ni se comparte con terceros. Puedes exportar una copia completa en ZIP o solo los datos en JSON.",
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

    pendingImport?.let { pending ->
        ConfirmDialog(
            title = "Restaurar esta copia",
            message = "La copia contiene ${pending.state.vehicles.size} vehículos, ${pending.state.records.size} servicios, ${pending.state.fuelEntries.size} repostajes y ${pending.attachmentCount} fotos. Se reemplazarán los datos actuales.",
            confirmLabel = "Restaurar",
            destructive = false,
            onConfirm = {
                val ready = pending
                pendingImport = null
                scope.launch {
                    busy = true
                    val applied = runCatching {
                        withContext(Dispatchers.IO) {
                            BackupManager.apply(context, ready, repository)
                        }
                    }
                    busy = false
                    if (applied.isSuccess) {
                        NotificationScheduler.runNow(context)
                        infoMessage = "Copia restaurada correctamente."
                    } else {
                        infoMessage = "No se pudo restaurar la copia: ${applied.exceptionOrNull()?.message}"
                    }
                }
            },
            onDismiss = {
                BackupManager.discard(pending)
                pendingImport = null
            },
        )
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("FixTracker", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { infoMessage = null }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.primary)
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
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
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
