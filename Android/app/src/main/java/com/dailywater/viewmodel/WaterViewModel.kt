package com.dailywater.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailywater.model.DailyRecord
import com.dailywater.model.IntakeEntry
import com.dailywater.storage.WaterStorage
import com.dailywater.util.DateKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

/**
 * MVVM ViewModel that owns all water-tracking state.
 * Uses StateFlow so Compose recomposes on changes.
 */
class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = WaterStorage(application)

    private val _dailyGoal = MutableStateFlow(0.0)
    val dailyGoal: StateFlow<Double> = _dailyGoal.asStateFlow()

    private val _unit = MutableStateFlow("ml")
    val unit: StateFlow<String> = _unit.asStateFlow()

    private val _todayIntake = MutableStateFlow(0.0)
    val todayIntake: StateFlow<Double> = _todayIntake.asStateFlow()

    private val _intakeEntries = MutableStateFlow<List<IntakeEntry>>(emptyList())
    val intakeEntries: StateFlow<List<IntakeEntry>> = _intakeEntries.asStateFlow()

    private val _history = MutableStateFlow<List<DailyRecord>>(emptyList())
    val history: StateFlow<List<DailyRecord>> = _history.asStateFlow()

    private val _bottleSize = MutableStateFlow(0.0)
    val bottleSize: StateFlow<Double> = _bottleSize.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isDarkModeSet = MutableStateFlow(false)
    val isDarkModeSet: StateFlow<Boolean> = _isDarkModeSet.asStateFlow()

    private val _appLanguage = MutableStateFlow("device")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            _dailyGoal.value = storage.getDailyGoal()
            _unit.value = storage.getUnit()
            _todayIntake.value = storage.getTodayIntake()
            _intakeEntries.value = storage.getEntries()
            _history.value = storage.getHistory()
            _bottleSize.value = storage.getBottleSize()
            _isDarkMode.value = storage.getIsDarkMode()
            _isDarkModeSet.value = storage.getIsDarkModeSet()
            _appLanguage.value = storage.getAppLanguage()
            checkForNewDayAndResetIfNeeded()
        }
    }

    /**
     * Daily reset logic:
     * Compares the stored dateKey (yyyy-MM-dd in LOCAL timezone) to today's
     * local date key. If they differ the calendar day has changed, so we
     * reset todayIntake and intakeEntries and store the new dateKey.
     * Called on app launch and every time the app resumes to foreground.
     */
    fun checkForNewDayAndResetIfNeeded() {
        viewModelScope.launch {
            val todayKey = DateKey.today()
            val storedKey = storage.getDateKey()
            if (storedKey != todayKey) {
                // Archive the previous day's data before resetting,
                // but only if there was actual intake to record.
                if (storedKey.isNotEmpty() && _todayIntake.value > 0) {
                    archiveDay(storedKey, _todayIntake.value, _dailyGoal.value, _unit.value)
                }
                storage.resetDay(todayKey)
                _todayIntake.value = 0.0
                _intakeEntries.value = emptyList()
            }
        }
    }

    /** Add an intake amount. Appends an entry and increases todayIntake. */
    fun addIntake(amount: Double) {
        viewModelScope.launch {
            val entry = IntakeEntry(amount = amount)
            val newEntries = _intakeEntries.value + entry
            val newIntake = _todayIntake.value + amount
            _intakeEntries.value = newEntries
            _todayIntake.value = newIntake
            storage.setEntries(newEntries)
            storage.setTodayIntake(newIntake)
        }
    }

    /** Undo the most recent intake entry. Subtracts its amount (floor at 0). */
    fun undoLastAdd() {
        viewModelScope.launch {
            val entries = _intakeEntries.value
            if (entries.isEmpty()) return@launch
            val last = entries.last()
            val newEntries = entries.dropLast(1)
            val newIntake = maxOf(_todayIntake.value - last.amount, 0.0)
            _intakeEntries.value = newEntries
            _todayIntake.value = newIntake
            storage.setEntries(newEntries)
            storage.setTodayIntake(newIntake)
        }
    }

    /** Manual reset for today (caller should confirm first). */
    fun resetToday() {
        viewModelScope.launch {
            val todayKey = DateKey.today()
            storage.resetDay(todayKey)
            _todayIntake.value = 0.0
            _intakeEntries.value = emptyList()
        }
    }

    /** Update the daily goal value. */
    fun setGoal(goal: Double) {
        viewModelScope.launch {
            val clamped = maxOf(goal, 0.0)
            _dailyGoal.value = clamped
            storage.setDailyGoal(clamped)
        }
    }

    /** Update the unit preference. */
    fun setUnit(newUnit: String) {
        viewModelScope.launch {
            _unit.value = newUnit
            storage.setUnit(newUnit)
        }
    }

    // MARK: - History

    /** Archive a completed day into the history list. Replaces any existing record for the same dateKey. */
    private suspend fun archiveDay(dateKey: String, intake: Double, goal: Double, unit: String) {
        val record = DailyRecord(dateKey = dateKey, totalIntake = intake, goal = goal, unit = unit)
        val h = _history.value.toMutableList()
        h.removeAll { it.dateKey == dateKey }
        h.add(record)
        h.sortByDescending { it.dateKey } // newest first
        _history.value = h
        storage.setHistory(h)
    }

    /** Archive today manually (snapshot). */
    fun archiveToday() {
        viewModelScope.launch {
            val todayKey = DateKey.today()
            if (_todayIntake.value > 0) {
                archiveDay(todayKey, _todayIntake.value, _dailyGoal.value, _unit.value)
            }
        }
    }

    // MARK: - Bottle Mode

    val isBottleMode: Boolean get() = _unit.value == "bottle"

    val displayUnit: String get() = if (_unit.value == "bottle") "bottles" else _unit.value

    fun getQuickAddAmounts(): List<Double> {
        val u = _unit.value
        val bs = _bottleSize.value
        if (u == "bottle" && bs > 0) {
            return listOf(bs * 0.25, bs * 0.5, bs * 0.75, bs)
        }
        return if (u == "ml") listOf(100.0, 200.0, 300.0, 500.0)
        else listOf(4.0, 8.0, 12.0, 16.0)
    }

    fun setBottleSize(size: Double) {
        viewModelScope.launch {
            val clamped = maxOf(size, 0.0)
            _bottleSize.value = clamped
            storage.setBottleSize(clamped)
        }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            _isDarkMode.value = dark
            _isDarkModeSet.value = true
            storage.setIsDarkMode(dark)
        }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            _appLanguage.value = lang
            storage.setAppLanguage(lang)
        }
    }

    // MARK: - Streak

    fun currentStreak(): Int {
        var streak = 0
        val todayKey = DateKey.today()
        // Check if today counts
        if (_dailyGoal.value > 0 && _todayIntake.value >= _dailyGoal.value) {
            streak++
        }
        // Walk backwards through history
        val sorted = _history.value.sortedByDescending { it.dateKey }
        var expectedDate = LocalDate.now().minusDays(1)
        for (record in sorted) {
            val expectedKey = expectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            if (record.dateKey == todayKey) continue
            if (record.dateKey == expectedKey && record.goalMet) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (record.dateKey < expectedKey) {
                break
            }
        }
        return streak
    }

    // MARK: - Hydration Timing

    fun morningIntake(): Double = intakeByPeriod(5, 12)
    fun afternoonIntake(): Double = intakeByPeriod(12, 17)
    fun eveningIntake(): Double = intakeByPeriod(17, 29)

    private fun intakeByPeriod(startHour: Int, endHour: Int): Double {
        val cal = Calendar.getInstance()
        return _intakeEntries.value.sumOf { entry ->
            cal.timeInMillis = entry.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (endHour <= 24) {
                if (hour in startHour until endHour) entry.amount else 0.0
            } else {
                if (hour >= startHour || hour < (endHour - 24)) entry.amount else 0.0
            }
        }
    }

    // MARK: - Chart Data

    data class ChartPoint(val dateKey: String, val intake: Double, val goal: Double)

    fun chartData(lastDays: Int): List<ChartPoint> {
        val todayKey = DateKey.today()
        val historyMap = _history.value.associateBy { it.dateKey }
        val result = mutableListOf<ChartPoint>()
        val today = LocalDate.now()
        for (offset in -(lastDays - 1)..0) {
            val date = today.plusDays(offset.toLong())
            val key = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            if (key == todayKey) {
                result.add(ChartPoint(key, _todayIntake.value, _dailyGoal.value))
            } else {
                val record = historyMap[key]
                result.add(ChartPoint(key, record?.totalIntake ?: 0.0, record?.goal ?: _dailyGoal.value))
            }
        }
        return result
    }
}
