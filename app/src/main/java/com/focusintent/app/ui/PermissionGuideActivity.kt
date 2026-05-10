package com.focusintent.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.focusintent.app.service.OverlayService

/**
 * Permission guide activity for directing users to enable required permissions.
 * Handles overlay, notification, and battery optimization permissions.
 */
class PermissionGuideActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionGuideScreen(
                        activity = this,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        onIgnoreBatteryOptimization = { ignoreBatteryOptimization() },
                        onContinue = { 
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission status when returning from settings
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    private fun ignoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

@Composable
fun PermissionGuideScreen(
    activity: PermissionGuideActivity,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onIgnoreBatteryOptimization: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    
    // Use derivedStateOf to automatically refresh when activity resumes
    val overlayGranted by remember {
        derivedStateOf { hasOverlayPermission(context) }
    }
    val notificationGranted by remember {
        derivedStateOf { hasNotificationPermission(context) }
    }
    val batteryOptIgnored by remember {
        derivedStateOf { isIgnoringBatteryOptimization(context) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "权限引导",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "为确保悬浮窗常驻显示，需要开启以下权限：",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Overlay Permission Card
        PermissionCard(
            title = "悬浮窗权限",
            description = "允许应用在其他应用上层显示悬浮窗",
            isGranted = overlayGranted,
            onAction = onRequestOverlayPermission
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notification Permission Card
        PermissionCard(
            title = "通知权限",
            description = "用于显示前台服务通知（Android 13+）",
            isGranted = notificationGranted,
            onAction = onRequestNotificationPermission
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Battery Optimization Card
        PermissionCard(
            title = "电池优化",
            description = "建议关闭电池优化以保证后台运行",
            isGranted = batteryOptIgnored,
            onAction = onIgnoreBatteryOptimization
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("继续", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            if (isGranted) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = onAction) {
                    Text("授予")
                }
            }
        }
    }
}

fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun isIgnoringBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}
