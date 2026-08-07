package com.finnvek.homecheck.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.finnvek.homecheck.ui.onboarding.OnboardingScreen
import com.finnvek.homecheck.ui.theme.HomeCheckTheme
import org.junit.Rule
import org.junit.Test

class LargeFontScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onboardingPrimaryActionRemainsReachableAtDoubleFontScale() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                HomeCheckTheme { OnboardingScreen(onGetStarted = {}) }
            }
        }

        compose.onNodeWithText("Get started").performScrollTo().assertIsDisplayed()
    }
}
