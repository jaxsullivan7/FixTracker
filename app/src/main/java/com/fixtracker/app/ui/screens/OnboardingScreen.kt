package com.fixtracker.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.notifications.NotificationHelper
import com.fixtracker.app.notifications.NotificationScheduler
import com.fixtracker.app.ui.components.AppTextField
import com.fixtracker.app.ui.components.FixCard
import com.fixtracker.app.ui.components.Label
import com.fixtracker.app.ui.components.PrimaryButton
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography

@Composable
fun OnboardingScreen(onFinish: (name: String, withDemo: Boolean) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var withDemo by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(NotificationHelper.canPost(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsEnabled = NotificationHelper.canPost(context)
        if (notificationsEnabled) NotificationScheduler.schedule(context)
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsEnabled = NotificationHelper.canPost(context)
            if (notificationsEnabled) NotificationScheduler.schedule(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(FixTheme.palette.copper)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "FIXTRACKER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.6.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(34.dp))
            Text(
                text = "Tu coche,\nen orden.",
                style = FixTypography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "El cuaderno de mantenimiento que querrías tener: servicios, avisos, documentos y gastos en un solo lugar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            FixCard(modifier = Modifier.fillMaxWidth()) {
                OnboardingFeature(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Avisa antes",
                    text = "ITV, seguro, aceite y neumáticos. Sin sorpresas.",
                )
                OnboardingFeature(
                    icon = Icons.Outlined.DirectionsCar,
                    title = "Todo el historial",
                    text = "Servicios, facturas y kilometraje de cada vehículo.",
                )
                OnboardingFeature(
                    icon = Icons.Outlined.BarChart,
                    title = "Gastos claros",
                    text = "Mantenimiento y combustible, con consumo medio.",
                )
            }
        }

        item {
            Spacer(Modifier.height(26.dp))
            AppTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = "¿Cómo te llamas?",
                placeholder = "Opcional",
            )
        }

        item {
            Spacer(Modifier.height(26.dp))
            Label("CÓMO QUIERES EMPEZAR")
            Spacer(Modifier.height(10.dp))
            StartOption(
                selected = withDemo,
                title = "Explorar con datos de ejemplo",
                subtitle = "Un Alfa Romeo y una Vespa con historial, avisos y repostajes.",
                icon = Icons.Outlined.DirectionsCar,
                onClick = { withDemo = true },
            )
            Spacer(Modifier.height(9.dp))
            StartOption(
                selected = !withDemo,
                title = "Empezar de cero",
                subtitle = "Añade tu primer vehículo y construye tu propio garaje.",
                icon = Icons.Outlined.Add,
                onClick = { withDemo = false },
            )
        }

        item {
            Spacer(Modifier.height(26.dp))
            val palette = FixTheme.palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (notificationsEnabled) palette.positiveSoft else palette.copperSoft)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (notificationsEnabled) Icons.Outlined.CheckCircle
                    else Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = if (notificationsEnabled) palette.positive else palette.copper,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (notificationsEnabled) "Avisos activados" else "Activa los avisos",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (notificationsEnabled) {
                            "Te avisaremos de lo importante, sin ruido."
                        } else {
                            "Así no se te pasa una ITV ni un cambio de aceite."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!notificationsEnabled) {
                    Text(
                        text = "Activar",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.copper,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { requestNotifications() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                text = if (withDemo) "Explorar FixTracker" else "Crear mi garaje",
                onClick = {
                    NotificationScheduler.schedule(context)
                    onFinish(name, withDemo)
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tus datos se guardan solo en este dispositivo. Podrás cambiar todo en Ajustes.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OnboardingFeature(icon: ImageVector, title: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(FixTheme.palette.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StartOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else FixTheme.palette.hairline,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 6.dp else 1.5.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else FixTheme.palette.hairline,
                    shape = CircleShape,
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}
