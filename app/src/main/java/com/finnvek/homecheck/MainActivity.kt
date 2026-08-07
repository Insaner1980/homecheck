package com.finnvek.homecheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.finnvek.homecheck.ui.HomeCheckApp
import dagger.hilt.android.AndroidEntryPoint

const val EXTRA_NOTIFICATION_TARGET = "notification_target"
const val NOTIFICATION_TARGET_MAINTENANCE = "maintenance"
const val NOTIFICATION_TARGET_ASSET_PREFIX = "asset:"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        notificationTarget = intent.getStringExtra(EXTRA_NOTIFICATION_TARGET)
        enableEdgeToEdge()
        setContent {
            HomeCheckApp(
                notificationTarget = notificationTarget,
                onNotificationHandled = { notificationTarget = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTarget = intent.getStringExtra(EXTRA_NOTIFICATION_TARGET)
    }
}
