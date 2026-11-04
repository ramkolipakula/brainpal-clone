package com.brainrot.rebuilt.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess
import com.brainrot.rebuilt.ui.theme.OrangeWarning
import java.util.Locale

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val app = BrainPalApp.instance
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe authoritative reactive StateFlows
    val streakDays by app.prefsRepository.streakDaysFlow.collectAsState()
    val todayReels by app.prefsRepository.todayReelCountFlow.collectAsState()
    val globalLimitMins by app.prefsRepository.dailyLimitMinutesFlow.collectAsState()
    val globalReelLimit by app.prefsRepository.dailyReelLimitFlow.collectAsState()
    val interventionsCount by app.prefsRepository.todayInterventionsFlow.collectAsState()
    val blockedPackages by app.prefsRepository.blockedPackagesFlow.collectAsState()

    var hasPermission by remember { mutableStateOf(app.usageRepository.hasUsageStatsPermission()) }
    var totalDistractedMins by remember { mutableStateOf(0) }
    var hourlyData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    fun refreshStats() {
        hasPermission = app.usageRepository.hasUsageStatsPermission()
        val totalMillis = app.usageRepository.getTotalTodayForegroundTimeMillis(blockedPackages)
        totalDistractedMins = (totalMillis / 60000L).toInt()
        hourlyData = app.usageRepository.getHourlyUsageToday(blockedPackages)
    }

    LaunchedEffect(blockedPackages) {
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

    val maxVal = (hourlyData.maxOfOrNull { it.second } ?: 10).coerceAtLeast(10).toFloat()
    val preservedMins = (globalLimitMins - totalDistractedMins).coerceAtLeast(0)
    val preservedHours = String.format(Locale.US, "%.1f", preservedMins / 60f)
    val streakText = if (streakDays == 1) "1 Day" else "$streakDays Days"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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

        // Missing Usage Access Banner
        if (!hasPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CoralPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usage Access Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grant usage access to calculate hourly screen time accurately.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }
        }

        // Streak & Monitored Cards
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
                        Text(
                            text = streakText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clean Detox Streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
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
                        Text(
                            text = "$todayReels",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (todayReels >= globalReelLimit) CoralPrimary else GreenSuccess
                        )
                        Text(
                            text = "Short Videos Monitored",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalDistractedMins > globalLimitMins) CoralPrimary else GreenSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!hasPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Usage permission not granted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else if (totalDistractedMins == 0 && hourlyData.all { it.second == 0 }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No guarded app screen time recorded today yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            hourlyData.forEach { (time, minutes) ->
                                val barHeightRatio = (minutes / maxVal).coerceIn(if (minutes > 0) 0.12f else 0.04f, 1f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    if (minutes > 0) {
                                        Text(
                                            text = "${minutes}m",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(barHeightRatio)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                when {
                                                    minutes == 0 -> Color(0xFF2C2C2C)
                                                    minutes > 20 -> CoralPrimary
                                                    else -> GreenSuccess
                                                }
                                            )
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
        }

        // Focus Recovery Insights Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CoralPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Focus Recovery Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    if (totalDistractedMins <= globalLimitMins) {
                        Text(
                            text = "• Preserved Screen Time: You kept ${preservedMins}m (~$preservedHours hrs) within your ${globalLimitMins}m daily budget (${totalDistractedMins}m used).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    } else {
                        val overMins = totalDistractedMins - globalLimitMins
                        Text(
                            text = "• Quota Exceeded: Screen time reached ${totalDistractedMins}m / ${globalLimitMins}m (+${overMins}m over budget). Active enforcement engaged.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OrangeWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Short Videos Monitored: $todayReels videos tracked across guarded feeds ($globalReelLimit reel daily target).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Interventions Triggered: $interventionsCount active blocker interventions executed to prevent mindless doomscrolling.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Guarded Applications: ${blockedPackages.size} apps actively guarded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
