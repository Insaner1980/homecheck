package com.finnvek.homecheck.ui.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.ui.components.dueLabel
import com.finnvek.homecheck.ui.components.localized
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MaintenanceUiState(
    val assets: List<AssetEntity> = emptyList(),
    val tasks: List<MaintenanceTaskEntity> = emptyList(),
    val history: List<MaintenanceHistoryEntity> = emptyList(),
    val showHistory: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    state: MaintenanceUiState,
    onShowHistory: (Boolean) -> Unit,
    onAddMaintenance: () -> Unit,
    onAddAsset: () -> Unit,
    onTask: (String) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val assetsById = state.assets.associateBy(AssetEntity::id)
    val today = LocalDate.now()
    val groups = listOf(
        stringResource(R.string.overdue) to state.tasks.filter { it.dueDate.isBefore(today) },
        stringResource(R.string.today) to state.tasks.filter { it.dueDate == today },
        stringResource(R.string.this_week) to state.tasks.filter { it.dueDate.isAfter(today) && !it.dueDate.isAfter(today.plusDays(7)) },
        stringResource(R.string.later) to state.tasks.filter { it.dueDate.isAfter(today.plusDays(7)) },
    ).filter { it.second.isNotEmpty() }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.maintenance)) }) },
        floatingActionButton = {
            if (state.assets.isNotEmpty() && !state.showHistory) {
                FloatingActionButton(onClick = onAddMaintenance) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_maintenance))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = HomeSpacing.page,
                end = HomeSpacing.page,
                bottom = 96.dp,
            ),
        ) {
            item {
                Row(Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !state.showHistory,
                        onClick = { onShowHistory(false) },
                        label = { Text(stringResource(R.string.upcoming)) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.padding(4.dp))
                    FilterChip(
                        selected = state.showHistory,
                        onClick = { onShowHistory(true) },
                        label = { Text(stringResource(R.string.history)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            if (state.showHistory) {
                if (state.history.isEmpty()) {
                    item { EmptyMessage(stringResource(R.string.no_history_title), stringResource(R.string.no_history_body)) }
                } else {
                    items(state.history, key = MaintenanceHistoryEntity::id) { entry ->
                        HistoryRow(entry, assetsById[entry.assetId]?.name.orEmpty())
                    }
                }
            } else if (state.assets.isEmpty()) {
                item {
                    EmptyMessage(stringResource(R.string.add_asset_first), stringResource(R.string.add_asset_first_body))
                    TextButton(onClick = onAddAsset) { Text(stringResource(R.string.add_asset)) }
                }
            } else if (state.tasks.isEmpty()) {
                item { EmptyMessage(stringResource(R.string.nothing_scheduled), stringResource(R.string.nothing_scheduled_body)) }
            } else {
                groups.forEach { (label, tasks) ->
                    item { Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                    items(tasks, key = MaintenanceTaskEntity::id) { task ->
                        TaskRow(task, assetsById[task.assetId]?.name.orEmpty(), onTask, onComplete)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: MaintenanceTaskEntity,
    assetName: String,
    onTask: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable { onTask(task.id) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            Text(assetName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(dueLabel(context, task.dueDate), color = if (task.dueDate.isBefore(LocalDate.now())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
        }
        FilledIconButton(onClick = { onComplete(task.id) }) {
            Icon(Icons.Default.Check, stringResource(R.string.mark_complete, task.title))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
}

@Composable
private fun HistoryRow(entry: MaintenanceHistoryEntity, assetName: String) {
    val date = Instant.ofEpochMilli(entry.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(entry.titleSnapshot, style = MaterialTheme.typography.titleMedium)
        Text(assetName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.completed_on, date.localized()), color = MaterialTheme.colorScheme.onSurfaceVariant)
        entry.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
}

@Composable
private fun EmptyMessage(title: String, body: String) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
