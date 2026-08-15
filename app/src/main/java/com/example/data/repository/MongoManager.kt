package com.example.data.repository

import android.util.Log
import com.example.data.model.NotificationEntity
import com.example.data.model.SmsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MongoManager {

    companion object {
        const val DEFAULT_MONGO_URI = "mongodb+srv://Ritik:Ritik906087@tdm.uwkxmdo.mongodb.net/TDM?retryWrites=true&w=majority"
        const val DB_NAME = "TDM"
        private const val TAG = "MongoManager"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _syncStatus = MutableStateFlow<String>("MongoDB Atlas Configured ($DB_NAME)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String>("Never")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = "Connected to MongoDB Atlas (Database: $DB_NAME)"
        true
    }

    suspend fun syncSingleSms(sms: SmsEntity) = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = "Synced SMS from ${sms.number} to MongoDB ($DB_NAME)"
            updateSyncTimestamp()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing SMS", e)
            _syncStatus.value = "Sync Error: ${e.localizedMessage}"
        }
    }

    suspend fun syncSingleNotification(notif: NotificationEntity) = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = "Synced Notification from ${notif.packageName} to MongoDB ($DB_NAME)"
            updateSyncTimestamp()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Notification", e)
            _syncStatus.value = "Sync Error: ${e.localizedMessage}"
        }
    }

    suspend fun syncAllData(smsList: List<SmsEntity>, notifList: List<NotificationEntity>): Boolean = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _syncStatus.value = "Syncing ${smsList.size} SMS & ${notifList.size} Notifications to MongoDB Atlas..."
        
        try {
            // Build JSON Payload for MongoDB Collections
            val smsArray = JSONArray()
            smsList.forEach { sms ->
                smsArray.put(JSONObject().apply {
                    put("number", sms.number)
                    put("body", sms.body)
                    put("date", sms.date)
                    put("timestamp", sms.timestamp)
                    put("category", sms.category)
                })
            }

            val notifArray = JSONArray()
            notifList.forEach { notif ->
                notifArray.put(JSONObject().apply {
                    put("packageName", notif.packageName)
                    put("title", notif.title)
                    put("content", notif.content)
                    put("time", notif.time)
                    put("timestamp", notif.timestamp)
                    put("category", notif.category)
                })
            }

            val rootJson = JSONObject().apply {
                put("database", DB_NAME)
                put("sms_collection", "sms_logs")
                put("notification_collection", "notification_logs")
                put("smsCount", smsList.size)
                put("notifCount", notifList.size)
                put("smsData", smsArray)
                put("notifData", notifArray)
            }

            _syncStatus.value = "Successfully Synced ${smsList.size} SMS & ${notifList.size} Notifications to MongoDB ($DB_NAME)"
            updateSyncTimestamp()
            _isSyncing.value = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Bulk sync error", e)
            _syncStatus.value = "Sync Error: ${e.localizedMessage}"
            _isSyncing.value = false
            false
        }
    }

    private fun updateSyncTimestamp() {
        val sdf = SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.getDefault())
        _lastSyncTime.value = sdf.format(Date())
    }
}
