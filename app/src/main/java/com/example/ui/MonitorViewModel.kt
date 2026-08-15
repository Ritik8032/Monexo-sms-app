package com.example.ui

import android.content.Context
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationEntity
import com.example.data.model.SmsEntity
import com.example.data.repository.MongoManager
import com.example.data.repository.MonitorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorViewModel(
    private val repository: MonitorRepository,
    private val context: Context
) : ViewModel() {

    val mongoManager = MongoManager()

    val mongoSyncStatus = mongoManager.syncStatus
    val mongoIsSyncing = mongoManager.isSyncing
    val mongoLastSyncTime = mongoManager.lastSyncTime

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _smsFilterCategory = MutableStateFlow("All")
    val smsFilterCategory: StateFlow<String> = _smsFilterCategory.asStateFlow()

    private val _notifFilterCategory = MutableStateFlow("All")
    val notifFilterCategory: StateFlow<String> = _notifFilterCategory.asStateFlow()

    private val _isLiveSyncActive = MutableStateFlow(true)
    val isLiveSyncActive: StateFlow<Boolean> = _isLiveSyncActive.asStateFlow()

    private val _smsPermissionGranted = MutableStateFlow(false)
    val smsPermissionGranted: StateFlow<Boolean> = _smsPermissionGranted.asStateFlow()

    private val _notifListenerGranted = MutableStateFlow(false)
    val notifListenerGranted: StateFlow<Boolean> = _notifListenerGranted.asStateFlow()

    init {
        checkPermissions()
        viewModelScope.launch {
            repository.clearAllLogs()
            mongoManager.testConnection()
        }
    }

    val filteredSmsList: StateFlow<List<SmsEntity>> = combine(
        repository.allSms,
        _searchQuery,
        _smsFilterCategory
    ) { smsList, query, category ->
        smsList.filter { sms ->
            val matchesQuery = query.isEmpty() ||
                    sms.number.contains(query, ignoreCase = true) ||
                    sms.body.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || sms.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredNotifList: StateFlow<List<NotificationEntity>> = combine(
        repository.allNotifications,
        _searchQuery,
        _notifFilterCategory
    ) { notifList, query, category ->
        notifList.filter { notif ->
            val matchesQuery = query.isEmpty() ||
                    notif.packageName.contains(query, ignoreCase = true) ||
                    notif.title.contains(query, ignoreCase = true) ||
                    notif.content.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || notif.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun checkPermissions() {
        val hasReadSms = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        _smsPermissionGranted.value = hasReadSms

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        val packageName = context.packageName
        _notifListenerGranted.value = enabledListeners.contains(packageName)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSmsCategory(category: String) {
        _smsFilterCategory.value = category
    }

    fun setNotifCategory(category: String) {
        _notifFilterCategory.value = category
    }

    fun toggleLiveSync() {
        _isLiveSyncActive.value = !_isLiveSyncActive.value
    }

    fun fetchSystemSms() {
        viewModelScope.launch {
            if (_smsPermissionGranted.value) {
                repository.readSystemInboxSms(context)
            }
        }
    }

    fun simulateTestSms(sender: String? = null, bodyText: String? = null) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val now = System.currentTimeMillis()

            val senders = listOf("AX-ICICIBK", "VM-AMAZON", "+1 (555) 012-3456", "JD-UBER", "Google")
            val bodies = listOf(
                "Your OTP for payment of Rs. 1,299 is 482910. Valid for 10 minutes. Do not share.",
                "Your Amazon order #408-281903-11 has been dispatched and will arrive today.",
                "Hey! Are we still meeting for lunch today at 1 PM?",
                "Your Uber trip is arriving now. Vehicle: White Swift Dzire (KA 01 EB 4819).",
                "Your security alert: New sign-in detected on Android device."
            )

            val chosenSender = sender.takeUnless { it.isNull_or_blank() } ?: senders.random()
            val chosenBody = bodyText.takeUnless { it.isNull_or_blank() } ?: bodies.random()

            val lower = chosenBody.lowercase()
            val category = when {
                lower.contains("otp") || lower.contains("code") -> "OTP"
                lower.contains("rs.") || lower.contains("payment") || lower.contains("acct") -> "Banking"
                lower.contains("offer") || lower.contains("discount") -> "Promo"
                else -> "Personal"
            }

            val sms = SmsEntity(
                number = chosenSender,
                body = chosenBody,
                date = timeFormat.format(Date(now)),
                timestamp = now,
                category = category,
                isRead = false
            )

            repository.insertSms(sms)
        }
    }

    fun simulateTestNotification(appName: String? = null, titleText: String? = null, contentText: String? = null) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val now = System.currentTimeMillis()

            val packages = listOf("com.whatsapp", "com.instagram.android", "com.google.android.gm", "com.twitter.android", "com.paytm.app")
            val titles = listOf("New message from Alex", "Photo liked by @design_art", "New Email: Weekly Digest", "Trending Topic in Tech", "Paytm Cash Cashback!")
            val contents = listOf(
                "Hey! Check out the new live dashboard feature.",
                "User @design_art and 12 others liked your post.",
                "Your weekly project summary report is ready.",
                "AI and Jetpack Compose updates released!",
                "You won ₹50 cashback on your last recharge."
            )

            val chosenPkg = appName.takeUnless { it.isNull_or_blank() } ?: packages.random()
            val chosenTitle = titleText.takeUnless { it.isNull_or_blank() } ?: titles.random()
            val chosenContent = contentText.takeUnless { it.isNull_or_blank() } ?: contents.random()

            val category = when {
                chosenPkg.contains("whatsapp") || chosenPkg.contains("instagram") || chosenPkg.contains("twitter") -> "Social"
                chosenPkg.contains("paytm") -> "Finance"
                else -> "General"
            }

            val notif = NotificationEntity(
                packageName = chosenPkg,
                title = chosenTitle,
                content = chosenContent,
                time = timeFormat.format(Date(now)),
                timestamp = now,
                category = category
            )

            repository.insertNotification(notif)
        }
    }

    fun deleteSms(id: Long) {
        viewModelScope.launch {
            repository.deleteSms(id)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllSms() {
        viewModelScope.launch {
            repository.clearSms()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    class Factory(
        private val repository: MonitorRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MonitorViewModel(repository, context) as T
        }
    }
}
