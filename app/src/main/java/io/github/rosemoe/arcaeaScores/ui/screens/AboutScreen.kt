package io.github.rosemoe.arcaeaScores.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rosemoe.arcaeaScores.BuildConfig
import io.github.rosemoe.arcaeaScores.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AboutScreen(
    onBack: () -> Unit,
    onOpenReleases: () -> Unit,
    onOpenSource: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about_app)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3DDC84)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(text = stringResource(R.string.app_name), fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_description),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                AboutItem(
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    title = stringResource(R.string.version_name),
                    subtitle = BuildConfig.VERSION_NAME
                )
            }
            item {
                AboutItem(
                    icon = { Icon(Icons.Outlined.Update, contentDescription = null) },
                    title = stringResource(R.string.release_notes),
                    onClick = onOpenReleases
                )
            }
            item {
                AboutItem(
                    icon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    title = stringResource(R.string.source_code),
                    onClick = onOpenSource
                )
            }
        }
    }
}

@Composable
private fun AboutItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon,
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    )
}
