package io.github.rosemoe.arcaeaScores.ui.components

import android.graphics.Typeface
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

data class ArcaeaFonts(
    val title: FontFamily,
    val score: FontFamily,
    val exo: FontFamily,
    val exoSemiBold: FontFamily,
    val unknownArtwork: ImageBitmap
)

@Composable
fun rememberArcaeaFonts(): ArcaeaFonts {
    val context = LocalContext.current
    return remember {
        ArcaeaFonts(
            title = FontFamily(Typeface.createFromAsset(context.assets, "fonts/L2-Regular.ttf")),
            score = FontFamily(Typeface.createFromAsset(context.assets, "fonts/GeosansLight.ttf")),
            exo = FontFamily(Typeface.createFromAsset(context.assets, "fonts/Exo-Regular.ttf")),
            exoSemiBold = FontFamily(Typeface.createFromAsset(context.assets, "fonts/Exo-SemiBold.ttf")),
            unknownArtwork = context.assets.open("unknown.jpg").use { stream ->
                BitmapFactory.decodeStream(stream).asImageBitmap()
            }
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
fun ScoreCard(
    score: ArcaeaScore,
    rank: Int,
    fonts: ArcaeaFonts,
    showArtwork: Boolean,
    artworkDataVersion: String?
) {
    val lostRankedScore = score.lostRankedScore?.takeIf { it > 0.0 }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showArtwork) {
                val artwork = remember(score.artworkPaths, artworkDataVersion) {
                    score.artworkPaths.asSequence()
                        .map(::File)
                        .firstOrNull(File::isFile)
                        ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
                        ?: fonts.unknownArtwork
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scale = maxOf(size.width / artwork.width, size.height / artwork.height)
                        val destinationWidth = (artwork.width * scale).roundToInt()
                        val destinationHeight = (artwork.height * scale).roundToInt()
                        drawImage(
                            image = artwork,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(artwork.width, artwork.height),
                            dstOffset = IntOffset(
                                ((size.width - destinationWidth) / 2).roundToInt(),
                                ((size.height - destinationHeight) / 2).roundToInt()
                            ),
                            dstSize = IntSize(destinationWidth, destinationHeight)
                        )

                        val triangleSize = minOf(size.width, size.height) * 0.38f
                        val triangle = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width - triangleSize, 0f)
                            lineTo(size.width, triangleSize)
                            close()
                        }
                        drawPath(triangle, Color(difficultyMainColor(score.difficulty)))
                    }
                }
                Spacer(Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color(difficultyMainColor(score.difficulty)))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = score.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        fontFamily = fonts.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "[${scoreGrade(score.score)}/${clearTypeShortString(score.clearType)}]",
                        fontFamily = fonts.exo
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "#$rank", fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = toScoreText(score.score),
                    fontFamily = fonts.score,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "PTT: ${score.chartConstant} > ${String.format(Locale.getDefault(), "%.5f", score.playPotential)}",
                    fontFamily = fonts.exoSemiBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "P/F/L: ${score.pureCount}(+${score.maxPureCount}) / ${score.farCount} / ${score.lostCount}",
                    fontFamily = fonts.exo,
                    fontSize = 15.sp
                )
                Text(
                    text = lostRankedScore?.let {
                        stringResource(R.string.lost_ranked_score, ceil(it * 100.0) / 100.0)
                    } ?: "\u00A0",
                    fontFamily = fonts.exo,
                    fontSize = 15.sp,
                    color = if (lostRankedScore == null) Color.Transparent else Color.Unspecified
                )
            }
        }
    }
}
