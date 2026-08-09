package com.finnvek.homecheck.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.finnvek.homecheck.billing.BillingState
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.ui.assetdetail.AssetDetailScreen
import com.finnvek.homecheck.ui.assetdetail.AssetDetailUiState
import com.finnvek.homecheck.ui.home.HomeScreen
import com.finnvek.homecheck.ui.home.HomeUiState
import com.finnvek.homecheck.ui.maintenance.MaintenanceScreen
import com.finnvek.homecheck.ui.maintenance.MaintenanceUiState
import com.finnvek.homecheck.ui.premium.PremiumSheet
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import java.time.LocalDate

private val previewAsset =
    AssetEntity(
        id = "preview-asset",
        name = "Heat pump",
        createdAt = 1,
        updatedAt = 1,
        category = "Heating",
        location = "Utility room",
        manufacturer = "Example",
        modelNumber = "HP-24",
    )

private fun previewTask(dueDate: LocalDate) =
    MaintenanceTaskEntity(
        id = "preview-task",
        assetId = previewAsset.id,
        title = "Clean filter",
        dueDate = dueDate,
        recurrenceInterval = 3,
        recurrenceUnit = com.finnvek.homecheck.domain.RecurrenceUnit.MONTHS,
        createdAt = 1,
        updatedAt = 1,
    )

@Preview(name = "Home empty", showBackground = true)
@Composable
private fun EmptyHomePreview() = PreviewTheme { HomeScreen(HomeUiState(), {}, {}, {}, {}, {}) }

@Preview(name = "Home all good", showBackground = true)
@Composable
private fun AllGoodHomePreview() =
    PreviewTheme {
        HomeScreen(HomeUiState(listOf(previewAsset)), {}, {}, {}, {}, {})
    }

@Preview(name = "Home needs attention", showBackground = true)
@Composable
private fun AttentionHomePreview() =
    PreviewTheme {
        HomeScreen(HomeUiState(listOf(previewAsset), listOf(previewTask(LocalDate.now().minusDays(2)))), {}, {}, {}, {}, {})
    }

@Preview(name = "Asset detail", showBackground = true)
@Composable
private fun AssetDetailPreview() =
    PreviewTheme {
        AssetDetailScreen(
            state = AssetDetailUiState(previewAsset, listOf(previewTask(LocalDate.now().plusDays(5))), isLoading = false),
            attachmentStore = AttachmentStore(LocalContext.current),
            onBack = {},
            onEdit = {},
            onDeleteAsset = {},
            onAddMaintenance = {},
            onEditTask = {},
            onDeleteTask = {},
            onAddDocument = {},
            onOpenAttachment = {},
            onShareAttachment = {},
            onRenameAttachment = { _, _ -> },
            onChangeAttachmentType = { _, _ -> },
            onDeleteAttachment = {},
        )
    }

@Preview(name = "Maintenance row", showBackground = true)
@Composable
private fun MaintenancePreview() =
    PreviewTheme {
        MaintenanceScreen(
            MaintenanceUiState(listOf(previewAsset), listOf(previewTask(LocalDate.now()))),
            {},
            {},
            {},
            {},
            {},
        )
    }

@Preview(name = "Premium", showBackground = true)
@Composable
private fun PremiumPreview() =
    PreviewTheme {
        PremiumSheet(BillingState(formattedPrice = "€14.99", isLoading = false, isAvailable = true), {}, {}, {})
    }

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    HomeCheckTheme(content = content)
}
