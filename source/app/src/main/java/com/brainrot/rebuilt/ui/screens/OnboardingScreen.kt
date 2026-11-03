package com.brainrot.rebuilt.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.R
import com.brainrot.rebuilt.service.ReelsAccessibilityService
import com.brainrot.rebuilt.ui.components.MascotView
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = BrainPalApp.instance
    var currentStep by remember { mutableStateOf(0) }

    fun checkUsage(): Boolean = app.usageRepository.hasUsageStatsPermission()
    fun checkOverlay(): Boolean = Settings.canDrawOverlays(context)
    fun checkAccessibility(): Boolean {
        val expected = "${context.packageName}/${ReelsAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabled.contains(expected) || ReelsAccessibilityService.isServiceConnected
    }
    fun checkNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var hasUsageAccess by remember { mutableStateOf(checkUsage()) }
    var hasOverlayPermission by remember { mutableStateOf(checkOverlay()) }
    var hasAccessibilityPermission by remember { mutableStateOf(checkAccessibility()) }
    var hasNotificationPermission by remember { mutableStateOf(checkNotification()) }

    fun refreshPermissions() {
        hasUsageAccess = checkUsage()
        hasOverlayPermission = checkOverlay()
        hasAccessibilityPermission = checkAccessibility()
        hasNotificationPermission = checkNotification()
    }

    // DEF-08 FIX: Lifecycle observer refreshes permission checks as soon as user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(if (index == currentStep) 32.dp else 12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index <= currentStep) CoralPrimary else Color.DarkGray)
                )
            }
        }

        when (currentStep) {
            0 -> {
                // Step 0: Welcome & Mascot Introduction
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    MascotView(
                        resId = R.drawable.br_brain_standing_1,
                        contentDescription = "Brain Mascot",
                        modifier = Modifier.size(160.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Meet BrainPal",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your companion to break the endless scrolling loop, reclaim your focus, and protect your dopamine.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            1 -> {
                // Step 1: Goal setting
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    MascotView(
                        resId = R.drawable.br_brain_standing_2,
                        contentDescription = "Brain Goal",
                        modifier = Modifier.size(140.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Choose Your Daily Goal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We softly remind and roast you when you exceed your set boundary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Default Limit: 30 minutes / day", fontWeight = FontWeight.Bold)
                            Text("Covers Instagram, YouTube Shorts, Snapchat, TikTok", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
            2 -> {
                // Step 2: System Permissions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Grant Focus Permissions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android requires these permissions to calculate screen time and display focus roasts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission 1: Usage Access
                    PermissionCard(
                        title = "Usage Access",
                        subtitle = "Tracks time spent in apps",
                        icon = Icons.Default.QueryStats,
                        isGranted = hasUsageAccess,
                        onEnable = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Permission 2: Overlay
                    PermissionCard(
                        title = "Display Over Apps",
                        subtitle = "Displays meme roast screen",
                        icon = Icons.Default.Layers,
                        isGranted = hasOverlayPermission,
                        onEnable = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // DEF-11 FIX: Permission 3: Accessibility for Short Video Detection
                    PermissionCard(
                        title = "Accessibility (Reels Counter)",
                        subtitle = "Counts short video scrolls",
                        icon = Icons.Default.Accessibility,
                        isGranted = hasAccessibilityPermission,
                        onEnable = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // DEF-11 FIX: Permission 4: Notifications
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionCard(
                            title = "Notifications",
                            subtitle = "Maintains ongoing focus guard",
                            icon = Icons.Default.Notifications,
                            isGranted = hasNotificationPermission,
                            onEnable = {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Back", color = Color.LightGray)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        app.prefsRepository.setOnboardingCompleted(true)
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = if (currentStep == 2) "Start BrainPal" else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onEnable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, contentDescription = null, tint = CoralPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess)
            } else {
                Button(
                    onClick = onEnable,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Enable", fontSize = 13.sp)
                }
            }
        }
    }
}
