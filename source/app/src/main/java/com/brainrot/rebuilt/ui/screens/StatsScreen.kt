package com.brainrot.rebuilt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess

@Composable
fun StatsScreen() {
    val app = BrainPalApp.instance
    val lifecycleOwner = LocalLifecycleOwner.current

    var streakDays by remember { mutableStateOf(app.prefsRepository.getStreakDays()) }
    var todayReels by remember { mutableStateOf(app.prefsRepository.getTodayReelCount()) }
    var totalDistractedMins by remember { mutableStateOf(0) }
    var globalLimitMins by remember { mutableStateOf(app.prefsRepository.getDailyLimitMinutes()) }
    var hourlyData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    fun refreshStats() {
        val blocked = app.prefsRepository.getBlockedPackages()
        streakDays = app.prefsRepository.getStreakDays()
        todayReels = app.prefsRepository.getTodayReelCount()
        globalLimitMins = app.prefsRepository.getDailyLimitMinutes()
        val totalMillis = app.usageRepository.getTotalTodayForegroundTimeMillis(blocked)
        totalDistractedMins = (totalMillis / 60000L).toInt()
        // DEF-04 FIX: Dynamic hourly query from UsageStatsManager
        hourlyData = app.usageRepository.getHourlyUsageToday(blocked)
    }

    LaunchedEffect(Unit) {
        refreshStats()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val maxVal = (hourlyData.maxOfOrNull { it.second } ?: 30).coerceAtLeast(15).toFloat()
    val savedHours = String.format("%.1f", maxOf(0.5f, (globalLimitMins * 1.5f - totalDistractedMins).coerceAtLeast(0f) / 60f))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Detox Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CoralPrimary
            )
            Text(
                text = "Real-time attention recovery metrics from UsageStatsManager",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Streak & Impact Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GreenSuccess)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$streakDays Days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Clean Detox Streak", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = CoralPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$todayReels", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreenSuccess)
                        Text(text = "Short Videos Intercepted", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }

        // Hourly Usage Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Hourly Scroll Density",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${totalDistractedMins}m total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        hourlyData.forEach { (time, count) ->
                            val barHeightRatio = (count / maxVal).coerceIn(0.06f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(barHeightRatio)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (count > 20) CoralPrimary else GreenSuccess)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = time.replace(" ", ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Focus Recovery Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• You reclaimed ~$savedHours hours of attention today through active quota limits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Intercepted reels and shorts: $todayReels videos prevented from loop scrolling.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Total guarded apps active: ${app.prefsRepository.getBlockedPackages().size} apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
