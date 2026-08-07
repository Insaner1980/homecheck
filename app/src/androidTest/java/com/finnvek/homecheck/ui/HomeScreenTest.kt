package com.finnvek.homecheck.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finnvek.homecheck.ui.home.HomeScreen
import com.finnvek.homecheck.ui.home.HomeUiState
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyHomeExplainsTheProductAndOffersFirstAsset() {
        compose.setContent {
            HomeCheckTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onSettings = {},
                    onAddAsset = {},
                    onTask = {},
                    onSeeAllMaintenance = {},
                    onAsset = {},
                )
            }
        }

        compose.onNodeWithText("Everything is in check").assertIsDisplayed()
        compose.onNodeWithText("Start with the things worth remembering.").assertIsDisplayed()
        compose.onNodeWithText("Add your first asset").assertIsDisplayed()
    }
}
