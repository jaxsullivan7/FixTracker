package com.fixtracker.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.data.AppState
import com.fixtracker.app.data.ServiceRecord
import com.fixtracker.app.data.ServiceType
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.DateField
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.PillButton
import com.fixtracker.app.ui.components.PrimaryButton
import com.fixtracker.app.ui.components.ScreenHeader
import com.fixtracker.app.ui.components.serviceColor
import com.fixtracker.app.ui.components.serviceIcon
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.util.AttachmentStore
import com.fixtracker.app.util.formatKm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

@Composable
fun RecordFormScreen(
    state: AppState,
    initialVehicleId: String?,
    editingRecordId: String? = null,
    initialType: ServiceType? = null,
    onClose: () -> Unit,
    onSave: (ServiceRecord) -> Unit,
) {
    val existing = editingRecordId?.let { id -> state.records.firstOrNull { it.id == id } }
    val defaultVehicle = state.vehicle(existing?.vehicleId ?: initialVehicleId ?: state.activeVehicleId)
        ?: state.activeVehicle
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var vehicleId by remember(existing?.id) {
        mutableStateOf(defaultVehicle?.id ?: state.vehicles.firstOrNull()?.id.orEmpty())
    }
    val vehicle = state.vehicle(vehicleId)
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: initialType ?: ServiceType.ACEITE) }
    var title by remember(existing?.id) {
        mutableStateOf(existing?.title ?: (initialType ?: ServiceType.ACEITE).label)
    }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var km by remember(existing?.id) {
        mutableStateOf(existing?.km?.toString() ?: vehicle?.km?.toString().orEmpty())
    }
    var cost by remember(existing?.id) {
        mutableStateOf(existing?.cost?.let { formatCostInput(it) }.orEmpty())
    }
    var shop by remember(existing?.id) { mutableStateOf(existing?.shop.orEmpty()) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var attachments by remember(existing?.id) {
        mutableStateOf(existing?.attachmentNames ?: emptyList())
    }
    var savingPhotos by remember { mutableStateOf(false) }

    val canSave = vehicle != null && (km.toIntOrNull() ?: -1) >= 0

    fun selectType(next: ServiceType) {
        if (title == type.label || title.isBlank()) {
            title = next.label
        }
        type = next
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            savingPhotos = true
            val available = (3 - attachments.size).coerceAtLeast(0)
            val names = runCatching {
                withContext(Dispatchers.IO) {
                    uris.take(available).mapNotNull { AttachmentStore.saveImage(context, it) }
                }
            }.getOrDefault(emptyList())
            attachments = (attachments + names).take(3)
            savingPhotos = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
    ) {
        item {
            ScreenHeader(
                title = if (existing == null) "Nuevo servicio" else "Editar servicio",
                onBack = onClose,
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (existing == null) "Registra el mantenimiento" else "Corrige el servicio",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (existing == null) {
                    "Añade los detalles y FixTracker actualizará el historial y los próximos avisos."
                } else {
                    "Los cambios se reflejarán en el historial y en los avisos del vehículo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.vehicles.size > 1) {
            item {
                Spacer(Modifier.height(24.dp))
                Label("VEHÍCULO")
                Spacer(Modifier.height(9.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.vehicles, key = { it.id }) { v ->
                        PillButton(
                            text = v.fullName,
                            selected = v.id == vehicleId,
                            onClick = {
                                if (v.id != vehicleId) {
                                    vehicleId = v.id
                                    if (existing == null) km = v.km.toString()
                                }
                            },
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Label("TIPO DE SERVICIO")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceType.entries.chunked(3).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTypes.forEach { itemType ->
                            TypeChip(
                                type = itemType,
                                selected = itemType == type,
                                onClick = { selectType(itemType) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            AppTextField(
                value = title,
                onValueChange = { title = it },
                label = "Concepto",
                placeholder = "Ej. Cambio de aceite y filtro",
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField(
                    label = "Fecha",
                    date = date,
                    onDateSelected = { date = it ?: LocalDate.now() },
                    allowClear = false,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = km,
                    onValueChange = { km = it.filter { c -> c.isDigit() }.take(7) },
                    label = "Kilometraje",
                    keyboardType = KeyboardType.Number,
                    suffix = "km",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = cost,
                    onValueChange = { input ->
                        cost = input.filter { it.isDigit() || it == ',' || it == '.' }.take(9)
                    },
                    label = "Coste",
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "0,00",
                    suffix = "€",
                    modifier = Modifier.weight(0.8f),
                )
                AppTextField(
                    value = shop,
                    onValueChange = { shop = it },
                    label = "Taller",
                    placeholder = "Opcional",
                    modifier = Modifier.weight(1.2f),
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notas",
                placeholder = "Detalles, referencias o próxima revisión…",
                singleLine = false,
                minLines = 3,
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("FOTOS Y FACTURAS")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${attachments.size}/3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(attachments, key = { it }) { name ->
                    AttachmentThumb(
                        context = context,
                        name = name,
                        onRemove = { attachments = attachments - name },
                    )
                }
                if (attachments.size < 3) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(FixTheme.palette.surfaceAlt)
                                .clickable {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (savingPhotos) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.AddAPhoto,
                                    contentDescription = "Añadir foto",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Hasta 3 fotos de la factura o del recambio. Se guardan solo en este dispositivo.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = FixTheme.palette.positive,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (type) {
                        ServiceType.ACEITE -> "Se programará el próximo cambio de aceite automáticamente."
                        ServiceType.ITV -> "La fecha de la próxima ITV se actualizará automáticamente."
                        ServiceType.SEGURO -> "La renovación del seguro se actualizará automáticamente."
                        else -> "El servicio se añadirá al historial del vehículo."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (existing == null) "Guardar servicio" else "Guardar cambios",
                enabled = canSave && !savingPhotos,
                onClick = {
                    val current = state.vehicle(vehicleId) ?: return@PrimaryButton
                    onSave(
                        ServiceRecord(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            vehicleId = current.id,
                            type = type,
                            title = title.ifBlank { type.label },
                            date = date,
                            km = km.toIntOrNull() ?: current.km,
                            cost = cost.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            shop = shop.trim(),
                            notes = notes.trim(),
                            attachmentNames = attachments,
                        )
                    )
                },
            )
            if (vehicle != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Kilometraje actual: ${formatKm(vehicle.km)} km",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AttachmentThumb(
    context: android.content.Context,
    name: String,
    onRemove: () -> Unit,
) {
    val bitmap = remember(name) { AttachmentStore.loadBitmap(context, name, 320) }
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FixTheme.palette.surfaceAlt),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto adjunta",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Quitar foto",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun TypeChip(
    type: ServiceType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = serviceColor(type)
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else FixTheme.palette.surfaceAlt
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = serviceIcon(type),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = type.shortLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatCostInput(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(java.util.Locale.US, "%.2f", value).replace('.', ',')
