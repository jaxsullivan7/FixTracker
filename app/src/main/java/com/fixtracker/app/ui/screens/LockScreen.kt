package com.fixtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixtracker.app.ui.components.PrimaryButton
import com.fixtracker.app.ui.theme.FixTheme
import com.fixtracker.app.ui.theme.FixTypography

@Composable
fun LockScreen(
    canAuthenticate: Boolean,
    onUnlock: () -> Unit,
    onDisableLock: () -> Unit,
) {
    LaunchedEffect(canAuthenticate) {
        if (canAuthenticate) onUnlock()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(FixTheme.palette.heroStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "FixTracker",
            style = FixTypography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (canAuthenticate) {
                "Confirma tu identidad para ver tu garaje."
            } else {
                "No hay un método de desbloqueo disponible en este dispositivo."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (canAuthenticate) {
            PrimaryButton(
                text = "Desbloquear",
                onClick = onUnlock,
                modifier = Modifier.width(220.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Puedes usar tu huella, cara o el PIN del dispositivo.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            TextButton(onClick = onDisableLock) {
                Text("Desactivar el bloqueo", color = FixTheme.palette.copper)
            }
        }
    }
}
