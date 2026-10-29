package com.fixtracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixtracker.app.data.FixRepository
import com.fixtracker.app.ui.FixTrackerApp
import com.fixtracker.app.ui.theme.FixTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember { FixRepository(applicationContext) }
            val state by repository.state.collectAsStateWithLifecycle()

            FixTrackerTheme(themeMode = state.settings.theme) {
                FixTrackerApp(state = state, repository = repository)
            }
        }
    }
}
