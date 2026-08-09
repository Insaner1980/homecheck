package com.finnvek.homecheck.ui.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.billing.BillingState
import com.finnvek.homecheck.ui.theme.HomeSpacing

@Composable
fun PremiumSheet(
    state: BillingState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(HomeSpacing.page)) {
        Text(stringResource(R.string.unlock_unlimited_assets), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.premium_limit_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Benefit(stringResource(R.string.unlimited_assets))
        Benefit(stringResource(R.string.no_subscription))
        Spacer(Modifier.height(20.dp))
        when {
            state.isLoading -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            state.isAvailable && state.formattedPrice != null -> {
                Text(stringResource(R.string.one_time_price, state.formattedPrice), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onPurchase, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.unlock_premium)) }
            }

            else -> {
                Text(stringResource(R.string.billing_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore_purchase)) }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.not_now)) }
    }
}

@Composable
private fun Benefit(text: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text)
        }
        Spacer(Modifier.height(10.dp))
    }
}
