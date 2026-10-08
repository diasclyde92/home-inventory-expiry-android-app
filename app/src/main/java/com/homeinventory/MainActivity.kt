package com.homeinventory
import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homeinventory.ui.InventoryApp
import com.homeinventory.notifications.ExpiryNotificationWorker

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExpiryNotificationWorker.schedule(this)

        setContent {
            val context = this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* App operates normally whether granted or denied */ }

                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("app_permission_state", Context.MODE_PRIVATE)
                    val requestedBefore = prefs.getBoolean("notification_prompt_requested", false)
                    if (!requestedBefore) {
                        prefs.edit().putBoolean("notification_prompt_requested", true).apply()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }
            ) {
                InventoryApp(viewModel())
            }
        }
    }
}
