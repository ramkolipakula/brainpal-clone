package com.brainrot.rebuilt.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess

data class AppItemModel(
    val packageName: String,
    val displayName: String
)

@Composable
fun AppLimitsScreen() {
    val context = LocalContext.current
    val app = BrainPalApp.instance
    var searchQuery by remember { mutableStateOf("") }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    // Phase 3: Observe authoritative StateFlows
    val globalMinutes by app.prefsRepository.dailyLimitMinutesFlow.collectAsState()
    val globalReels by app.prefsRepository.dailyReelLimitFlow.collectAsState()
    val blockedPackages by app.prefsRepository.blockedPackagesFlow.collectAsState()
    val perAppMinuteLimits by app.prefsRepository.perAppMinuteLimitsFlow.collectAsState()
    val perAppReelLimits by app.prefsRepository.perAppReelLimitsFlow.collectAsState()

    // Dynamic discovery of installed applications on the device
    val allApps = remember {
        val pm = context.packageManager
        val popularApps = listOf(
            "com.instagram.android" to "Instagram",
            "com.google.android.youtube" to "YouTube",
            "com.snapchat.android" to "Snapchat",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.facebook.katana" to "Facebook",
            "com.reddit.frontpage" to "Reddit",
            "com.twitter.android" to "X / Twitter",
            "com.pinterest" to "Pinterest"
        )

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedResolve = pm.queryIntentActivities(launcherIntent, 0)
        val installedItems = installedResolve
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .distinct()
            .map { pkg ->
                val label = try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                }
                AppItemModel(pkg, label)
            }

        // Combine popular presets with installed apps
        val popularModels = popularApps.map { AppItemModel(it.first, it.second) }
        val combined = (popularModels + installedItems).distinctBy { it.packageName }
        combined.sortedWith(compareByDescending<AppItemModel> { app.prefsRepository.isAppBlocked(it.packageName) }.thenBy { it.displayName.lowercase() })
    }

    val filteredApps = allApps.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    fun toggleAppGuarded(pkg: String, isGuarded: Boolean) {
        val current = blockedPackages.toMutableSet()
        if (isGuarded) {
            current.add(pkg)
        } else {
            current.remove(pkg)
        }
        app.prefsRepository.setBlockedPackages(current)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Target Apps & Limits",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CoralPrimary
            )
            Text(
                text = "BrainPal intercepts these apps when your quotas are reached",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Global Daily Limits Slider Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Global Default Quotas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Default quotas apply to all guarded apps unless custom overrides are set below",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Default Screen Time: $globalMinutes mins / day")
                    Slider(
                        value = globalMinutes.toFloat(),
                        onValueChange = {
                            app.prefsRepository.setDailyLimitMinutes(it.toInt())
                        },
                        valueRange = 10f..180f,
                        steps = 16,
                        colors = SliderDefaults.colors(thumbColor = CoralPrimary, activeTrackColor = CoralPrimary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Default Short Videos: $globalReels reels / day")
                    Slider(
                        value = globalReels.toFloat(),
                        onValueChange = {
                            app.prefsRepository.setDailyReelLimit(it.toInt())
                        },
                        valueRange = 10f..100f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = CoralPrimary, activeTrackColor = CoralPrimary)
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search ${allApps.size} installed apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // List of Apps
        items(filteredApps, key = { it.packageName }) { item ->
            val isGuarded = blockedPackages.contains(item.packageName)
            val hasCustomMinutes = app.prefsRepository.hasCustomAppLimitMinutes(item.packageName)
            val hasCustomReels = app.prefsRepository.hasCustomAppReelLimit(item.packageName)
            val currentAppLimitMins = perAppMinuteLimits[item.packageName] ?: globalMinutes
            val currentAppLimitReels = perAppReelLimits[item.packageName] ?: globalReels
            val isExpanded = expandedPackage == item.packageName

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    expandedPackage = if (isExpanded) null else item.packageName
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = if (isGuarded) CoralPrimary else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isGuarded) CoralPrimary.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isGuarded) "Guarded" else "Excluded",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGuarded) CoralPrimary else Color.Gray
                                        )
                                    }
                                }
                                if (isGuarded) {
                                    Text(
                                        text = "Time: ${currentAppLimitMins}m ${if (hasCustomMinutes) "(Custom)" else "(Default)"} • Reels: ${currentAppLimitReels} ${if (hasCustomReels) "(Custom)" else "(Default)"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasCustomMinutes || hasCustomReels) CoralPrimary else GreenSuccess
                                    )
                                } else {
                                    Text(
                                        text = "Excluded from monitoring",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isGuarded,
                                onCheckedChange = { toggleAppGuarded(item.packageName, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CoralPrimary,
                                    checkedTrackColor = CoralPrimary.copy(alpha = 0.5f)
                                )
                            )

                            IconButton(onClick = {
                                expandedPackage = if (isExpanded) null else item.packageName
                            }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand"
                                )
                            }
                        }
                    }

                    // Expandable Per-App Limit Sliders
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Screen Time Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Screen Time: $currentAppLimitMins mins ${if (hasCustomMinutes) "(Custom Override)" else "(Global Default)"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (hasCustomMinutes) {
                                TextButton(
                                    onClick = { app.prefsRepository.removeCustomAppLimitMinutes(item.packageName) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Reset to Default", fontSize = 12.sp, color = CoralPrimary)
                                }
                            }
                        }
                        Slider(
                            value = currentAppLimitMins.toFloat(),
                            onValueChange = {
                                app.prefsRepository.setAppDailyLimitMinutes(item.packageName, it.toInt())
                            },
                            valueRange = 5f..180f,
                            steps = 34,
                            colors = SliderDefaults.colors(thumbColor = CoralPrimary, activeTrackColor = CoralPrimary)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Reel Limit Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Short Video Quota: $currentAppLimitReels reels ${if (hasCustomReels) "(Custom Override)" else "(Global Default)"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (hasCustomReels) {
                                TextButton(
                                    onClick = { app.prefsRepository.removeCustomAppReelLimit(item.packageName) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Reset to Default", fontSize = 12.sp, color = CoralPrimary)
                                }
                            }
                        }
                        Slider(
                            value = currentAppLimitReels.toFloat(),
                            onValueChange = {
                                app.prefsRepository.setAppDailyReelLimit(item.packageName, it.toInt())
                            },
                            valueRange = 5f..150f,
                            steps = 28,
                            colors = SliderDefaults.colors(thumbColor = CoralPrimary, activeTrackColor = CoralPrimary)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
