package io.github.rosemoe.arcaeaScores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rosemoe.arcaeaScores.ui.ArcaeaScoresApp
import io.github.rosemoe.arcaeaScores.ui.theme.ArcaeaScoresTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArcaeaScoresTheme {
                val viewModel: MainViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ArcaeaScoresApp(state = state, viewModel = viewModel)
            }
        }
    }
}
