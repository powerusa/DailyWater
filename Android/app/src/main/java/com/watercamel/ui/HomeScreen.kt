package com.watercamel.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watercamel.util.L10n
import com.watercamel.viewmodel.WaterViewModel
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WaterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {}
) {
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val unit by viewModel.unit.collectAsState()
    val todayIntake by viewModel.todayIntake.collectAsState()
    val entries by viewModel.intakeEntries.collectAsState()
    val bottleSize by viewModel.bottleSize.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    val progress = if (dailyGoal > 0) min(todayIntake / dailyGoal, 1.0) else 0.0
    val remaining = if (dailyGoal > 0) maxOf(dailyGoal - todayIntake, 0.0) else 0.0
    val goalReached = dailyGoal > 0 && todayIntake >= dailyGoal
    val isBottle = unit == "bottle"
    val displayUnit = if (isBottle) L10n.t("bottles", lang) else unit

    val quickAddAmounts = viewModel.getQuickAddAmounts()
    val streak = viewModel.currentStreak()
    val morningAmt = viewModel.morningIntake()
    val afternoonAmt = viewModel.afternoonIntake()
    val eveningAmt = viewModel.eveningIntake()

    var showResetDialog by remember { mutableStateOf(false) }
    var showCustomAddDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(L10n.t("reset_today", lang)) },
            text = { Text(L10n.t("reset_today_msg", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToday()
                    showResetDialog = false
                }) {
                    Text(L10n.t("reset", lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(L10n.t("cancel", lang))
                }
            }
        )
    }

    if (showCustomAddDialog) {
        CustomAddDialog(
            unit = unit,
            onDismiss = { showCustomAddDialog = false },
            onAdd = { amount ->
                viewModel.addIntake(amount)
                showCustomAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(L10n.t("app_name", lang)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.DateRange, contentDescription = L10n.t("history", lang))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = L10n.t("settings", lang))
                    }
                }
            )
        }
    ) { padding ->
        if (dailyGoal <= 0) {
            // Empty state prompting to set goal
            EmptyState(onNavigateToSettings = onNavigateToSettings, lang = lang, modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Goal reached banner
                if (goalReached) {
                    Text(
                        text = L10n.t("goal_reached", lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Ring
                ProgressRing(
                    progress = progress.toFloat(),
                    goalReached = goalReached,
                    todayIntake = todayIntake,
                    unit = displayUnit
                )

                // Streak badge
                if (streak > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDD25", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                L10n.dayStreak(streak, lang),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Row
                StatsRow(
                    todayIntake = todayIntake,
                    dailyGoal = dailyGoal,
                    remaining = remaining,
                    lang = lang
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hydration Timing
                Text(
                    text = L10n.t("todays_timing", lang),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimingCard("☀\uFE0F", L10n.t("morning", lang), morningAmt, Color(0xFFFF9800), Modifier.weight(1f))
                    TimingCard("☀\uFE0F", L10n.t("afternoon", lang), afternoonAmt, Color(0xFFFFC107), Modifier.weight(1f))
                    TimingCard("\uD83C\uDF19", L10n.t("evening", lang), eveningAmt, Color(0xFF3F51B5), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Charts button
                OutlinedButton(
                    onClick = onNavigateToCharts,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9C27B0))
                ) {
                    Text(L10n.t("weekly_monthly_charts", lang), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Add section
                Text(
                    text = L10n.t("quick_add", lang),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAddAmounts.forEach { amount ->
                        Button(
                            onClick = { viewModel.addIntake(amount) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isBottle && bottleSize > 0) bottleLabel(amount, bottleSize)
                                else "+${formatAmount(amount)}"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Add button
                OutlinedButton(
                    onClick = { showCustomAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.t("custom_amount", lang), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Undo & Reset row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastAdd() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = entries.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.t("undo", lang))
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L10n.t("reset", lang))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(onNavigateToSettings: () -> Unit, lang: String = "en", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            L10n.t("set_your_daily_goal", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            L10n.t("set_goal_prompt", lang),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToSettings,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.t("set_goal", lang))
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    goalReached: Boolean,
    todayIntake: Double,
    unit: String,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    val ringColor = if (goalReached) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 20.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = formatAmount(todayIntake),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(
    todayIntake: Double,
    dailyGoal: Double,
    remaining: Double,
    lang: String = "en"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(L10n.t("today", lang), formatAmount(todayIntake))
            StatItem(L10n.t("goal", lang), formatAmount(dailyGoal))
            StatItem(L10n.t("remaining", lang), formatAmount(remaining))
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimingCard(icon: String, label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatAmount(amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Format amount: drop decimal if it's a whole number. */
private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}

/** Bottle quick-add label: shows fraction symbols. */
private fun bottleLabel(amount: Double, bottleSize: Double): String {
    if (bottleSize <= 0) return "+${formatAmount(amount)}"
    val ratio = amount / bottleSize
    if (abs(ratio - 0.25) < 0.01) return "+¼"
    if (abs(ratio - 0.5) < 0.01) return "+½"
    if (abs(ratio - 0.75) < 0.01) return "+¾"
    if (abs(ratio - 1.0) < 0.01) return "+1\uD83C\uDF76"
    return "+${formatAmount(amount)}"
}
