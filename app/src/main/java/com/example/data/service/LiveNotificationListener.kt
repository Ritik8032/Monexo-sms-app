package com.example.data.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.db.AppDatabase
import com.example.data.model.NotificationEntity
import com.example.data.repository.MongoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName ?: "Unknown"
        // Ignore self notifications to avoid feedback loop
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        val postTimeMs = sbn.postTime
        val formattedTime = timeFormat.format(Date(postTimeMs))

        val category = when {
            packageName.contains("whatsapp") || packageName.contains("instagram") ||
                    packageName.contains("telegram") || packageName.contains("facebook") ||
                    packageName.contains("twitter") || packageName.contains("discord") -> "Social"

            packageName.contains("paytm") || packageName.contains("gpay") ||
                    packageName.contains("bank") || packageName.contains("finance") ||
                    packageName.contains("paypal") || packageName.contains("wallet") -> "Finance"

            packageName.contains("android") || packageName.contains("system") ||
                    packageName.contains("settings") -> "System"

            else -> "General"
        }

        val notificationItem = NotificationEntity(
            packageName = packageName,
            title = title,
            content = text,
            time = formattedTime,
            timestamp = if (postTimeMs > 0) postTimeMs else System.currentTimeMillis(),
            category = category
        )

        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                db.notificationDao().insertNotification(notificationItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
