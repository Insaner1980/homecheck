package com.finnvek.homecheck.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finnvek.homecheck.R
import com.finnvek.homecheck.EXTRA_NOTIFICATION_TARGET
import com.finnvek.homecheck.MainActivity
import com.finnvek.homecheck.NOTIFICATION_TARGET_ASSET_PREFIX
import com.finnvek.homecheck.NOTIFICATION_TARGET_MAINTENANCE
import com.finnvek.homecheck.data.preferences.UserPreferencesRepository
import com.finnvek.homecheck.data.repository.HomeRepository
import com.finnvek.homecheck.domain.MaintenanceNotificationRules
import com.finnvek.homecheck.domain.WarrantyRules
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: HomeRepository,
    private val preferences: UserPreferencesRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!preferences.preferences.first().remindersEnabled || !notificationsAvailable()) return Result.success()
        val today = LocalDate.now()
        val assets = repository.assets.first()
        val assetNames = assets.associate { it.id to it.name }
        val maintenance = repository.tasks.first()
            .filter { it.reminderEnabled && MaintenanceNotificationRules.shouldNotify(it.dueDate, today) }
            .map { ReminderItem(it.title, assetNames[it.assetId].orEmpty(), it.assetId) }
        val warranties = assets
            .filter { it.warrantyExpirationDate?.let { date -> WarrantyRules.shouldNotify(date, today) } == true }
            .map { ReminderItem(context.getString(R.string.warranty_expiring), it.name, it.id) }
        val reminders = maintenance + warranties
        if (reminders.isNotEmpty()) show(reminders)
        return Result.success()
    }

    private fun show(items: List<ReminderItem>) {
        val target = if (items.size == 1) NOTIFICATION_TARGET_ASSET_PREFIX + items.single().assetId else NOTIFICATION_TARGET_MAINTENANCE
        val launchIntent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_NOTIFICATION_TARGET, target)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            target.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        if (items.size == 1) {
            val item = items.single()
            builder.setContentTitle(item.title).setContentText(item.assetName)
        } else {
            builder
                .setContentTitle(context.resources.getQuantityString(R.plurals.notification_summary_title, items.size, items.size))
                .setContentText(context.getString(R.string.notification_summary_body))
                .setStyle(NotificationCompat.InboxStyle().also { style ->
                    items.take(5).forEach { style.addLine("${it.title} · ${it.assetName}") }
                })
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and the notify call.
        }
    }

    private fun notificationsAvailable(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private data class ReminderItem(val title: String, val assetName: String, val assetId: String)

    private companion object { const val REMINDER_NOTIFICATION_ID = 1001 }
}
