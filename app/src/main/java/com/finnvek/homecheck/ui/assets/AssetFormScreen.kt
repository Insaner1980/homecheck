package com.finnvek.homecheck.ui.assets

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.finnvek.homecheck.R
import com.finnvek.homecheck.ui.components.localized
import com.finnvek.homecheck.ui.theme.HomeSpacing
import java.time.LocalDate

data class AssetFormState(
    val assetId: String? = null,
    val name: String = "",
    val category: String = "",
    val location: String = "",
    val manufacturer: String = "",
    val modelNumber: String = "",
    val serialNumber: String = "",
    val purchaseDate: String = "",
    val retailer: String = "",
    val warrantyExpirationDate: String = "",
    val notes: String = "",
    val pendingPhotoUri: String? = null,
    val pendingPhotoTempPath: String? = null,
    val nameError: Boolean = false,
    val saveError: Boolean = false,
    val isSaving: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetFormScreen(
    state: AssetFormState,
    onStateChange: (AssetFormState) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onChoosePhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    onPickPurchaseDate: () -> Unit = {},
    onPickWarrantyDate: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.assetId == null) R.string.add_asset else R.string.edit_asset)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !state.isSaving) { Text(stringResource(R.string.save)) }
                },
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
            FormSection(stringResource(R.string.basics))
            if (state.saveError) {
                Text(stringResource(R.string.asset_save_failed), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }
            Text(stringResource(R.string.photo), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onTakePhoto) { Text(stringResource(R.string.take_photo)) }
                OutlinedButton(onClick = onChoosePhoto) { Text(stringResource(R.string.choose_photo)) }
            }
            Spacer(Modifier.height(16.dp))
            Field(
                value = state.name,
                label = stringResource(R.string.name),
                onValue = { onStateChange(state.copy(name = it, nameError = false)) },
                error = state.nameError,
                supporting = if (state.nameError) stringResource(R.string.name_required) else null,
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.category), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stringArrayResource(R.array.asset_categories).forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { onStateChange(state.copy(category = if (state.category == category) "" else category)) },
                        label = { Text(category) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Field(state.location, stringResource(R.string.location), { onStateChange(state.copy(location = it)) })

            FormSection(stringResource(R.string.product_details))
            Field(state.manufacturer, stringResource(R.string.manufacturer), { onStateChange(state.copy(manufacturer = it)) })
            Spacer(Modifier.height(12.dp))
            Field(state.modelNumber, stringResource(R.string.model_number), { onStateChange(state.copy(modelNumber = it)) })
            Spacer(Modifier.height(12.dp))
            Field(state.serialNumber, stringResource(R.string.serial_number), { onStateChange(state.copy(serialNumber = it)) })

            FormSection(stringResource(R.string.purchase_and_warranty))
            OutlinedButton(onClick = onPickPurchaseDate, modifier = Modifier.fillMaxWidth()) {
                Text(formattedDate(state.purchaseDate, R.string.purchase_date))
            }
            Spacer(Modifier.height(12.dp))
            Field(state.retailer, stringResource(R.string.retailer), { onStateChange(state.copy(retailer = it)) })
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPickWarrantyDate, modifier = Modifier.fillMaxWidth()) {
                Text(formattedDate(state.warrantyExpirationDate, R.string.warranty_expiration_date))
            }

            FormSection(stringResource(R.string.notes))
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
private fun formattedDate(value: String, emptyLabel: Int): String =
    if (value.isBlank()) stringResource(emptyLabel) else LocalDate.parse(value).localized()

@Composable
private fun FormSection(title: String) {
    Spacer(Modifier.height(HomeSpacing.section))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Field(
    value: String,
    label: String,
    onValue: (String) -> Unit,
    error: Boolean = false,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        isError = error,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
