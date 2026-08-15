package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MonitorViewModel
import com.example.ui.components.StatusBadge

@Composable
fun MonitorDashboardScreen(
    viewModel: MonitorViewModel,
    onRequestSmsPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val smsFilterCategory by viewModel.smsFilterCategory.collectAsStateWithLifecycle()
    val notifFilterCategory by viewModel.notifFilterCategory.collectAsStateWithLifecycle()
    val isLiveSyncActive by viewModel.isLiveSyncActive.collectAsStateWithLifecycle()

    val smsList by viewModel.filteredSmsList.collectAsStateWithLifecycle()
    val notifList by viewModel.filteredNotifList.collectAsStateWithLifecycle()

    val smsPermissionGranted by viewModel.smsPermissionGranted.collectAsStateWithLifecycle()
    val notifListenerGranted by viewModel.notifListenerGranted.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HeaderTopBar(
                isLiveSyncActive = isLiveSyncActive,
                onToggleSync = { viewModel.toggleLiveSync() },
                smsCount = smsList.size,
                notifCount = notifList.size
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelect = { viewModel.setTab(it) }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            viewModel.simulateTestSms()
                        } else {
                            viewModel.simulateTestNotification()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_simulate_quick")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Test Data")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SmsTab(
                    smsList = smsList,
                    searchQuery = searchQuery,
                    selectedCategory = smsFilterCategory,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.setSmsCategory(it) },
                    onDeleteSms = { viewModel.deleteSms(it) },
                    onSimulateSmsClick = { viewModel.simulateTestSms() }
                )
                1 -> NotificationTab(
                    notifList = notifList,
                    searchQuery = searchQuery,
                    selectedCategory = notifFilterCategory,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.setNotifCategory(it) },
                    onDeleteNotif = { viewModel.deleteNotification(it) },
                    onSimulateNotifClick = { viewModel.simulateTestNotification() }
                )
                2 -> LocalApiTab(
                    smsList = smsList,
                    notifList = notifList,
                    isLiveSyncActive = isLiveSyncActive
                )
                3 -> SimulatorTab(
                    smsPermissionGranted = smsPermissionGranted,
                    notifListenerGranted = notifListenerGranted,
                    onRequestSmsPermission = onRequestSmsPermission,
                    onFetchSystemSms = { viewModel.fetchSystemSms() },
                    onSimulateSms = { sender, body -> viewModel.simulateTestSms(sender, body) },
                    onSimulateNotif = { app, title, text -> viewModel.simulateTestNotification(app, title, text) },
                    onSeedSampleData = { viewModel.seedSampleData() },
                    onClearAllLogs = { viewModel.clearAllLogs() }
                )
            }
        }
    }
}

@Composable
private fun HeaderTopBar(
    isLiveSyncActive: Boolean,
    onToggleSync: () -> Unit,
    smsCount: Int,
    notifCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Termux Monitor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active Node: localhost:5000",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(isActive = isLiveSyncActive)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isLiveSyncActive,
                        onCheckedChange = { onToggleSync() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("toggle_live_sync_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Inbox SMS",
                    count = "$smsCount",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                MetricSummaryCard(
                    title = "Notifications",
                    count = "$notifCount",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = count,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelect(0) },
            icon = { Icon(Icons.Default.Message, contentDescription = "SMS") },
            label = { Text("SMS", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_sms")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelect(1) },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifs") },
            label = { Text("Notifs", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_notifs")
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelect(2) },
            icon = { Icon(Icons.Default.Code, contentDescription = "Local API") },
            label = { Text("Local API", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_local_api")
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelect(3) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Simulator") },
            label = { Text("Simulator", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_simulator")
        )
    }
}
