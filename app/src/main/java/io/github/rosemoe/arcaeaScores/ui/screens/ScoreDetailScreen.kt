package io.github.rosemoe.arcaeaScores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaScore
import io.github.rosemoe.arcaeaScores.arc.clearTypeShortString
import io.github.rosemoe.arcaeaScores.arc.scoreGrade
import io.github.rosemoe.arcaeaScores.arc.toScoreText
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScoreDetailScreen(score: ArcaeaScore?, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = score?.title ?: stringResource(R.string.score_detail_title),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        if (score == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(R.string.score_detail_not_found))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ScoreOverview(score) }
                item {
                    DetailSection(title = stringResource(R.string.score_detail_score_data)) {
                        DetailRow(
                            label = stringResource(R.string.score_detail_score),
                            value = toScoreText(score.score),
                            emphasized = true
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_grade),
                            value = scoreGrade(score.score)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_potential),
                            value = String.format(Locale.getDefault(), "%.5f", score.playPotential)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_chart_constant),
                            value = score.chartConstant.toString()
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_clear_type),
                            value = clearTypeShortString(score.clearType)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_judgement),
                            value = "${score.pureCount}(+${score.maxPureCount}) / ${score.farCount} / ${score.lostCount}"
                        )
                        score.lostRankedScore?.let { lostScore ->
                            DetailRow(
                                label = stringResource(R.string.score_detail_lost_score),
                                value = String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    ceil(lostScore * 100.0) / 100.0
                                )
                            )
                        }
                    }
                }
                item {
                    DetailSection(title = stringResource(R.string.score_detail_chart_info)) {
                        DetailRow(
                            label = stringResource(R.string.score_detail_artist),
                            value = score.artist ?: stringResource(R.string.score_detail_not_available)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_chart_designer),
                            value = score.chartInfo?.chartDesigner
                                ?: stringResource(R.string.score_detail_not_available)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_jacket_designer),
                            value = score.chartInfo?.jacketDesigner
                                ?: stringResource(R.string.score_detail_not_available)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_chart_level),
                            value = "${difficultyName(score.difficulty)} ${score.chartInfo?.displayRating.orEmpty()}".trim()
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_side),
                            value = sideName(score.side)
                        )
                        DetailRow(
                            label = stringResource(R.string.score_detail_release_date),
                            value = score.releaseDate?.let { releaseDate ->
                                DateFormat.getDateInstance(
                                    DateFormat.MEDIUM,
                                    Locale.getDefault()
                                ).format(Date(releaseDate * 1_000L))
                            } ?: stringResource(R.string.score_detail_not_available)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.padding(bottom = 12.dp)) }
            }
        }
    }
}

@Composable
private fun ScoreOverview(score: ArcaeaScore) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = score.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            score.artist?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${difficultyName(score.difficulty)} ${score.chartInfo?.displayRating.orEmpty()}".trim(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = sideName(score.side),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            style = MaterialTheme.typography.titleMedium
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (emphasized) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun difficultyName(difficulty: Int): String = stringResource(
    when (difficulty) {
        0 -> R.string.difficulty_past
        1 -> R.string.difficulty_present
        2 -> R.string.difficulty_future
        3 -> R.string.difficulty_beyond
        4 -> R.string.difficulty_eternal
        else -> R.string.score_detail_not_available
    }
)

@Composable
private fun sideName(side: Int?): String = stringResource(
    when (side) {
        0 -> R.string.side_light
        1 -> R.string.side_conflict
        2 -> R.string.side_colorless
        3 -> R.string.side_lephon
        else -> R.string.score_detail_not_available
    }
)
