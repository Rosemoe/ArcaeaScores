package io.github.rosemoe.arcaeaScores.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.app.MainUiState
import io.github.rosemoe.arcaeaScores.arc.ArcaeaScore
import io.github.rosemoe.arcaeaScores.arc.clearTypeShortString
import io.github.rosemoe.arcaeaScores.arc.difficultyMainColor
import io.github.rosemoe.arcaeaScores.arc.scoreGrade
import io.github.rosemoe.arcaeaScores.arc.toScoreText
import io.github.rosemoe.arcaeaScores.util.toScaledString
import java.text.DateFormat
import java.util.Date
import java.util.Locale

data class ArcaeaFonts(
    val title: FontFamily,
    val score: FontFamily,
    val exo: FontFamily,
    val exoSemiBold: FontFamily
)

@Composable
fun rememberArcaeaFonts(): ArcaeaFonts {
    val context = LocalContext.current
    return remember {
        ArcaeaFonts(
            title = FontFamily(Typeface.createFromAsset(context.assets, "fonts/L2-Regular.ttf")),
            score = FontFamily(Typeface.createFromAsset(context.assets, "fonts/GeosansLight.ttf")),
            exo = FontFamily(Typeface.createFromAsset(context.assets, "fonts/Exo-Regular.ttf")),
            exoSemiBold = FontFamily(Typeface.createFromAsset(context.assets, "fonts/Exo-SemiBold.ttf"))
        )
    }
}

@Composable
fun PlayerSummary(state: MainUiState, fonts: ArcaeaFonts, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = state.playerName.ifBlank { stringResource(R.string.click_to_set_name) },
                fontFamily = fonts.exoSemiBold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.best_30_potential),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.best30Potential.toScaledString(),
                        fontFamily = fonts.exoSemiBold,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.max_potential),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.maxPotential.toScaledString(),
                        fontFamily = fonts.exoSemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.last_updated),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (state.updateTime > 0) {
                    DateFormat.getDateTimeInstance(
                        DateFormat.DEFAULT,
                        DateFormat.DEFAULT,
                        Locale.getDefault()
                    ).format(Date(state.updateTime))
                } else {
                    stringResource(R.string.not_updated)
                },
                fontFamily = fonts.exo,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ScoreCard(score: ArcaeaScore, rank: Int, fonts: ArcaeaFonts) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color(difficultyMainColor(score.difficulty)))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = score.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        fontFamily = fonts.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "[${scoreGrade(score.score)}/${clearTypeShortString(score.clearType)}]",
                        fontFamily = fonts.exo
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "#$rank", fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = "Potential: ${score.chartConstant} > ${String.format(Locale.getDefault(), "%.5f", score.playPotential)}",
                    fontFamily = fonts.exo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = toScoreText(score.score), fontFamily = fonts.score, fontSize = 17.sp)
                Text(
                    text = "P/F/L: ${score.pureCount}(+${score.maxPureCount}) / ${score.farCount} / ${score.lostCount}",
                    fontFamily = fonts.exo,
                    fontSize = 15.sp
                )
            }
        }
    }
}
