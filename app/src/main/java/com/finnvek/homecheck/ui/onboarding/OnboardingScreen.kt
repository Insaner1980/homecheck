package com.finnvek.homecheck.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.ui.components.HomeStatusIllustration
import com.finnvek.homecheck.ui.theme.HomeSpacing

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HomeSpacing.page),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeStatusIllustration(allClear = true)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingPoint(stringResource(R.string.private_by_default))
            OnboardingPoint(stringResource(R.string.works_offline))
            OnboardingPoint(stringResource(R.string.no_subscription))
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.get_started)) }
    }
}

@Composable private fun OnboardingPoint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
