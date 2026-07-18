package io.github.rosemoe.arcaeaScores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.app.MainUiState
import io.github.rosemoe.arcaeaScores.ui.components.PlayerSummary
import io.github.rosemoe.arcaeaScores.ui.components.ScoreCard
import io.github.rosemoe.arcaeaScores.ui.components.rememberArcaeaFonts

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: MainUiState,
    onUpdate: () -> Unit,
    onSetName: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberArcaeaFonts()
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = topAppBarScrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onUpdate) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = stringResource(R.string.pd_dialog_title)
                )
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            ) {
                item {
                    PlayerSummary(state = state, fonts = fonts, onClick = onSetName)
                }
                itemsIndexed(
                    items = state.scores,
                    key = { _, score -> "${score.songId}-${score.difficulty}" }
                ) { index, score ->
                    ScoreCard(score = score, rank = index + 1, fonts = fonts)
                }
            }
            if (state.isLoading) {
                LoadingOverlay(state.loadingMessage.orEmpty())
            }
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(message)
            }
        }
    }
}
