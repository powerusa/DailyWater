package com.dailywater.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
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
import com.dailywater.model.DailyRecord
import com.dailywater.util.DateKey
import com.dailywater.util.L10n
import com.dailywater.viewmodel.WaterViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: WaterViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    val todayIntake by viewModel.todayIntake.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val unit by viewModel.unit.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val todayKey = DateKey.today()

    val historyMap = remember(history) {
        history.associateBy { it.dateKey }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(L10n.t("history", lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Streak banner
            val streak = viewModel.currentStreak()
            if (streak > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDD25", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    L10n.dayStreak(streak, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                                Text(
                                    L10n.t("keep_it_going", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Month navigation
            item {
                MonthHeader(
                    displayedMonth = displayedMonth,
                    isCurrentMonth = displayedMonth == YearMonth.now(),
                    onPrevious = { displayedMonth = displayedMonth.minusMonths(1) },
                    onNext = { displayedMonth = displayedMonth.plusMonths(1) }
                )
            }

            // Weekday labels
            item {
                WeekdayHeader()
            }

            // Calendar grid (rows of 7)
            val days = buildCalendarDays(displayedMonth)
            val rows = days.chunked(7)
            items(rows) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { date ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (date != null) {
                                val dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                val isToday = dateKey == todayKey
                                val isFuture = date.isAfter(today)

                                val intake: Double
                                val goal: Double
                                val progress: Double

                                if (isToday) {
                                    intake = todayIntake
                                    goal = dailyGoal
                                    progress = if (dailyGoal > 0) minOf(todayIntake / dailyGoal, 1.0) else 0.0
                                } else {
                                    val record = historyMap[dateKey]
                                    intake = record?.totalIntake ?: 0.0
                                    goal = record?.goal ?: 0.0
                                    progress = record?.progress ?: 0.0
                                }

                                DayCell(
                                    dayNumber = date.dayOfMonth,
                                    progress = progress.toFloat(),
                                    intake = intake,
                                    isToday = isToday,
                                    isFuture = isFuture
                                )
                            } else {
                                Spacer(modifier = Modifier.height(64.dp))
                            }
                        }
                    }
                }
            }

            // Spacer
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Recent history list
            if (history.isNotEmpty()) {
                item {
                    Text(
                        L10n.t("recent_days", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(history.take(30)) { record ->
                    RecentDayCard(record = record)
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    displayedMonth: YearMonth,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onNext, enabled = !isCurrentMonth) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekFields = WeekFields.of(Locale.getDefault())
    val firstDayOfWeek = weekFields.firstDayOfWeek
    val daysOfWeek = (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    progress: Float,
    intake: Double,
    isToday: Boolean,
    isFuture: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "dayProgress"
    )

    val ringColor = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        // Mini progress ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp)
        ) {
            Canvas(modifier = Modifier.size(30.dp)) {
                val strokeWidth = 3.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (animatedProgress > 0f) {
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
            }

            Text(
                text = "$dayNumber",
                fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        // Intake label
        if (intake > 0) {
            Text(
                text = shortAmount(intake),
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentDayCard(record: DailyRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateKey(record.dateKey),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = record.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Intake / Goal
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmount(record.totalIntake)} / ${formatAmount(record.goal)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.goalMet) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            L10n.t("goal_met", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text(
                            "${(record.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Mini progress bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = record.progress.toFloat())
                        .background(
                            color = if (record.goalMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

// MARK: - Helpers

/** Build the list of nullable LocalDate for the calendar grid.
 *  null = blank leading cell before day 1. */
private fun buildCalendarDays(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    val weekFields = WeekFields.of(Locale.getDefault())
    val firstDayOfWeek = weekFields.firstDayOfWeek

    val dayOfWeekValue = firstOfMonth.dayOfWeek.value
    val firstDayValue = firstDayOfWeek.value
    val leadingBlanks = (dayOfWeekValue - firstDayValue + 7) % 7

    val days = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { days.add(null) }
    for (day in 1..yearMonth.lengthOfMonth()) {
        days.add(yearMonth.atDay(day))
    }
    return days
}

private fun formatDateKey(dateKey: String): String {
    return try {
        val date = LocalDate.parse(dateKey)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) {
        dateKey
    }
}

private fun formatAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}

private fun shortAmount(value: Double): String {
    return if (value >= 1000) {
        String.format("%.1fk", value / 1000)
    } else {
        formatAmount(value)
    }
}
