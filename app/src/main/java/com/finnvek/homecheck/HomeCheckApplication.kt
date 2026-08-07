package com.finnvek.homecheck

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.finnvek.homecheck.notifications.ReminderScheduler
import com.finnvek.homecheck.data.preferences.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class HomeCheckApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferencesRepository: UserPreferencesRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        applicationScope.launch {
            val preferences = preferencesRepository.preferences.first()
            if (preferences.remindersEnabled) {
                ReminderScheduler.schedule(this@HomeCheckApplication, preferences.reminderHour, preferences.reminderMinute)
            } else {
                ReminderScheduler.cancel(this@HomeCheckApplication)
            }
        }
    }
}
