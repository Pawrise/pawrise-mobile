package com.pawrise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.app.navigation.PawriseNavDisplay
import com.pawrise.core.designsystem.theme.PawriseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PawriseTheme {
                val initialSession by viewModel.initialSession.collectAsStateWithLifecycle()
                // Rien à l'écran pendant la relecture de la session (quelques ms, fond = surface-page).
                initialSession?.let { PawriseNavDisplay(initialSession = it, session = viewModel.session) }
            }
        }
    }
}
