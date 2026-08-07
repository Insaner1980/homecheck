package com.finnvek.homecheck.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.ui.maintenance.MaintenanceScreen
import com.finnvek.homecheck.ui.maintenance.MaintenanceUiState
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class MaintenanceScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyMaintenanceDirectsUserToAddAnAssetFirst() {
        compose.setContent {
            HomeCheckTheme {
                MaintenanceScreen(
                    state = MaintenanceUiState(),
                    onShowHistory = {},
                    onAddMaintenance = {},
                    onAddAsset = {},
                    onTask = {},
                    onComplete = {},
                )
            }
        }

        compose.onNodeWithText("Add an asset first").assertIsDisplayed()
        compose.onNodeWithText("Add asset").assertIsDisplayed()
    }

    @Test fun taskCanBeCompletedAndItsHistoryCanBeViewed() {
        val asset = AssetEntity("asset", "Heat pump", 1, 1)
        val task = MaintenanceTaskEntity("task", asset.id, "Clean filter", LocalDate.now(), createdAt = 1, updatedAt = 1)
        var completedTask: String? = null
        var state by mutableStateOf(MaintenanceUiState(assets = listOf(asset), tasks = listOf(task)))
        compose.setContent {
            HomeCheckTheme {
                MaintenanceScreen(
                    state = state,
                    onShowHistory = {}, onAddMaintenance = {}, onAddAsset = {}, onTask = {},
                    onComplete = { completedTask = it },
                )
            }
        }

        compose.onNodeWithContentDescription("Mark Clean filter complete").performClick()
        assertEquals("task", completedTask)

        compose.runOnIdle {
            state = MaintenanceUiState(
                assets = listOf(asset),
                history = listOf(MaintenanceHistoryEntity("history", asset.id, task.id, task.title, LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())),
                showHistory = true,
            )
        }
        compose.onNodeWithText("Clean filter").assertIsDisplayed()
        compose.onNodeWithText("Heat pump").assertIsDisplayed()
    }
}
