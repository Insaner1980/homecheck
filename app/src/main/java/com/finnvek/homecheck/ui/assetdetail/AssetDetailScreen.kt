package com.finnvek.homecheck.ui.assetdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.domain.MaintenanceStatus
import com.finnvek.homecheck.domain.maintenanceStatus
import com.finnvek.homecheck.ui.components.LocalImage
import com.finnvek.homecheck.ui.components.dueLabel
import com.finnvek.homecheck.ui.components.localized
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AssetDetailUiState(
    val asset: AssetEntity? = null,
    val tasks: List<MaintenanceTaskEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val history: List<MaintenanceHistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
)

private const val PHOTO_ASPECT_RATIO = 16f / 9f
private const val RECENT_HISTORY_LIMIT = 5
private const val DETAIL_LABEL_WEIGHT = 0.4f
private const val DETAIL_VALUE_WEIGHT = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    state: AssetDetailUiState,
    attachmentStore: AttachmentStore,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteAsset: () -> Unit,
    onAddMaintenance: () -> Unit,
    onEditTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddDocument: (AttachmentType) -> Unit,
    onOpenAttachment: (AttachmentEntity) -> Unit,
    onShareAttachment: (AttachmentEntity) -> Unit,
    onRenameAttachment: (AttachmentEntity, String) -> Unit,
    onChangeAttachmentType: (AttachmentEntity, AttachmentType) -> Unit,
    onDeleteAttachment: (AttachmentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val asset = state.asset
    var assetMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<String?>(null) }
    var attachmentToDelete by remember { mutableStateOf<AttachmentEntity?>(null) }
    var chooseDocumentType by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(asset?.name ?: stringResource(R.string.asset)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    if (asset != null) {
                        TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                        Box {
                            IconButton(
                                onClick = { assetMenu = true },
                            ) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
                            DropdownMenu(expanded = assetMenu, onDismissRequest = { assetMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_asset)) },
                                    onClick = {
                                        assetMenu = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.isLoading && asset == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(HomeSpacing.page)) {
                Text(stringResource(R.string.asset_not_found), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.asset_not_found_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_assets)) }
            }
        } else if (asset != null) {
            val photo = state.attachments.firstOrNull { it.type == AttachmentType.ASSET_PHOTO }
            val documents = state.attachments.filter { it.type != AttachmentType.ASSET_PHOTO }
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(bottom = 40.dp),
            ) {
                item {
                    if (photo != null && attachmentStore.fileFor(photo.localPath).isFile) {
                        LocalImage(
                            file = attachmentStore.fileFor(photo.localPath),
                            contentDescription = stringResource(R.string.asset_photo_description, asset.name),
                            modifier = Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT_RATIO),
                        )
                    }
                    Column(Modifier.padding(HomeSpacing.page)) {
                        Text(asset.name, style = MaterialTheme.typography.headlineMedium)
                        asset.location?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            assetStatus(state.tasks),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                item {
                    DetailSection(asset)
                    SectionHeader(stringResource(R.string.maintenance), action = stringResource(R.string.add), onAction = onAddMaintenance)
                }
                if (state.tasks.isEmpty()) {
                    item { MutedText(stringResource(R.string.no_maintenance_for_asset)) }
                } else {
                    items(state.tasks, key = MaintenanceTaskEntity::id) { task ->
                        AssetTaskRow(task, onEditTask) { taskToDelete = it }
                    }
                }
                if (asset.warrantyExpirationDate != null) {
                    item {
                        SectionHeader(stringResource(R.string.warranty))
                        DetailPair(stringResource(R.string.until), asset.warrantyExpirationDate.localized())
                    }
                }
                item {
                    SectionHeader(stringResource(R.string.documents), action = stringResource(R.string.add_document)) {
                        chooseDocumentType = true
                    }
                }
                if (documents.isEmpty()) {
                    item { MutedText(stringResource(R.string.no_documents)) }
                } else {
                    items(documents, key = AttachmentEntity::id) { attachment ->
                        AttachmentRow(
                            attachment,
                            isMissing = !attachmentStore.fileFor(attachment.localPath).isFile,
                            onOpenAttachment,
                            onShareAttachment,
                            onRenameAttachment,
                            onChangeAttachmentType,
                            onDelete = { attachmentToDelete = it },
                        )
                    }
                }
                item { SectionHeader(stringResource(R.string.history)) }
                if (state.history.isEmpty()) {
                    item { MutedText(stringResource(R.string.no_history_body)) }
                } else {
                    items(state.history.take(RECENT_HISTORY_LIMIT), key = MaintenanceHistoryEntity::id) { HistoryEntry(it) }
                }
                asset.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    item {
                        SectionHeader(stringResource(R.string.notes))
                        Text(notes, Modifier.padding(horizontal = HomeSpacing.page))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_asset_question)) },
            text = { Text(stringResource(R.string.delete_asset_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteAsset()
                }) { Text(stringResource(R.string.delete_asset)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    taskToDelete?.let { taskId ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.delete_maintenance_question)) },
            text = { Text(stringResource(R.string.delete_maintenance_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    taskToDelete = null
                    onDeleteTask(taskId)
                }) {
                    Text(stringResource(R.string.delete_maintenance))
                }
            },
            dismissButton = { TextButton(onClick = { taskToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    attachmentToDelete?.let { attachment ->
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            title = { Text(stringResource(R.string.delete_document_question)) },
            text = { Text(stringResource(R.string.delete_document_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    attachmentToDelete = null
                    onDeleteAttachment(attachment)
                }) {
                    Text(stringResource(R.string.delete_document))
                }
            },
            dismissButton = { TextButton(onClick = { attachmentToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (chooseDocumentType) {
        AlertDialog(
            onDismissRequest = { chooseDocumentType = false },
            title = { Text(stringResource(R.string.document_type)) },
            text = {
                Column {
                    listOf(AttachmentType.RECEIPT, AttachmentType.MANUAL, AttachmentType.WARRANTY, AttachmentType.OTHER).forEach { type ->
                        TextButton(
                            onClick = {
                                chooseDocumentType = false
                                onAddDocument(type)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(attachmentTypeLabel(type)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseDocumentType = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun DetailSection(asset: AssetEntity) {
    val details =
        listOfNotNull(
            asset.manufacturer?.takeIf(String::isNotBlank)?.let { stringResource(R.string.manufacturer) to it },
            asset.modelNumber?.takeIf(String::isNotBlank)?.let { stringResource(R.string.model) to it },
            asset.serialNumber?.takeIf(String::isNotBlank)?.let { stringResource(R.string.serial_number) to it },
            asset.purchaseDate?.let { stringResource(R.string.purchased) to it.localized() },
            asset.retailer?.takeIf(String::isNotBlank)?.let { stringResource(R.string.retailer) to it },
            asset.category?.takeIf(String::isNotBlank)?.let { stringResource(R.string.category) to it },
        )
    if (details.isNotEmpty()) {
        SectionHeader(stringResource(R.string.details))
        details.forEach { (label, value) -> DetailPair(label, value) }
    }
}

@Composable private fun DetailPair(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = HomeSpacing.page, vertical = 8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(DETAIL_LABEL_WEIGHT))
        Text(value, modifier = Modifier.weight(DETAIL_VALUE_WEIGHT))
    }
}

@Composable private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = HomeSpacing.page, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable private fun MutedText(value: String) {
    Text(
        value,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = HomeSpacing.page, vertical = 8.dp),
    )
}

@Composable
private fun AssetTaskRow(
    task: MaintenanceTaskEntity,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = HomeSpacing.page, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    dueLabel(context, task.dueDate),
                    color =
                        if (task.dueDate.isBefore(
                                LocalDate.now(),
                            )
                        ) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                recurrenceText(
                    task,
                )?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = {
                        menu = false
                        onEdit(task.id)
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                        menu = false
                        onDelete(task.id)
                    })
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = HomeSpacing.page), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    }
}

@Composable
private fun AttachmentRow(
    attachment: AttachmentEntity,
    isMissing: Boolean,
    onOpen: (AttachmentEntity) -> Unit,
    onShare: (AttachmentEntity) -> Unit,
    onRename: (AttachmentEntity, String) -> Unit,
    onChangeType: (AttachmentEntity, AttachmentType) -> Unit,
    onDelete: (AttachmentEntity) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var name by remember(attachment.displayName) { mutableStateOf(attachment.displayName) }
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(attachment) }.padding(horizontal = HomeSpacing.page, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(attachment.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                attachmentTypeLabel(attachment.type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isMissing) {
                Text(
                    stringResource(R.string.file_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.open)) }, onClick = {
                    menu = false
                    onOpen(attachment)
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, onClick = {
                    menu = false
                    onShare(attachment)
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = {
                    menu = false
                    rename = true
                })
                listOf(AttachmentType.RECEIPT, AttachmentType.MANUAL, AttachmentType.WARRANTY, AttachmentType.OTHER)
                    .filter { it != attachment.type }
                    .forEach { type ->
                        DropdownMenuItem(text = { Text(stringResource(R.string.change_to, attachmentTypeLabel(type))) }, onClick = {
                            menu = false
                            onChangeType(attachment, type)
                        })
                    }
                DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                    menu = false
                    onDelete(attachment)
                })
            }
        }
    }
    if (rename) {
        AlertDialog(
            onDismissRequest = { rename = false },
            title = { Text(stringResource(R.string.rename_document)) },
            text = { androidx.compose.material3.OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }) },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) onRename(attachment, name.trim())
                    rename = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { rename = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable private fun HistoryEntry(entry: MaintenanceHistoryEntity) {
    val date = Instant.ofEpochMilli(entry.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    Column(Modifier.fillMaxWidth().padding(horizontal = HomeSpacing.page, vertical = 8.dp)) {
        Text(date.localized(), style = MaterialTheme.typography.labelLarge)
        Text(entry.titleSnapshot)
    }
}

@Composable private fun attachmentTypeLabel(type: AttachmentType): String =
    stringResource(
        when (type) {
            AttachmentType.ASSET_PHOTO -> R.string.photo
            AttachmentType.RECEIPT -> R.string.receipt
            AttachmentType.MANUAL -> R.string.manual
            AttachmentType.WARRANTY -> R.string.warranty
            AttachmentType.OTHER -> R.string.other
        },
    )

@Composable private fun recurrenceText(task: MaintenanceTaskEntity): String? {
    val interval = task.recurrenceInterval
    val unit = task.recurrenceUnit
    if (interval == null || unit == null) return null
    val unitText =
        pluralStringResource(
            when (unit) {
                com.finnvek.homecheck.domain.RecurrenceUnit.DAYS -> R.plurals.recurrence_days
                com.finnvek.homecheck.domain.RecurrenceUnit.WEEKS -> R.plurals.recurrence_weeks
                com.finnvek.homecheck.domain.RecurrenceUnit.MONTHS -> R.plurals.recurrence_months
                com.finnvek.homecheck.domain.RecurrenceUnit.YEARS -> R.plurals.recurrence_years
            },
            interval,
            interval,
        )
    return stringResource(R.string.every_interval, interval, unitText)
}

@Composable private fun assetStatus(tasks: List<MaintenanceTaskEntity>): String {
    val next = tasks.minByOrNull(MaintenanceTaskEntity::dueDate) ?: return stringResource(R.string.all_good)
    return when (maintenanceStatus(next.dueDate)) {
        MaintenanceStatus.OVERDUE -> stringResource(R.string.maintenance_overdue)
        MaintenanceStatus.DUE_TODAY -> stringResource(R.string.maintenance_due_today)
        else -> stringResource(R.string.next_maintenance_date, next.dueDate.localized())
    }
}
