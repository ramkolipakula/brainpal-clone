package com.brainrot.rebuilt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess
import com.brainrot.rebuilt.ui.theme.OrangeWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = BrainPalApp.instance

    // Phase 3: Observe authoritative StateFlows directly
    val strictMode by app.prefsRepository.strictModeFlow.collectAsState()
    val passesLeft by app.prefsRepository.emergencyPassesFlow.collectAsState()
    val bubbleEnabled by app.prefsRepository.floatingBubbleEnabledFlow.collectAsState()
    val bubbleSize by app.prefsRepository.bubbleSizeFlow.collectAsState()
    val bedtimeEnabled by app.prefsRepository.bedtimeEnabledFlow.collectAsState()
    val streakDays by app.prefsRepository.streakDaysFlow.collectAsState()

    var currentLang by remember { mutableStateOf(app.prefsRepository.getRoastLanguage()) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val languages = remember { app.memeRepository.getAvailableLanguages() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences & System",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CoralPrimary
            )
            Text(
                text = "Configure intervention rules, focus hours, and roast humor",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // 1. Meme Language Setting
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLangDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = CoralPrimary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Roast Meme Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pool: ${currentLang.replaceFirstChar { it.uppercase() }} (${app.memeRepository.getPoolSize(currentLang)} memes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        // 2. Strict Mode Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CoralPrimary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Strict Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Disables emergency passes and friction bypasses once quota is reached",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = strictMode,
                        onCheckedChange = {
                            app.prefsRepository.setStrictMode(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CoralPrimary, checkedTrackColor = CoralPrimary.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // 3. Floating Reel Counter Settings (Phase 2 UI Reference)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = CoralPrimary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Floating Reel Counter Pill",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Live reel swipe counter pill over Instagram Reels & YouTube Shorts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Switch(
                            checked = bubbleEnabled,
                            onCheckedChange = {
                                app.prefsRepository.setFloatingBubbleEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CoralPrimary, checkedTrackColor = CoralPrimary.copy(alpha = 0.5f))
                        )
                    }

                    if (bubbleEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF2C2C2C))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Counter Pill Size", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                            Row {
                                FilterChip(
                                    selected = bubbleSize == "compact",
                                    onClick = {
                                        app.prefsRepository.setBubbleSize("compact")
                                    },
                                    label = { Text("Compact") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = bubbleSize == "large",
                                    onClick = {
                                        app.prefsRepository.setBubbleSize("large")
                                    },
                                    label = { Text("Large") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Bedtime / Night Focus Schedule
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = CoralPrimary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Night Focus & Bedtime Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Immediate intervention during sleep hours (10:00 PM – 6:00 AM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = bedtimeEnabled,
                        onCheckedChange = {
                            app.prefsRepository.setBedtimeModeEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CoralPrimary, checkedTrackColor = CoralPrimary.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // 5. Emergency Passes Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = GreenSuccess)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Emergency Passes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$passesLeft passes available today (5-min snooze each)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    TextButton(onClick = {
                        app.prefsRepository.resetDailyPasses()
                    }) {
                        Text("Replenish", color = CoralPrimary)
                    }
                }
            }
        }

        // 6. Streak & Data Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = OrangeWarning)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Detox Streak: Day $streakDays",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Reset habit streak if routine was broken",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    TextButton(onClick = { showResetConfirm = true }) {
                        Text("Reset", color = OrangeWarning)
                    }
                }
            }
        }

        // 7. About & Rebuild Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "About BrainPal Rebuilt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Native Android digital wellbeing reconstruction with zero commercial telemetry or payment gates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Built with Kotlin, Jetpack Compose, Material 3, and Localized Roast Humor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version: 1.0.2-phase10 (Release)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralPrimary
                    )
                }
            }
        }
    }

    // Language Selection Modal
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text("Select Roast Meme Language") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(languages.size) { idx ->
                        val lang = languages[idx]
                        val isSelected = lang == currentLang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentLang = lang
                                    app.prefsRepository.setRoastLanguage(lang)
                                    showLangDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.replaceFirstChar { it.uppercase() },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CoralPrimary else Color.White
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CoralPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLangDialog = false }) {
                    Text("Close", color = CoralPrimary)
                }
            }
        )
    }

    // Reset Streak Confirmation
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Detox Streak?") },
            text = { Text("Are you sure you want to reset your streak back to Day 1? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        app.prefsRepository.resetStreak()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                ) {
                    Text("Reset to Day 1")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}
