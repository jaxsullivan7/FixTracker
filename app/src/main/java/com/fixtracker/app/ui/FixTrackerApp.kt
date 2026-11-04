package com.fixtracker.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.FixRepository
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.ui.screens.FuelFormScreen
import com.fixtracker.app.ui.screens.FuelScreen
import com.fixtracker.app.ui.screens.HistoryScreen
import com.fixtracker.app.ui.screens.HomeScreen
import com.fixtracker.app.ui.screens.PlanScreen
import com.fixtracker.app.ui.screens.RecordDetailScreen
import com.fixtracker.app.ui.screens.RecordFormScreen
import com.fixtracker.app.ui.screens.RemindersScreen
import com.fixtracker.app.ui.screens.SettingsScreen
import com.fixtracker.app.ui.screens.VehicleDetailScreen
import com.fixtracker.app.ui.screens.VehicleFormScreen
import com.fixtracker.app.ui.screens.VehiclesScreen
import com.fixtracker.app.ui.theme.FixTheme
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object Vehicles : Screen
    data object History : Screen
    data object Settings : Screen
    data object Reminders : Screen
    data object AddVehicle : Screen
    data class VehicleDetail(val id: String) : Screen
    data class EditVehicle(val id: String) : Screen
    data class AddRecord(
        val vehicleId: String?,
        val recordId: String? = null,
        val initialType: ServiceType? = null,
    ) : Screen
    data class RecordDetail(val id: String) : Screen
    data class Fuel(val vehicleId: String) : Screen
    data class AddFuel(val vehicleId: String, val entryId: String? = null) : Screen
    data class Plan(val vehicleId: String) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixTrackerApp(
    state: AppState,
    repository: FixRepository,
    initialOpenReminders: Boolean = false,
    onInitialRemindersHandled: () -> Unit = {},
) {
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val current = stack.last()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showQuickAdd by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenReminders) {
        if (initialOpenReminders) {
            stack = listOf(Screen.Home, Screen.Reminders)
            onInitialRemindersHandled()
        }
    }

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun openTab(screen: Screen) {
        stack = listOf(screen)
    }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Deshacer",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    fun deleteRecordWithUndo(recordId: String) {
        val record = state.records.firstOrNull { it.id == recordId } ?: return
        repository.deleteRecord(recordId)
        pop()
        showUndo("Servicio eliminado") { repository.restoreRecord(record) }
    }

    fun deleteFuelWithUndo(entryId: String) {
        val entry = state.fuelEntries.firstOrNull { it.id == entryId } ?: return
        repository.deleteFuelEntry(entryId)
        pop()
        showUndo("Repostaje eliminado") { repository.addFuelEntry(entry) }
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val isTab = current is Screen.Home || current is Screen.Vehicles ||
        current is Screen.History || current is Screen.Settings

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isTab) {
                FixBottomBar(
                    current = current,
                    onSelect = { openTab(it) },
                    onAdd = {
                        val active = state.activeVehicle
                        if (active == null) {
                            openTab(Screen.Vehicles)
                            push(Screen.AddVehicle)
                        } else {
                            showQuickAdd = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(140))
            },
            label = "screen",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { screen ->
            when (screen) {
                is Screen.Home -> HomeScreen(
                    state = state,
                    onOpenVehicle = { push(Screen.VehicleDetail(it)) },
                    onOpenRecord = { push(Screen.RecordDetail(it)) },
                    onAddRecord = {
                        state.activeVehicle?.let { push(Screen.AddRecord(it.id)) }
                    },
                    onAddFuel = { push(Screen.AddFuel(it)) },
                    onOpenFuel = { push(Screen.Fuel(it)) },
                    onUpdateKm = { km ->
                        state.activeVehicle?.let { repository.updateKm(it.id, km) }
                    },
                    onOpenReminders = { push(Screen.Reminders) },
                    onOpenHistory = { openTab(Screen.History) },
                    onOpenSettings = { openTab(Screen.Settings) },
                    onAddVehicle = {
                        openTab(Screen.Vehicles)
                        push(Screen.AddVehicle)
                    },
                    onSetActive = { repository.setActiveVehicle(it) },
                )

                is Screen.Vehicles -> VehiclesScreen(
                    state = state,
                    onOpenVehicle = { push(Screen.VehicleDetail(it)) },
                    onAddVehicle = { push(Screen.AddVehicle) },
                )

                is Screen.History -> HistoryScreen(
                    state = state,
                    onOpenRecord = { push(Screen.RecordDetail(it)) },
                )

                is Screen.Settings -> SettingsScreen(
                    state = state,
                    repository = repository,
                    onUpdateSettings = { repository.updateSettings(it) },
                    onUpdateName = { repository.updateOwnerName(it) },
                    onReset = { repository.resetDemo() },
                    onClearAll = { repository.clearAll() },
                )

                is Screen.Reminders -> RemindersScreen(
                    state = state,
                    onBack = { pop() },
                    onToggle = { repository.toggleReminder(it) },
                    onSnooze = { id, until -> repository.snoozeReminder(id, until) },
                    onAdd = { repository.addReminder(it) },
                )

                is Screen.VehicleDetail -> VehicleDetailScreen(
                    state = state,
                    vehicleId = screen.id,
                    onBack = { pop() },
                    onEdit = { push(Screen.EditVehicle(it)) },
                    onAddRecord = { push(Screen.AddRecord(it)) },
                    onOpenRecord = { push(Screen.RecordDetail(it)) },
                    onOpenReminders = { push(Screen.Reminders) },
                    onOpenHistory = { openTab(Screen.History) },
                    onSetActive = { repository.setActiveVehicle(it) },
                    onDelete = {
                        repository.deleteVehicle(it)
                        pop()
                    },
                    onOpenPlan = { push(Screen.Plan(it)) },
                    onOpenFuel = { push(Screen.Fuel(it)) },
                    onAddFuel = { push(Screen.AddFuel(it)) },
                )

                is Screen.Plan -> PlanScreen(
                    state = state,
                    vehicleId = screen.vehicleId,
                    onBack = { pop() },
                    onAddRecord = { type ->
                        push(Screen.AddRecord(screen.vehicleId, initialType = type))
                    },
                )

                is Screen.Fuel -> FuelScreen(
                    state = state,
                    vehicleId = screen.vehicleId,
                    onBack = { pop() },
                    onAdd = { push(Screen.AddFuel(screen.vehicleId)) },
                    onEdit = { push(Screen.AddFuel(screen.vehicleId, it)) },
                )

                is Screen.AddFuel -> FuelFormScreen(
                    state = state,
                    initialVehicleId = screen.vehicleId,
                    editingEntryId = screen.entryId,
                    onClose = { pop() },
                    onSave = { entry ->
                        if (state.fuelEntries.any { it.id == entry.id }) {
                            repository.updateFuelEntry(entry)
                        } else {
                            repository.addFuelEntry(entry)
                        }
                        pop()
                    },
                    onDelete = { deleteFuelWithUndo(it) },
                )

                is Screen.AddRecord -> RecordFormScreen(
                    state = state,
                    initialVehicleId = screen.vehicleId,
                    editingRecordId = screen.recordId,
                    initialType = screen.initialType,
                    onClose = { pop() },
                    onSave = { record ->
                        if (state.records.any { it.id == record.id }) {
                            repository.updateRecord(record)
                        } else {
                            repository.addRecord(record)
                        }
                        pop()
                    },
                )

                is Screen.RecordDetail -> RecordDetailScreen(
                    state = state,
                    recordId = screen.id,
                    onBack = { pop() },
                    onEdit = { id ->
                        val record = state.records.firstOrNull { it.id == id }
                        if (record != null) {
                            push(Screen.AddRecord(record.vehicleId, recordId = record.id))
                        }
                    },
                    onDelete = { deleteRecordWithUndo(it) },
                )

                is Screen.AddVehicle -> VehicleFormScreen(
                    state = state,
                    vehicleId = null,
                    onClose = { pop() },
                    onSave = {
                        repository.addVehicle(it)
                        pop()
                    },
                )

                is Screen.EditVehicle -> VehicleFormScreen(
                    state = state,
                    vehicleId = screen.id,
                    onClose = { pop() },
                    onSave = {
                        repository.updateVehicle(it)
                        pop()
                    },
                )
            }
        }
    }

    if (showQuickAdd) {
        QuickAddSheet(
            onDismiss = { showQuickAdd = false },
            onService = {
                showQuickAdd = false
                state.activeVehicle?.let { push(Screen.AddRecord(it.id)) }
            },
            onFuel = {
                showQuickAdd = false
                state.activeVehicle?.let { push(Screen.AddFuel(it.id)) }
            },
            onReminder = {
                showQuickAdd = false
                push(Screen.Reminders)
            },
            onVehicle = {
                showQuickAdd = false
                openTab(Screen.Vehicles)
                push(Screen.AddVehicle)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    onDismiss: () -> Unit,
    onService: () -> Unit,
    onFuel: () -> Unit,
    onReminder: () -> Unit,
    onVehicle: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
        ) {
            Text(
                text = "¿Qué quieres registrar?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAddTile(
                    label = "Servicio",
                    description = "Taller o mantenimiento",
                    icon = Icons.Outlined.Build,
                    onClick = onService,
                    modifier = Modifier.weight(1f),
                )
                QuickAddTile(
                    label = "Repostaje",
                    description = "Litros y precio",
                    icon = Icons.Outlined.LocalGasStation,
                    onClick = onFuel,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAddTile(
                    label = "Recordatorio",
                    description = "Una fecha o un km",
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onReminder,
                    modifier = Modifier.weight(1f),
                )
                QuickAddTile(
                    label = "Vehículo",
                    description = "Añadir al garaje",
                    icon = Icons.Outlined.DirectionsCar,
                    onClick = onVehicle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QuickAddTile(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FixTheme.palette.surfaceAlt)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FixBottomBar(
    current: Screen,
    onSelect: (Screen) -> Unit,
    onAdd: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, FixTheme.palette.hairline, RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavItem(
                label = "Inicio",
                icon = Icons.Outlined.Home,
                selected = current is Screen.Home,
                onClick = { onSelect(Screen.Home) },
                modifier = Modifier.weight(1f),
            )
            NavItem(
                label = "Garaje",
                icon = Icons.Outlined.DirectionsCar,
                selected = current is Screen.Vehicles,
                onClick = { onSelect(Screen.Vehicles) },
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(10.dp, CircleShape, ambientColor = FixTheme.palette.copper.copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(FixTheme.palette.copper)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Añadir",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
            NavItem(
                label = "Historial",
                icon = Icons.Outlined.History,
                selected = current is Screen.History,
                onClick = { onSelect(Screen.History) },
                modifier = Modifier.weight(1f),
            )
            NavItem(
                label = "Ajustes",
                icon = Icons.Outlined.Settings,
                selected = current is Screen.Settings,
                onClick = { onSelect(Screen.Settings) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp, letterSpacing = 0.sp),
            color = color,
        )
    }
}
