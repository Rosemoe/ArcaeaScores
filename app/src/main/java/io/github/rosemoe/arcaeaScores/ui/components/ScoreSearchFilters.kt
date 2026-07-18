package io.github.rosemoe.arcaeaScores.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rosemoe.arcaeaScores.R

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ScoreSearchFilters(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    selectedClearTypes: List<Int>,
    onClearTypeToggle: (Int) -> Unit,
    availableLevels: List<String>,
    selectedLevels: List<String>,
    onLevelToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var clearTypeMenuExpanded by remember { mutableStateOf(false) }
    var chartLevelMenuExpanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_keyword)) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = if (keyword.isNotEmpty()) {
                {
                    IconButton(onClick = { onKeywordChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_clear_search)
                        )
                    }
                }
            } else {
                null
            },
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MultiSelectFilterChip(
                label = stringResource(R.string.filter_clear_type),
                selected = selectedClearTypes.isNotEmpty(),
                expanded = clearTypeMenuExpanded,
                onExpandChange = { clearTypeMenuExpanded = it }
            ) {
                clearTypeOptions.forEach { clearType ->
                    DropdownMenuItem(
                        text = { Text(clearTypeLabel(clearType)) },
                        onClick = { onClearTypeToggle(clearType) },
                        leadingIcon = {
                            Checkbox(
                                checked = clearType in selectedClearTypes,
                                onCheckedChange = null
                            )
                        }
                    )
                }
            }
            MultiSelectFilterChip(
                label = stringResource(R.string.filter_chart_level),
                selected = selectedLevels.isNotEmpty(),
                expanded = chartLevelMenuExpanded,
                onExpandChange = { chartLevelMenuExpanded = it }
            ) {
                availableLevels.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level) },
                        onClick = { onLevelToggle(level) },
                        leadingIcon = {
                            Checkbox(
                                checked = level in selectedLevels,
                                onCheckedChange = null
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSelectFilterChip(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Box {
        FilterChip(
            selected = selected,
            onClick = { onExpandChange(true) },
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.widthIn(min = 196.dp),
            content = menuContent
        )
    }
}

@Composable
private fun clearTypeLabel(clearType: Int): String = stringResource(
    when (clearType) {
        0 -> R.string.clear_type_track_lost
        1 -> R.string.clear_type_track_complete
        2 -> R.string.clear_type_full_recall
        3 -> R.string.clear_type_pure_memory
        4 -> R.string.clear_type_easy_clear
        else -> R.string.clear_type_hard_clear
    }
)

private val clearTypeOptions = 0..5
