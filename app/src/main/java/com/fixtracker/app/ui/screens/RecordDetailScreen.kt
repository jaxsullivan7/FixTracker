package com.fixtracker.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fixtracker.app.data.AppState
import com.fixtracker.app.ui.components.ConfirmDialog
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Hairline
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.TypeGlyph
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography
import com.fixtracker.app.util.AttachmentStore
import com.fixtracker.app.util.formatDate
import com.fixtracker.app.util.formatKm
import com.fixtracker.app.util.formatMoney
import com.fixtracker.app.util.relativePastLabel

@Composable
fun RecordDetailScreen(
    state: AppState,
    recordId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val record = state.records.firstOrNull { it.id == recordId }
    if (record == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Servicio no encontrado",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val vehicle = state.vehicle(record.vehicleId)
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = "Detalle del servicio",
                onBack = onBack,
                trailing = {
                    Row {
                        IconButton(onClick = { onEdit(record.id) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Editar servicio",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Eliminar servicio",
                                tint = FixTheme.palette.danger,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )
        }

        item {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TypeGlyph(record.type, size = 64.dp, iconSize = 28.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = record.type.shortLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = FixTheme.palette.copper,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = record.title,
                    style = FixTypography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(vehicle?.fullName, relativePastLabel(record.date))
                        .joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(FixTheme.palette.surfaceAlt)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "COSTE DEL SERVICIO",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatMoney(record.cost, state.settings.currency),
                        style = FixTypography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatKm(record.km),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "kilómetros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (record.attachmentNames.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fotos y facturas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${record.attachmentNames.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(record.attachmentNames, key = { it }) { name ->
                        val bitmap = remember(name) { AttachmentStore.loadBitmap(context, name, 420) }
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(FixTheme.palette.surfaceAlt)
                                .clickable { viewer = name },
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto del servicio",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(label = "Fecha", value = formatDate(record.date))
                Hairline(Modifier.padding(horizontal = 16.dp))
                InfoRow(label = "Kilometraje", value = "${formatKm(record.km)} km")
                Hairline(Modifier.padding(horizontal = 16.dp))
                InfoRow(
                    label = "Vehículo",
                    value = vehicle?.fullName ?: "—",
                )
                if (record.shop.isNotBlank()) {
                    Hairline(Modifier.padding(horizontal = 16.dp))
                    InfoRow(label = "Taller", value = record.shop)
                }
                if (record.notes.isNotBlank()) {
                    Hairline(Modifier.padding(horizontal = 16.dp))
                    InfoRow(label = "Notas", value = record.notes, multiline = true)
                }
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
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
                    text = "Eliminar servicio",
                    style = MaterialTheme.typography.labelMedium,
                    color = FixTheme.palette.danger,
                )
            }
        }
    }

    viewer?.let { name ->
        Dialog(
            onDismissRequest = { viewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                val bitmap = remember(name) { AttachmentStore.loadBitmap(context, name, 1800) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto del servicio",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                } else {
                    Text(
                        text = "No se pudo abrir la foto.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(22.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { viewer = null },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar foto",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "¿Eliminar este servicio?",
            message = "El registro desaparecerá del historial. Podrás deshacerlo justo después.",
            onConfirm = {
                confirmDelete = false
                onDelete(record.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, multiline: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}
