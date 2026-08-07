package com.finnvek.homecheck.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.ui.assetdetail.AssetDetailScreen
import com.finnvek.homecheck.ui.assetdetail.AssetDetailUiState
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class AssetDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun assetDetailsAndMaintenanceAreReadable() {
        val asset = AssetEntity(
            id = "asset",
            name = "Washing machine",
            createdAt = 1,
            updatedAt = 1,
            manufacturer = "Example",
            modelNumber = "WM-10",
        )
        val task = MaintenanceTaskEntity(
            id = "task",
            assetId = asset.id,
            title = "Clean filter",
            dueDate = LocalDate.now().plusMonths(1),
            createdAt = 1,
            updatedAt = 1,
        )
        val attachmentStore = AttachmentStore(InstrumentationRegistry.getInstrumentation().targetContext)

        compose.setContent {
            HomeCheckTheme {
                AssetDetailScreen(
                    state = AssetDetailUiState(asset = asset, tasks = listOf(task), isLoading = false),
                    attachmentStore = attachmentStore,
                    onBack = {}, onEdit = {}, onDeleteAsset = {}, onAddMaintenance = {}, onEditTask = {},
                    onDeleteTask = {}, onAddDocument = {}, onOpenAttachment = {}, onShareAttachment = {},
                    onRenameAttachment = { _, _ -> }, onChangeAttachmentType = { _, _ -> }, onDeleteAttachment = {},
                )
            }
        }

        compose.onAllNodesWithText("Washing machine").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Example").assertIsDisplayed()
        compose.onNodeWithText("WM-10").assertIsDisplayed()
        compose.onNodeWithText("Clean filter").assertIsDisplayed()
    }
}
