package com.homeinventory.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.homeinventory.data.InventoryDatabase
import com.homeinventory.domain.ExpiryStatus
import com.homeinventory.domain.expiryStatus
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ExpiryNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val items = InventoryDatabase.getInstance(applicationContext).dao().observeItems().first()
        val preferences = applicationContext.getSharedPreferences("expiry_notifications", Context.MODE_PRIVATE)
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }
        createChannel(applicationContext)
        items.forEach { row ->
            val status = expiryStatus(row.item.expiryDate)
            if (status != ExpiryStatus.SOON && status != ExpiryStatus.EXPIRED) return@forEach
            val key = "${row.item.id}_${status.name}_${row.item.expiryDate}"
            if (preferences.getBoolean(key, false)) return@forEach
            val message = if (status == ExpiryStatus.SOON) "${row.item.name} expires within 7 days." else "${row.item.name} has expired."

            val publicNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(com.homeinventory.R.drawable.ic_notification_expiry)
                .setContentTitle("Inventory alert")
                .setContentText(if (status == ExpiryStatus.SOON) "An item is expiring soon." else "An item has expired.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val privateNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(com.homeinventory.R.drawable.ic_notification_expiry)
                .setContentTitle(if (status == ExpiryStatus.SOON) "Item expiring soon" else "Item expired")
                .setContentText(message)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(applicationContext).notify(row.item.id.toInt() * 10 + status.ordinal, privateNotification)
                preferences.edit().putBoolean(key, true).apply()
            } catch (_: SecurityException) {}
        }
        return Result.success()
    }
    companion object {
        private const val CHANNEL_ID = "expiry_alerts"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("expiry_alerts", ExistingPeriodicWorkPolicy.KEEP, request)
        }
        private fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Expiry alerts", NotificationManager.IMPORTANCE_HIGH).apply { description = "Alerts for expiring and expired inventory items" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
