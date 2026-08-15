package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.NotificationEntity
import com.example.data.model.SmsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val smsDao = db.smsDao()
    private val notifDao = db.notificationDao()

    val allSms: Flow<List<SmsEntity>> = smsDao.getAllSms()
    val allNotifications: Flow<List<NotificationEntity>> = notifDao.getAllNotifications()

    suspend fun insertSms(sms: SmsEntity) {
        smsDao.insertSms(sms)
    }

    suspend fun deleteSms(id: Long) {
        smsDao.deleteSmsById(id)
    }

    suspend fun clearSms() {
        smsDao.clearAll()
    }

    suspend fun insertNotification(notification: NotificationEntity) {
        notifDao.insertNotification(notification)
    }

    suspend fun deleteNotification(id: Long) {
        notifDao.deleteNotificationById(id)
    }

    suspend fun clearNotifications() {
        notifDao.clearAll()
    }

    suspend fun clearAllLogs() {
        smsDao.clearAll()
        notifDao.clearAll()
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = System.currentTimeMillis()

        if (smsDao.getCount() == 0) {
            val sampleSms = listOf(
                SmsEntity(
                    number = "AX-HDFCBK",
                    body = "Your Acct XX9021 has been credited with Rs. 15,000.00 on 15-AUG-2026. Avail Bal: Rs. 48,210.50.",
                    date = timeFormat.format(Date(now - 300000)),
                    timestamp = now - 300000,
                    category = "Banking"
                ),
                SmsEntity(
                    number = "VM-GOOGLE",
                    body = "G-829104 is your Google verification code. Do not share this OTP with anyone.",
                    date = timeFormat.format(Date(now - 900000)),
                    timestamp = now - 900000,
                    category = "OTP"
                ),
                SmsEntity(
                    number = "+1 (555) 019-2834",
                    body = "Hey! Let's meet up at the cafe around 5 PM today.",
                    date = timeFormat.format(Date(now - 3600000)),
                    timestamp = now - 3600000,
                    category = "Personal"
                ),
                SmsEntity(
                    number = "JD-SWIGGY",
                    body = "FLAT 50% OFF on your favorite food orders! Use promo code WEEKEND50 at checkout.",
                    date = timeFormat.format(Date(now - 86400000)),
                    timestamp = now - 86400000,
                    category = "Promo"
                )
            )
            smsDao.insertAll(sampleSms)
        }

        if (notifDao.getCount() == 0) {
            val sampleNotifs = listOf(
                NotificationEntity(
                    packageName = "com.whatsapp",
                    title = "WhatsApp - Sarah",
                    content = "Hey, did you get the project documentation report?",
                    time = timeFormat.format(Date(now - 120000)),
                    timestamp = now - 120000,
                    category = "Social"
                ),
                NotificationEntity(
                    packageName = "com.google.android.apps.messaging",
                    title = "New Message from VM-GOOGLE",
                    content = "G-829104 is your Google verification code.",
                    time = timeFormat.format(Date(now - 900000)),
                    timestamp = now - 900000,
                    category = "General"
                ),
                NotificationEntity(
                    packageName = "com.paytm.app",
                    title = "Payment Received",
                    content = "Received ₹250.00 from Rahul via UPI Scan & Pay.",
                    time = timeFormat.format(Date(now - 1800000)),
                    timestamp = now - 1800000,
                    category = "Finance"
                ),
                NotificationEntity(
                    packageName = "android",
                    title = "System Update Ready",
                    content = "Security patch update available for install.",
                    time = timeFormat.format(Date(now - 43200000)),
                    timestamp = now - 43200000,
                    category = "System"
                )
            )
            notifDao.insertAll(sampleNotifs)
        }
    }

    suspend fun readSystemInboxSms(context: Context) = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val uri = Uri.parse("content://sms/inbox")
            val cursor = contentResolver.query(
                uri,
                arrayOf("_id", "address", "body", "date"),
                null,
                null,
                "date DESC LIMIT 30"
            )

            cursor?.use {
                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val addressIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                val smsList = mutableListOf<SmsEntity>()
                while (it.moveToNext()) {
                    val address = if (addressIdx != -1) it.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val dateLong = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    val lower = body.lowercase()
                    val category = when {
                        lower.contains("otp") || lower.contains("code") || lower.contains("verification") -> "OTP"
                        lower.contains("debit") || lower.contains("credit") || lower.contains("bank") || lower.contains("acct") -> "Banking"
                        lower.contains("off") || lower.contains("sale") || lower.contains("promo") -> "Promo"
                        else -> "Personal"
                    }

                    smsList.add(
                        SmsEntity(
                            number = address,
                            body = body,
                            date = timeFormat.format(Date(dateLong)),
                            timestamp = dateLong,
                            category = category
                        )
                    )
                }
                if (smsList.isNotEmpty()) {
                    smsDao.insertAll(smsList)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
