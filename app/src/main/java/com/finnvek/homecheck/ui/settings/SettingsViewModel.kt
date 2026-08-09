package com.finnvek.homecheck.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.homecheck.BuildConfig
import com.finnvek.homecheck.backup.BackupManager
import com.finnvek.homecheck.billing.BillingManager
import com.finnvek.homecheck.data.preferences.ThemeMode
import com.finnvek.homecheck.data.preferences.UserPreferences
import com.finnvek.homecheck.data.preferences.UserPreferencesRepository
import com.finnvek.homecheck.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsEvent { BACKUP_SUCCEEDED, BACKUP_FAILED, RESTORE_SUCCEEDED, RESTORE_FAILED }

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val preferencesRepository: UserPreferencesRepository,
        private val backupManager: BackupManager,
        private val billingManager: BillingManager,
    ) : ViewModel() {
        private val working = MutableStateFlow(false)
        private val mutableEvents = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
        val events = mutableEvents.asSharedFlow()

        val state =
            combine(preferencesRepository.preferences, billingManager.state, working) { preferences, billing, busy ->
                SettingsUiState(preferences, billing, BuildConfig.VERSION_NAME, isWorking = busy)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        fun setTheme(value: ThemeMode) = viewModelScope.launch { preferencesRepository.setThemeMode(value) }

        fun setDynamicColor(value: Boolean) = viewModelScope.launch { preferencesRepository.setDynamicColor(value) }

        fun setReminders(value: Boolean) =
            viewModelScope.launch {
                preferencesRepository.setRemindersEnabled(value)
                if (value) {
                    val preferences = preferencesRepository.preferences.first()
                    ReminderScheduler.schedule(context, preferences.reminderHour, preferences.reminderMinute)
                } else {
                    ReminderScheduler.cancel(context)
                }
            }

        fun setReminderTime(
            hour: Int,
            minute: Int,
        ) = viewModelScope.launch {
            preferencesRepository.setReminderTime(hour, minute)
            if (preferencesRepository.preferences.first().remindersEnabled) ReminderScheduler.schedule(context, hour, minute)
        }

        fun backup(uri: Uri) =
            viewModelScope.launch {
                working.value = true
                runCatching { backupManager.export(uri) }
                    .onSuccess { mutableEvents.emit(SettingsEvent.BACKUP_SUCCEEDED) }
                    .onFailure { mutableEvents.emit(SettingsEvent.BACKUP_FAILED) }
                working.value = false
            }

        fun restore(uri: Uri) =
            viewModelScope.launch {
                working.value = true
                runCatching { backupManager.restore(uri) }
                    .onSuccess { mutableEvents.emit(SettingsEvent.RESTORE_SUCCEEDED) }
                    .onFailure { mutableEvents.emit(SettingsEvent.RESTORE_FAILED) }
                working.value = false
            }

        fun restorePurchase() = billingManager.refreshPurchases()
    }
