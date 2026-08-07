package com.finnvek.homecheck.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.domain.AssetSearchDocument
import com.finnvek.homecheck.domain.maintenanceStatus
import com.finnvek.homecheck.domain.MaintenanceStatus
import com.finnvek.homecheck.ui.components.LocalImage
import com.finnvek.homecheck.ui.components.dueLabel
import com.finnvek.homecheck.ui.components.localized
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.LocalDate

enum class AssetSort { NAME, RECENT, NEXT_MAINTENANCE }

data class AssetsUiState(
    val assets: List<AssetEntity> = emptyList(),
    val tasks: List<MaintenanceTaskEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val query: String = "",
    val needsAttentionOnly: Boolean = false,
    val sort: AssetSort = AssetSort.RECENT,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    state: AssetsUiState,
    attachmentStore: AttachmentStore,
    onQueryChange: (String) -> Unit,
    onNeedsAttentionChange: (Boolean) -> Unit,
    onSortChange: (AssetSort) -> Unit,
    onAddAsset: () -> Unit,
    onAsset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val tasksByAsset = state.tasks.groupBy(MaintenanceTaskEntity::assetId)
    val photosByAsset = state.attachments.filter { it.type == AttachmentType.ASSET_PHOTO }.associateBy(AttachmentEntity::assetId)
    val filtered = state.assets.filter { asset ->
        AssetSearchDocument(
            asset.name, asset.manufacturer, asset.modelNumber, asset.serialNumber, asset.location, asset.category,
        ).matches(state.query) && (!state.needsAttentionOnly || tasksByAsset[asset.id].orEmpty().any { !it.dueDate.isAfter(today) })
    }.let { assets ->
        when (state.sort) {
            AssetSort.NAME -> assets.sortedBy { it.name.lowercase() }
            AssetSort.RECENT -> assets.sortedByDescending { it.updatedAt }
            AssetSort.NEXT_MAINTENANCE -> assets.sortedBy { asset ->
                tasksByAsset[asset.id]?.minOfOrNull { it.dueDate } ?: LocalDate.MAX
            }
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.assets)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAsset) { Icon(Icons.Default.Add, stringResource(R.string.add_asset)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_assets)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = HomeSpacing.page),
            )
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = HomeSpacing.page, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = state.needsAttentionOnly,
                    onClick = { onNeedsAttentionChange(!state.needsAttentionOnly) },
                    label = { Text(stringResource(R.string.needs_attention)) },
                )
                SortMenu(state.sort, onSortChange)
            }
            if (filtered.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(HomeSpacing.page)) {
                    Text(
                        stringResource(if (state.assets.isEmpty()) R.string.no_assets_title else R.string.no_search_results),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(if (state.assets.isEmpty()) R.string.no_assets_body else R.string.try_another_search),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                    items(filtered, key = AssetEntity::id) { asset ->
                        AssetRow(
                            asset = asset,
                            tasks = tasksByAsset[asset.id].orEmpty(),
                            photo = photosByAsset[asset.id],
                            attachmentStore = attachmentStore,
                            onClick = { onAsset(asset.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(sort: AssetSort, onSortChange: (AssetSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            val value = when (sort) {
                AssetSort.NAME -> stringResource(R.string.sort_name)
                AssetSort.RECENT -> stringResource(R.string.sort_recent)
                AssetSort.NEXT_MAINTENANCE -> stringResource(R.string.sort_next_maintenance)
            }
            Text(stringResource(R.string.sort_by_value, value))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AssetSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(when (option) {
                        AssetSort.NAME -> stringResource(R.string.sort_name)
                        AssetSort.RECENT -> stringResource(R.string.sort_recent)
                        AssetSort.NEXT_MAINTENANCE -> stringResource(R.string.sort_next_maintenance)
                    }) },
                    onClick = { onSortChange(option); open = false },
                )
            }
        }
    }
}

@Composable
private fun AssetRow(
    asset: AssetEntity,
    tasks: List<MaintenanceTaskEntity>,
    photo: AttachmentEntity?,
    attachmentStore: AttachmentStore,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val next = tasks.minByOrNull(MaintenanceTaskEntity::dueDate)
    val status = when {
        next != null -> dueLabel(context, next.dueDate)
        asset.warrantyExpirationDate != null -> stringResource(R.string.warranty_until, asset.warrantyExpirationDate.localized())
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = HomeSpacing.page, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (photo != null && attachmentStore.fileFor(photo.localPath).isFile) {
            LocalImage(
                file = attachmentStore.fileFor(photo.localPath),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                maxDimension = 192,
            )
        } else {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(56.dp).padding(12.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(asset.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            asset.location.ifNullOrBlank { asset.category.orEmpty() }.takeIf(String::isNotBlank)?.let { subtitle ->
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (status.isNotBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (next?.let { maintenanceStatus(it.dueDate) } == MaintenanceStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                )
            }
        }
    }
    HorizontalDivider(Modifier.padding(start = 90.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
}

private inline fun String?.ifNullOrBlank(defaultValue: () -> String): String = if (isNullOrBlank()) defaultValue() else this
