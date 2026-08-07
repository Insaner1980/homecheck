package com.finnvek.homecheck.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.domain.HomeAttentionStatus
import com.finnvek.homecheck.domain.WarrantyRules
import com.finnvek.homecheck.domain.homeAttentionStatus
import com.finnvek.homecheck.ui.components.HomeStatusIllustration
import com.finnvek.homecheck.ui.components.dueLabel
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.LocalDate

data class HomeUiState(
    val assets: List<AssetEntity> = emptyList(),
    val tasks: List<MaintenanceTaskEntity> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSettings: () -> Unit,
    onAddAsset: () -> Unit,
    onTask: (String) -> Unit,
    onSeeAllMaintenance: () -> Unit,
    onAsset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val assetsById = state.assets.associateBy(AssetEntity::id)
    val attention = state.tasks.filter { !it.dueDate.isAfter(today) }
    val upcoming = state.tasks.filter { it.dueDate.isAfter(today) }.sortedBy { it.dueDate }
    val warranties = state.assets.filter { asset ->
        asset.warrantyExpirationDate?.let { WarrantyRules.isExpiringSoon(it, today) } == true
    }.sortedBy { it.warrantyExpirationDate }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = HomeSpacing.page,
                end = HomeSpacing.page,
                bottom = 96.dp,
            ),
        ) {
            item {
                StatusArea(state.tasks, today)
                Spacer(Modifier.height(HomeSpacing.section))
            }
            if (state.assets.isEmpty()) {
                item {
                    Text(stringResource(R.string.empty_home_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.empty_home_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAddAsset) { Text(stringResource(R.string.add_first_asset)) }
                }
                return@LazyColumn
            }
            if (attention.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.needs_attention)) }
                items(attention, key = MaintenanceTaskEntity::id) { task ->
                    MaintenanceRow(task, assetsById[task.assetId]?.name.orEmpty(), onTask)
                }
                item { Spacer(Modifier.height(HomeSpacing.section)) }
            }
            if (upcoming.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.coming_up), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        if (upcoming.size > 3) TextButton(onClick = onSeeAllMaintenance) { Text(stringResource(R.string.see_all)) }
                    }
                }
                items(upcoming.take(3), key = MaintenanceTaskEntity::id) { task ->
                    MaintenanceRow(task, assetsById[task.assetId]?.name.orEmpty(), onTask)
                }
            } else if (attention.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_maintenance_scheduled), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (warranties.isNotEmpty()) {
                item { Spacer(Modifier.height(HomeSpacing.section)); SectionTitle(stringResource(R.string.warranties)) }
                items(warranties, key = AssetEntity::id) { asset ->
                    val days = WarrantyRules.daysRemaining(requireNotNull(asset.warrantyExpirationDate), today).toInt()
                    Row(
                        Modifier.fillMaxWidth().clickable { onAsset(asset.id) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.warranty_expiring), style = MaterialTheme.typography.labelLarge)
                            Text(asset.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(pluralStringResource(R.plurals.days_remaining, days, days), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusArea(tasks: List<MaintenanceTaskEntity>, today: LocalDate) {
    val status = homeAttentionStatus(tasks.map { it.dueDate }, today)
    val overdue = tasks.count { it.dueDate.isBefore(today) }
    val dueToday = tasks.count { it.dueDate == today }
    val dueSoon = tasks.count { it.dueDate.isAfter(today) && !it.dueDate.isAfter(today.plusDays(7)) }
    val title = when (status) {
        HomeAttentionStatus.ALL_CLEAR -> stringResource(R.string.everything_in_check)
        HomeAttentionStatus.UPCOMING -> stringResource(R.string.on_top_of_things)
        HomeAttentionStatus.DUE_TODAY -> stringResource(R.string.few_things_need_attention)
        HomeAttentionStatus.OVERDUE -> stringResource(R.string.home_needs_attention)
    }
    val body = when (status) {
        HomeAttentionStatus.ALL_CLEAR -> stringResource(R.string.no_attention_needed)
        HomeAttentionStatus.UPCOMING -> pluralStringResource(R.plurals.items_this_week, dueSoon, dueSoon)
        HomeAttentionStatus.DUE_TODAY -> pluralStringResource(R.plurals.tasks_due_today, dueToday, dueToday)
        HomeAttentionStatus.OVERDUE -> pluralStringResource(R.plurals.tasks_overdue, overdue, overdue)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeStatusIllustration(status == HomeAttentionStatus.ALL_CLEAR, Modifier.size(104.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable private fun SectionTitle(value: String) {
    Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun MaintenanceRow(task: MaintenanceTaskEntity, assetName: String, onTask: (String) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable { onTask(task.id) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(assetName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(dueLabel(context, task.dueDate), color = if (task.dueDate.isBefore(LocalDate.now())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
}
