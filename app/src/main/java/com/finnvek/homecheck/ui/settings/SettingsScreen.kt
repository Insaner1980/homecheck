package com.finnvek.homecheck.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.billing.BillingState
import com.finnvek.homecheck.data.preferences.ThemeMode
import com.finnvek.homecheck.data.preferences.UserPreferences
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val billing: BillingState = BillingState(),
    val appVersion: String = "",
    val notificationsGranted: Boolean = true,
    val isWorking: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onReminders: (Boolean) -> Unit,
    onReminderTime: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onUnlockPremium: () -> Unit,
    onRestorePurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = state.preferences
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(
                start = HomeSpacing.page,
                end = HomeSpacing.page,
                bottom = 40.dp,
            ),
        ) {
            SettingsSection(stringResource(R.string.appearance))
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.system_default)
                    ThemeMode.LIGHT -> stringResource(R.string.light)
                    ThemeMode.DARK -> stringResource(R.string.dark)
                }
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = preferences.themeMode == mode,
                        onClick = { onTheme(mode) },
                        role = Role.RadioButton,
                    ).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = preferences.themeMode == mode, onClick = null)
                    Text(label, Modifier.padding(start = 12.dp))
                }
            }
            ToggleRow(stringResource(R.string.use_device_colors), preferences.dynamicColor, onDynamicColor)

            SettingsSection(stringResource(R.string.reminders))
            ToggleRow(stringResource(R.string.reminders_enabled), preferences.remindersEnabled, onReminders)
            val time = LocalTime.of(preferences.reminderHour, preferences.reminderMinute)
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            TextButton(onClick = onReminderTime, enabled = preferences.remindersEnabled) {
                Text(stringResource(R.string.reminder_time_value, time))
            }
            if (!state.notificationsGranted && preferences.remindersEnabled) {
                Text(stringResource(R.string.notifications_denied), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRequestNotificationPermission) { Text(stringResource(R.string.allow_notifications)) }
            }

            SettingsSection(stringResource(R.string.data))
            OutlinedButton(onClick = onBackup, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.back_up_homecheck))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRestoreBackup, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.restore_backup))
            }

            SettingsSection(stringResource(R.string.premium))
            if (state.billing.entitlement.isPremium) {
                Text(stringResource(R.string.premium_active), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.lifetime_purchase_active), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Button(onClick = onUnlockPremium, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.unlock_unlimited_assets)) }
            }
            TextButton(onClick = onRestorePurchase) { Text(stringResource(R.string.restore_purchase)) }

            SettingsSection(stringResource(R.string.about))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.version_value, state.appVersion), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.privacy_statement), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.play_purchase_statement), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SettingsSection(title: String) {
    Spacer(Modifier.height(HomeSpacing.section))
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
}

@Composable private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChecked).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
