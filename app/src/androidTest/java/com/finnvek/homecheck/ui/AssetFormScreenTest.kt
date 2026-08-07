package com.finnvek.homecheck.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.finnvek.homecheck.ui.assets.AssetFormScreen
import com.finnvek.homecheck.ui.assets.AssetFormState
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AssetFormScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nameAloneCanBeEnteredAndSaved() {
        var state by mutableStateOf(AssetFormState())
        var savedName: String? = null
        compose.setContent {
            HomeCheckTheme {
                AssetFormScreen(
                    state = state,
                    onStateChange = { state = it },
                    onSave = { savedName = state.name },
                    onBack = {},
                    onChoosePhoto = {},
                    onTakePhoto = {},
                )
            }
        }

        compose.onNodeWithText("Name").performTextInput("Washing machine")
        compose.onNodeWithText("Save").performClick()

        assertEquals("Washing machine", savedName)
    }
}
