package com.watercamel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watercamel.util.L10n
import com.watercamel.viewmodel.WaterViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    viewModel: WaterViewModel,
    onBack: () -> Unit
) {
    val lang by viewModel.appLanguage.collectAsState()
    var selectedRange by remember { mutableIntStateOf(7) }
    val rangeOptions = listOf(7, 14, 30)
    val rangeLabelKeys = listOf("seven_days", "fourteen_days", "thirty_days")

    val data = remember(selectedRange, viewModel.todayIntake.collectAsState().value, viewModel.history.collectAsState().value) {
        viewModel.chartData(selectedRange)
    }

    val avgIntake = if (data.isNotEmpty()) data.sumOf { it.intake } / data.size else 0.0
    val daysGoalMet = data.count { it.goal > 0 && it.intake >= it.goal }
    val streak = viewModel.currentStreak()
    val maxIntake = max(data.maxOfOrNull { it.intake } ?: 0.0, data.firstOrNull()?.goal ?: 0.0).coerceAtLeast(1.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(L10n.t("trends", lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Range picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rangeOptions.forEachIndexed { index, days ->
                    FilterChip(
                        selected = selectedRange == days,
                        onClick = { selectedRange = days },
                        label = { Text(L10n.t(rangeLabelKeys[index], lang)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(L10n.t("average", lang), formatChartAmount(avgIntake), "\uD83D\uDCC8", Modifier.weight(1f))
                SummaryCard(L10n.t("goals_met", lang), "$daysGoalMet/$selectedRange", "✅", Modifier.weight(1f))
                SummaryCard(L10n.t("streak", lang), "${streak}d", "\uD83D\uDD25", Modifier.weight(1f))
            }

            // Bar chart
            Text(
                L10n.t("daily_intake", lang),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Canvas bar chart
            val barColor = MaterialTheme.colorScheme.primary
            val goalColor = Color(0xFFE53935)
            val goalMetColor = Color(0xFF4CAF50)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val barCount = data.size
                if (barCount == 0) return@Canvas
                val spacing = 2.dp.toPx()
                val totalSpacing = spacing * (barCount - 1)
                val barWidth = (size.width - totalSpacing) / barCount
                val chartHeight = size.height - 20.dp.toPx()

                // Goal line
                val goalY = if (data.isNotEmpty() && data.first().goal > 0) {
                    chartHeight * (1f - (data.first().goal / maxIntake).toFloat())
                } else null

                data.forEachIndexed { index, point ->
                    val barHeight = (point.intake / maxIntake * chartHeight).toFloat().coerceAtLeast(2f)
                    val x = index * (barWidth + spacing)
                    val isGoalMet = point.goal > 0 && point.intake >= point.goal

                    drawRect(
                        color = if (isGoalMet) goalMetColor else barColor,
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }

                // Goal line
                if (goalY != null) {
                    drawLine(
                        color = goalColor.copy(alpha = 0.5f),
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Date labels row
            if (data.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val displayFormatter = if (selectedRange <= 7) DateTimeFormatter.ofPattern("EEE") else DateTimeFormatter.ofPattern("d")
                    val step = if (selectedRange > 14) 5 else 1
                    data.filterIndexed { index, _ -> index % step == 0 || index == data.lastIndex }.forEach { point ->
                        val date = LocalDate.parse(point.dateKey, formatter)
                        Text(
                            date.format(displayFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = if (selectedRange > 14) 8.sp else 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawRect(color = Color(0xFFE53935).copy(alpha = 0.5f))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(L10n.t("goal_line", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawRect(color = Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(L10n.t("goal_met", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatChartAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
