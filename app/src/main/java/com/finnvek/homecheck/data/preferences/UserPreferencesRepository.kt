package com.finnvek.homecheck.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val premiumCached: Boolean = false,
)

private val Context.dataStore by preferencesDataStore("homecheck_preferences")

@Singleton
class UserPreferencesRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val preferences: Flow<UserPreferences> =
            context.dataStore.data.map { values ->
                UserPreferences(
                    onboardingComplete = values[ONBOARDING_COMPLETE] ?: false,
                    themeMode = values[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                    dynamicColor = values[DYNAMIC_COLOR] ?: false,
                    remindersEnabled = values[REMINDERS_ENABLED] ?: true,
                    reminderHour = values[REMINDER_HOUR]?.coerceIn(MIN_REMINDER_HOUR, MAX_REMINDER_HOUR) ?: 9,
                    reminderMinute = values[REMINDER_MINUTE]?.coerceIn(MIN_REMINDER_MINUTE, MAX_REMINDER_MINUTE) ?: 0,
                    premiumCached = values[PREMIUM_CACHED] ?: false,
                )
            }

        suspend fun completeOnboarding() = edit { it[ONBOARDING_COMPLETE] = true }

        suspend fun setThemeMode(value: ThemeMode) = edit { it[THEME_MODE] = value.name }

        suspend fun setDynamicColor(value: Boolean) = edit { it[DYNAMIC_COLOR] = value }

        suspend fun setRemindersEnabled(value: Boolean) = edit { it[REMINDERS_ENABLED] = value }

        suspend fun setReminderTime(
            hour: Int,
            minute: Int,
        ) = edit {
            it[REMINDER_HOUR] = hour.coerceIn(MIN_REMINDER_HOUR, MAX_REMINDER_HOUR)
            it[REMINDER_MINUTE] = minute.coerceIn(MIN_REMINDER_MINUTE, MAX_REMINDER_MINUTE)
        }

        suspend fun setPremiumCached(value: Boolean) = edit { it[PREMIUM_CACHED] = value }

        private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
            context.dataStore.edit(block)
        }

        private companion object {
            const val MIN_REMINDER_HOUR = 0
            const val MAX_REMINDER_HOUR = 23
            const val MIN_REMINDER_MINUTE = 0
            const val MAX_REMINDER_MINUTE = 59
            val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
            val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
            val REMINDER_HOUR = intPreferencesKey("reminder_hour")
            val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
            val PREMIUM_CACHED = booleanPreferencesKey("premium_cached")
        }
    }
