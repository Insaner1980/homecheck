package com.finnvek.homecheck.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.finnvek.homecheck.billing.BillingState
import com.finnvek.homecheck.ui.premium.PremiumSheet
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import org.junit.Rule
import org.junit.Test

class PremiumSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun premiumBoundaryExplainsOneTimeUnlimitedPurchase() {
        compose.setContent {
            HomeCheckTheme {
                PremiumSheet(
                    state = BillingState(formattedPrice = "€14.99", isLoading = false, isAvailable = true),
                    onPurchase = {},
                    onRestore = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Unlock unlimited assets").assertIsDisplayed()
        compose.onNodeWithText("One-time purchase · €14.99").assertIsDisplayed()
        compose.onNodeWithText("Restore purchase").assertIsDisplayed()
    }

    @Test fun restorePurchaseRemainsReachableAtDoubleFontScale() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                HomeCheckTheme {
                    PremiumSheet(
                        state = BillingState(formattedPrice = "€14.99", isLoading = false, isAvailable = true),
                        onPurchase = {},
                        onRestore = {},
                        onDismiss = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Restore purchase").performScrollTo().assertIsDisplayed()
    }
}
