package com.finnvek.homecheck.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
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

    @Test fun selectedPhotoIsShownInTheForm() {
        val photo = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.resolve("asset-form-photo.png")
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        compose.setContent {
            HomeCheckTheme {
                AssetFormScreen(
                    state = AssetFormState(pendingPhotoUri = Uri.fromFile(photo).toString()),
                    onStateChange = {},
                    onSave = {},
                    onBack = {},
                    onChoosePhoto = {},
                    onTakePhoto = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Photo").fetchSemanticsNode()
    }
}
