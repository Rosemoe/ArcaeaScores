package io.github.rosemoe.arcaeaScores.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaScoreSortOrder
import io.github.rosemoe.arcaeaScores.app.MainUiState
import io.github.rosemoe.arcaeaScores.ui.components.PlayerSummary
import io.github.rosemoe.arcaeaScores.ui.components.ScoreCard
import io.github.rosemoe.arcaeaScores.ui.components.ScoreSearchFilters
import io.github.rosemoe.arcaeaScores.ui.components.rememberArcaeaFonts

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: MainUiState,
    onUpdate: () -> Unit,
    onSetName: () -> Unit,
    collapseTopbarOnScroll: Boolean,
    modifier: Modifier = Modifier,
) {
    val fonts = rememberArcaeaFonts()
    val topAppBarScrollBehavior = if (!collapseTopbarOnScroll) TopAppBarDefaults.pinnedScrollBehavior() else TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isFiltering by rememberSaveable { mutableStateOf(false) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var selectedClearTypes by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var selectedLevels by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var zeroLostScoreOnly by rememberSaveable { mutableStateOf(false) }
    var sortOrderName by rememberSaveable { mutableStateOf(ArcaeaScoreSortOrder.Potential.name) }
    val sortOrder = ArcaeaScoreSortOrder.valueOf(sortOrderName)
    val filteredScores = state.scores
        .filter { score ->
            (keyword.isBlank() ||
                score.title.contains(keyword, ignoreCase = true) ||
                score.songId.contains(keyword, ignoreCase = true)) &&
                (selectedClearTypes.isEmpty() || score.clearType in selectedClearTypes) &&
                (selectedLevels.isEmpty() || score.chartInfo?.displayRating in selectedLevels) &&
                (!zeroLostScoreOnly || score.lostRankedScore == 0.0)
        }
        .sortedWith(sortOrder.comparator())
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                actions = {
                    IconButton(onClick = onUpdate) {
                        Icon(
                            painter = painterResource(R.drawable.refresh),
                            contentDescription = stringResource(R.string.action_read_game_data)
                        )
                    }
                    IconButton(
                        onClick = { isFiltering = !isFiltering },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isFiltering) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            contentColor = if (isFiltering) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = stringResource(R.string.action_filter)
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
        ) { contentPadding ->
            AnimatedContent(
                targetState = state.isLoading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(animationSpec = tween(180)) +
                            slideInVertically(animationSpec = tween(180)) { -it / 8 }) togetherWith
                            (fadeOut(animationSpec = tween(120)) +
                                slideOutVertically(animationSpec = tween(120)) { it / 8 })
                    } else {
                        (fadeIn(animationSpec = tween(260, delayMillis = 60)) +
                            slideInVertically(animationSpec = tween(260, delayMillis = 60)) { it / 8 }) togetherWith
                            (fadeOut(animationSpec = tween(140)) +
                                slideOutVertically(animationSpec = tween(140)) { -it / 8 })
                    }
                },
                label = "score loading state"
            ) { isLoading ->
                Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                LoadingScores(message = state.loadingMessage.orEmpty())
            } else if (isFiltering) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = scoreCardMinWidth),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ScoreSearchFilters(
                            keyword = keyword,
                            onKeywordChange = { keyword = it },
                            selectedClearTypes = selectedClearTypes,
                            onClearTypeToggle = { clearType ->
                                selectedClearTypes = selectedClearTypes.toggle(clearType)
                            },
                            availableLevels = chartLevelOptions,
                            selectedLevels = selectedLevels,
                            onLevelToggle = { level ->
                                selectedLevels = selectedLevels.toggle(level)
                            },
                            zeroLostScoreOnly = zeroLostScoreOnly,
                            onZeroLostScoreToggle = {
                                zeroLostScoreOnly = !zeroLostScoreOnly
                            },
                            sortOrder = sortOrder,
                            onSortOrderChange = { sortOrderName = it.name }
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ScoreListHeader(scoreCount = filteredScores.size)
                    }
                    if (filteredScores.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptySearchResults() }
                    } else {
                        itemsIndexed(
                            items = filteredScores,
                            key = { _, score -> "${score.songId}-${score.difficulty}" }
                        ) { index, score ->
                            ScoreCard(
                                score = score,
                                rank = index + 1,
                                fonts = fonts,
                                showArtwork = state.showArtwork,
                                artworkDataVersion = state.artworkDataVersion
                            )
                        }
                    }
                }
            } else if (state.scores.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    PlayerSummary(state = state, fonts = fonts, onClick = onSetName)
                    ScoreListHeader(scoreCount = 0)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyScores(onUpdate = onUpdate)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = scoreCardMinWidth),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PlayerSummary(state = state, fonts = fonts, onClick = onSetName)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ScoreListHeader(scoreCount = state.scores.size)
                    }
                    itemsIndexed(
                        items = state.scores,
                        key = { _, score -> "${score.songId}-${score.difficulty}" }
                    ) { index, score ->
                        ScoreCard(
                            score = score,
                            rank = index + 1,
                            fonts = fonts,
                            showArtwork = state.showArtwork,
                            artworkDataVersion = state.artworkDataVersion
                        )
                    }
                }
            }
            }
            }
        }
    }
}

@Composable
private fun ScoreListHeader(scoreCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.best_30),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = pluralStringResource(R.plurals.score_count, scoreCount, scoreCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyScores(onUpdate: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.empty_scores_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.empty_scores_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onUpdate) {
            Text(stringResource(R.string.action_read_game_data))
        }
    }
}

@Composable
private fun EmptySearchResults() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_matching_scores),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun <T> List<T>.toggle(item: T): List<T> =
    if (item in this) this - item else this + item

private val chartLevelOptions = buildList {
    for (level in 1..12) {
        add(level.toString())
        if (level in 7..11) {
            add("$level+")
        }
    }
}

private val scoreCardMinWidth = 400.dp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LoadingScores(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingIndicator(modifier = Modifier.size(64.dp))
        if (message.isNotBlank()) {
            Text(
                text = message,
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
