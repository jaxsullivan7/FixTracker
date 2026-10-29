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
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.fixtracker.app.data.Settings
import com.fixtracker.app.ui.screens.HistoryScreen
import com.fixtracker.app.ui.screens.HomeScreen
import com.fixtracker.app.ui.screens.RecordDetailScreen
import com.fixtracker.app.ui.screens.RecordFormScreen
import com.fixtracker.app.ui.screens.RemindersScreen
import com.fixtracker.app.ui.screens.SettingsScreen
import com.fixtracker.app.ui.screens.VehicleDetailScreen
import com.fixtracker.app.ui.screens.VehicleFormScreen
import com.fixtracker.app.ui.screens.VehiclesScreen
import com.fixtracker.app.ui.theme.FixTheme

sealed interface Screen {
    data object Home : Screen
    data object Vehicles : Screen
    data object History : Screen
    data object Settings : Screen
    data object Reminders : Screen
    data object AddVehicle : Screen
    data class VehicleDetail(val id: String) : Screen
    data class EditVehicle(val id: String) : Screen
    data class AddRecord(val vehicleId: String?) : Screen
    data class RecordDetail(val id: String) : Screen
}

@Composable
fun FixTrackerApp(
    state: AppState,
    repository: FixRepository,
) {
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val current = stack.last()

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun openTab(screen: Screen) {
        stack = listOf(screen)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val isTab = current is Screen.Home || current is Screen.Vehicles ||
        current is Screen.History || current is Screen.Settings

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTab) {
                FixBottomBar(
                    current = current,
                    onSelect = { openTab(it) },
                    onAdd = {
                        val target = state.activeVehicle?.id
                        if (target == null) {
                            openTab(Screen.Vehicles)
                            push(Screen.AddVehicle)
                        } else {
                            push(Screen.AddRecord(target))
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
                    onUpdateKm = { km ->
                        state.activeVehicle?.let { repository.updateKm(it.id, km) }
                    },
                    onOpenReminders = { push(Screen.Reminders) },
                    onOpenHistory = { openTab(Screen.History) },
                    onOpenSettings = { openTab(Screen.Settings) },
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
                    onUpdateSettings = { repository.updateSettings(it) },
                    onUpdateName = { repository.updateOwnerName(it) },
                    onReset = { repository.resetDemo() },
                    exportJson = { repository.exportJson() },
                )

                is Screen.Reminders -> RemindersScreen(
                    state = state,
                    onBack = { pop() },
                    onToggle = { repository.toggleReminder(it) },
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
                )

                is Screen.AddRecord -> RecordFormScreen(
                    state = state,
                    initialVehicleId = screen.vehicleId,
                    onClose = { pop() },
                    onSave = {
                        repository.addRecord(it)
                        pop()
                    },
                )

                is Screen.RecordDetail -> RecordDetailScreen(
                    state = state,
                    recordId = screen.id,
                    onBack = { pop() },
                    onDelete = {
                        repository.deleteRecord(it)
                        pop()
                    },
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
                        contentDescription = "Añadir servicio",
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
