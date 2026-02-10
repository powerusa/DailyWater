package com.watercamel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.watercamel.util.L10n
import com.watercamel.viewmodel.WaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WaterViewModel,
    onBack: () -> Unit
) {
    val currentGoal by viewModel.dailyGoal.collectAsState()
    val currentUnit by viewModel.unit.collectAsState()
    val currentBottleSize by viewModel.bottleSize.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var goalText by remember { mutableStateOf(if (currentGoal > 0) formatGoal(currentGoal) else "") }
    var selectedUnit by remember { mutableStateOf(currentUnit) }
    var bottleSizeText by remember { mutableStateOf(if (currentBottleSize > 0) formatGoal(currentBottleSize) else "24") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(L10n.t("settings", lang)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(L10n.t("daily_goal", lang), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text(L10n.t("enter_goal", lang)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(L10n.t("unit", lang), style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedUnit == "ml",
                    onClick = { selectedUnit = "ml" },
                    label = { Text("ml") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedUnit == "oz",
                    onClick = { selectedUnit = "oz" },
                    label = { Text("oz") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedUnit == "bottle",
                    onClick = { selectedUnit = "bottle" },
                    label = { Text(L10n.t("bottle", lang)) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedUnit == "bottle") {
                Text(L10n.t("bottle_setup", lang), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = bottleSizeText,
                    onValueChange = { bottleSizeText = it },
                    label = { Text(L10n.t("bottle_size_label", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    L10n.t("bottle_hint", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(L10n.t("appearance", lang), style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(L10n.t("dark_mode", lang))
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }

            Text(L10n.t("language", lang), style = MaterialTheme.typography.titleMedium)

            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    val currentName = L10n.supportedLanguages.firstOrNull { it.code == lang }?.name ?: lang
                    Text(currentName)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    L10n.supportedLanguages.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                viewModel.setAppLanguage(option.code)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val goal = goalText.toDoubleOrNull() ?: 0.0
                    viewModel.setGoal(goal)
                    if (selectedUnit == "bottle") {
                        val bSize = bottleSizeText.toDoubleOrNull() ?: 0.0
                        viewModel.setBottleSize(bSize)
                    }
                    viewModel.setUnit(selectedUnit)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(L10n.t("save", lang), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

private fun formatGoal(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
