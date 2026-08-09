package com.finnvek.homecheck.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.domain.RecurrenceUnit
import com.finnvek.homecheck.ui.components.localized
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.LocalDate

data class MaintenanceFormState(
    val taskId: String? = null,
    val assetId: String = "",
    val title: String = "",
    val dueDate: String = "",
    val notes: String = "",
    val recurrenceInterval: String = "",
    val recurrenceUnit: RecurrenceUnit? = null,
    val reminderEnabled: Boolean = true,
    val titleError: Boolean = false,
    val assetError: Boolean = false,
    val dueDateError: Boolean = false,
    val recurrenceError: Boolean = false,
    val saveError: Boolean = false,
    val isSaving: Boolean = false,
)

private const val INTERVAL_FIELD_WEIGHT = 0.45f
private const val RECURRENCE_UNIT_WEIGHT = 0.55f

private data class RepeatPreset(
    val label: Int,
    val interval: Int?,
    val unit: RecurrenceUnit?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceFormScreen(
    state: MaintenanceFormState,
    assets: List<AssetEntity>,
    onStateChange: (MaintenanceFormState) -> Unit,
    onPickDueDate: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.taskId == null) R.string.add_maintenance else R.string.edit_maintenance)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = { TextButton(onClick = onSave, enabled = !state.isSaving) { Text(stringResource(R.string.save)) } },
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
            Spacer(Modifier.height(12.dp))
            if (state.saveError) {
                Text(stringResource(R.string.maintenance_save_failed), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }
            AssetSelector(state, assets, onStateChange)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = { onStateChange(state.copy(title = it, titleError = false)) },
                label = { Text(stringResource(R.string.title)) },
                isError = state.titleError,
                supportingText = if (state.titleError) ({ Text(stringResource(R.string.title_required)) }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPickDueDate, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.dueDate.isBlank()) stringResource(R.string.due_date) else LocalDate.parse(state.dueDate).localized())
            }
            if (state.dueDateError) Text(stringResource(R.string.due_date_required), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(HomeSpacing.section))
            Text(stringResource(R.string.repeat), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            RepeatOptions(state, onStateChange)
            if (state.usesCustomRepeat()) {
                Spacer(Modifier.height(12.dp))
                CustomRepeat(state, onStateChange)
            }
            Spacer(Modifier.height(HomeSpacing.section))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_enabled), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.reminder_enabled_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.reminderEnabled, onCheckedChange = { onStateChange(state.copy(reminderEnabled = it)) })
            }
            Spacer(Modifier.height(HomeSpacing.section))
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onStateChange(state.copy(notes = it)) },
                label = { Text(stringResource(R.string.notes)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AssetSelector(
    state: MaintenanceFormState,
    assets: List<AssetEntity>,
    onStateChange: (MaintenanceFormState) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = assets.firstOrNull { it.id == state.assetId }
    Column {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name ?: stringResource(R.string.select_asset))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            assets.forEach { asset ->
                DropdownMenuItem(
                    text = { Text(asset.name) },
                    onClick = {
                        open = false
                        onStateChange(state.copy(assetId = asset.id, assetError = false))
                    },
                )
            }
        }
        if (state.assetError) Text(stringResource(R.string.asset_required), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun RepeatOptions(
    state: MaintenanceFormState,
    onStateChange: (MaintenanceFormState) -> Unit,
) {
    val presets =
        listOf(
            RepeatPreset(R.string.does_not_repeat, null, null),
            RepeatPreset(R.string.weekly, 1, RecurrenceUnit.WEEKS),
            RepeatPreset(R.string.monthly, 1, RecurrenceUnit.MONTHS),
            RepeatPreset(R.string.every_three_months, 3, RecurrenceUnit.MONTHS),
            RepeatPreset(R.string.every_six_months, 6, RecurrenceUnit.MONTHS),
            RepeatPreset(R.string.yearly, 1, RecurrenceUnit.YEARS),
            RepeatPreset(R.string.custom, -1, RecurrenceUnit.DAYS),
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            val selected =
                if (preset.interval == -1) {
                    state.usesCustomRepeat()
                } else {
                    state.recurrenceInterval == preset.interval?.toString().orEmpty() && state.recurrenceUnit == preset.unit
                }
            FilterChip(
                selected = selected,
                onClick = {
                    if (preset.interval == -1) {
                        onStateChange(state.copy(recurrenceInterval = "2", recurrenceUnit = RecurrenceUnit.DAYS))
                    } else {
                        onStateChange(state.copy(recurrenceInterval = preset.interval?.toString().orEmpty(), recurrenceUnit = preset.unit))
                    }
                },
                label = { Text(stringResource(preset.label)) },
            )
        }
    }
}

private fun MaintenanceFormState.usesCustomRepeat(): Boolean {
    val preset = recurrenceInterval to recurrenceUnit
    return recurrenceUnit != null && preset !in
        setOf(
            "1" to RecurrenceUnit.WEEKS,
            "1" to RecurrenceUnit.MONTHS,
            "3" to RecurrenceUnit.MONTHS,
            "6" to RecurrenceUnit.MONTHS,
            "1" to RecurrenceUnit.YEARS,
        )
}

@Composable
private fun CustomRepeat(
    state: MaintenanceFormState,
    onStateChange: (MaintenanceFormState) -> Unit,
) {
    var unitMenu by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.recurrenceInterval,
            onValueChange = { value ->
                if (value.length <= 3 && value.all(Char::isDigit)) {
                    onStateChange(state.copy(recurrenceInterval = value, recurrenceError = false))
                }
            },
            label = { Text(stringResource(R.string.every)) },
            isError = state.recurrenceError,
            supportingText = if (state.recurrenceError) ({ Text(stringResource(R.string.recurrence_interval_required)) }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(INTERVAL_FIELD_WEIGHT),
        )
        Column(Modifier.weight(RECURRENCE_UNIT_WEIGHT)) {
            OutlinedButton(onClick = { unitMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(recurrenceUnitLabel(state.recurrenceUnit ?: RecurrenceUnit.DAYS))
            }
            DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                RecurrenceUnit.entries.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(recurrenceUnitLabel(unit)) },
                        onClick = {
                            unitMenu = false
                            onStateChange(state.copy(recurrenceUnit = unit))
                        },
                    )
                }
            }
        }
    }
}

@Composable private fun recurrenceUnitLabel(unit: RecurrenceUnit) =
    stringResource(
        when (unit) {
            RecurrenceUnit.DAYS -> R.string.days
            RecurrenceUnit.WEEKS -> R.string.weeks
            RecurrenceUnit.MONTHS -> R.string.months
            RecurrenceUnit.YEARS -> R.string.years
        },
    )
