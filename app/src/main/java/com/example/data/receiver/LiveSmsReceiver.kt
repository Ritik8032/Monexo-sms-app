package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.db.AppDatabase
import com.example.data.model.SmsEntity
import com.example.data.repository.MongoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveSmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val nowMs = System.currentTimeMillis()
            val formattedDate = timeFormat.format(Date(nowMs))

            for (smsMessage in messages) {
                val number = smsMessage.displayOriginatingAddress ?: smsMessage.originatingAddress ?: "Unknown"
                val body = smsMessage.messageBody ?: ""

                val category = classifySms(body)

                val smsEntity = SmsEntity(
                    number = number,
                    body = body,
                    date = formattedDate,
                    timestamp = nowMs,
                    category = category,
                    isRead = false
                )

                receiverScope.launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        db.smsDao().insertSms(smsEntity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun classifySms(body: String): String {
        val lower = body.lowercase()
        return when {
            lower.contains("otp") || lower.contains("code") || lower.contains("verification") || lower.contains("pin") -> "OTP"
            lower.contains("debit") || lower.contains("credit") || lower.contains("bank") || lower.contains("inr") || lower.contains("usd") || lower.contains("acct") || lower.contains("balance") -> "Banking"
            lower.contains("off") || lower.contains("sale") || lower.contains("promo") || lower.contains("discount") || lower.contains("coupon") -> "Promo"
            else -> "Personal"
        }
    }
}
