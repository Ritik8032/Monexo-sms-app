package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.data.repository.MonitorRepository
import com.example.ui.MonitorViewModel
import com.example.ui.screens.MonitorDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy { MonitorRepository(applicationContext) }
    private val viewModel: MonitorViewModel by viewModels {
        MonitorViewModel.Factory(repository, applicationContext)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
        viewModel.fetchSystemSms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MonitorDashboardScreen(
                    viewModel = viewModel,
                    onRequestSmsPermission = { requestSmsPermissions() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    private fun requestSmsPermissions() {
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}
