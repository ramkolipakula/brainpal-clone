package com.brainrot.rebuilt.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.domain.MascotStageCalculator
import com.brainrot.rebuilt.domain.model.AppUsageInfo
import com.brainrot.rebuilt.domain.model.MascotStage
import com.brainrot.rebuilt.ui.components.MascotView
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess
import com.brainrot.rebuilt.ui.theme.OrangeWarning

@Composable
fun HomeScreen(
    onNavigateToLimits: () -> Unit
) {
    val app = BrainPalApp.instance
    val lifecycleOwner = LocalLifecycleOwner.current

    // Phase 3: Single source of truth via reactive StateFlows
    val todayReels by app.prefsRepository.todayReelCountFlow.collectAsState()
    val limitReels by app.prefsRepository.dailyReelLimitFlow.collectAsState()
    val limitMinutes by app.prefsRepository.dailyLimitMinutesFlow.collectAsState()
    val isBubbleEnabled by app.prefsRepository.floatingBubbleEnabledFlow.collectAsState()
    val isStrictMode by app.prefsRepository.strictModeFlow.collectAsState()
    val snoozeUntil by app.prefsRepository.snoozeUntilFlow.collectAsState()
    val streakDays by app.prefsRepository.streakDaysFlow.collectAsState()
    val blockedPackages by app.prefsRepository.blockedPackagesFlow.collectAsState()

    val isSnoozed = System.currentTimeMillis() < snoozeUntil

    var usageStats by remember { mutableStateOf(emptyList<AppUsageInfo>()) }
    var totalMinutesSpent by remember { mutableStateOf(0) }

    fun refreshUsageStats() {
        app.prefsRepository.checkAndPerformMidnightReset()
        usageStats = app.usageRepository.getTodayUsageStats(blockedPackages, app.prefsRepository)
        val totalMillis = app.usageRepository.getTotalTodayForegroundTimeMillis(blockedPackages)
        totalMinutesSpent = (totalMillis / 60000L).toInt()
    }

    LaunchedEffect(blockedPackages) {
        refreshUsageStats()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshUsageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Phase 2: Deterministic mascot aging stage calculation
    val mascotStage = MascotStageCalculator.calculateStage(todayReels, limitReels)
    val mascotRes = MascotStageCalculator.getDrawableResId(mascotStage)

    val percentage = if (limitMinutes > 0) (totalMinutesSpent.toFloat() / limitMinutes.toFloat()) else 0f

    val streakLabel = if (streakDays == 1) "1 Day" else "$streakDays Days"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BrainPal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoralPrimary
                    )
                    Text(
                        text = "$streakLabel Dopamine Detox Streak 🔥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                if (isSnoozed) {
                    AssistChip(
                        onClick = {
                            app.prefsRepository.setSnoozeMinutes(0)
                        },
                        label = { Text("Pause Active") },
                        leadingIcon = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = OrangeWarning) }
                    )
                }
            }
        }

        // Buddy Mascot Card with Aging Transition (Phase 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val (statusTitle, statusSubtitle, statusColor) = when (mascotStage) {
                            MascotStage.FRESH -> Triple("Brain is Thriving!", "Zero brainrot. Sharp and energetic!", GreenSuccess)
                            MascotStage.HAPPY -> Triple("Staying in Control", "Healthy balance maintained.", Color.White)
                            MascotStage.TIRED -> Triple("Slightly Fatigued", "Getting tired. Time to slow down.", OrangeWarning)
                            MascotStage.EXHAUSTED -> Triple("Approaching Limit!", "Wrap up scrolling soon.", OrangeWarning)
                            MascotStage.ROASTED -> Triple("Brain is Fried!", "Quota breached. Full focus lock engaged.", CoralPrimary)
                        }

                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    // Smooth crossfade transition between mascot aging stages
                    Crossfade(
                        targetState = mascotRes,
                        label = "MascotAgingCrossfade"
                    ) { resId ->
                        MascotView(
                            resId = resId,
                            contentDescription = "Brain Mascot (${mascotStage.description})",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        // Screen Time & Reels Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Minutes Spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Distracted Screen Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalMinutesSpent / $limitMinutes mins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalMinutesSpent > limitMinutes) CoralPrimary else GreenSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            percentage < 0.7f -> GreenSuccess
                            percentage < 1.0f -> OrangeWarning
                            else -> CoralPrimary
                        },
                        trackColor = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reel Count
                    val reelPct = if (limitReels > 0) (todayReels.toFloat() / limitReels.toFloat()).coerceIn(0f, 1f) else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Short Videos / Reels",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$todayReels / $limitReels reels",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (todayReels >= limitReels) CoralPrimary else Color.LightGray
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { reelPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (todayReels >= limitReels) CoralPrimary else GreenSuccess,
                        trackColor = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.Snooze,
                            label = if (isSnoozed) "Resume" else "15m Snooze",
                            onClick = {
                                if (isSnoozed) {
                                    app.prefsRepository.setSnoozeMinutes(0)
                                } else {
                                    app.prefsRepository.setSnoozeMinutes(15)
                                }
                            }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Security,
                            label = if (isStrictMode) "Strict: ON" else "Strict: OFF",
                            onClick = {
                                app.prefsRepository.setStrictMode(!isStrictMode)
                            }
                        )
                        QuickActionItem(
                            icon = Icons.Default.SmartDisplay,
                            label = if (isBubbleEnabled) "Bubble: ON" else "Bubble: OFF",
                            onClick = {
                                app.prefsRepository.setFloatingBubbleEnabled(!isBubbleEnabled)
                            }
                        )
                        QuickActionItem(
                            icon = Icons.Default.Tune,
                            label = "Limits",
                            onClick = onNavigateToLimits
                        )
                    }
                }
            }
        }

        // Monitored Apps Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distracted Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToLimits) {
                    Text("Manage Apps", color = CoralPrimary)
                }
            }
        }

        // Monitored Apps Items
        items(usageStats) { appInfo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val mins = (appInfo.usageTimeMillis / 60000L).toInt()
                        Text(
                            text = "$mins mins spent today",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    AssistChip(
                        onClick = onNavigateToLimits,
                        label = { Text("Guarded") }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(46.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CoralPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.LightGray,
            maxLines = 1
        )
    }
}
