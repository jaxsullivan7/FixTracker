package com.fixtracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixtracker.app.data.FixRepository
import com.fixtracker.app.notifications.NotificationScheduler
import com.fixtracker.app.ui.FixTrackerApp
import com.fixtracker.app.ui.screens.LockScreen
import com.fixtracker.app.ui.screens.OnboardingScreen
import com.fixtracker.app.ui.theme.FixTrackerTheme

class MainActivity : FragmentActivity() {

    private val openReminders = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openReminders.value = intent?.getBooleanExtra("open_reminders", false) == true

        setContent {
            val repository = remember { FixRepository(applicationContext) }
            val state by repository.state.collectAsStateWithLifecycle()
            var unlocked by remember { mutableStateOf(!state.settings.biometricLock) }
            val canAuthenticate = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS

            LaunchedEffect(state.settings.biometricLock) {
                if (!state.settings.biometricLock) unlocked = true
            }

            FixTrackerTheme(themeMode = state.settings.theme) {
                when {
                    !state.settings.onboarded -> OnboardingScreen(
                        onFinish = { name, withDemo ->
                            repository.completeOnboarding(name, withDemo)
                            NotificationScheduler.schedule(this)
                        },
                    )

                    state.settings.biometricLock && !unlocked -> LockScreen(
                        canAuthenticate = canAuthenticate,
                        onUnlock = { requestBiometric { unlocked = true } },
                        onDisableLock = {
                            repository.updateSettings(state.settings.copy(biometricLock = false))
                            unlocked = true
                        },
                    )

                    else -> FixTrackerApp(
                        state = state,
                        repository = repository,
                        initialOpenReminders = openReminders.value,
                        onInitialRemindersHandled = { openReminders.value = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_reminders", false)) {
            openReminders.value = true
        }
    }

    private fun requestBiometric(onSuccess: () -> Unit) {
        if (BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) != BiometricManager.BIOMETRIC_SUCCESS
        ) return

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("FixTracker")
                .setSubtitle("Confirma tu identidad")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}
