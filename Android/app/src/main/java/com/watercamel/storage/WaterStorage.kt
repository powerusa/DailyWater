package com.watercamel.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watercamel.model.DailyRecord
import com.watercamel.model.IntakeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * DataStore Preferences wrapper for persisting water tracking data.
 * All date handling uses the device's local timezone via [com.watercamel.util.DateKey].
 */
class WaterStorage(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_water")

        private val KEY_DAILY_GOAL = doublePreferencesKey("daily_goal")
        private val KEY_UNIT = stringPreferencesKey("unit")
        private val KEY_DATE_KEY = stringPreferencesKey("date_key")
        private val KEY_TODAY_INTAKE = doublePreferencesKey("today_intake")
        private val KEY_ENTRIES_JSON = stringPreferencesKey("entries_json")
        private val KEY_HISTORY_JSON = stringPreferencesKey("history_json")
        private val KEY_BOTTLE_SIZE = doublePreferencesKey("bottle_size")
        private val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val KEY_IS_DARK_MODE_SET = booleanPreferencesKey("is_dark_mode_set")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    private val dataStore get() = context.dataStore

    // MARK: - Read flows

    val dailyGoalFlow: Flow<Double> = dataStore.data.map { it[KEY_DAILY_GOAL] ?: 0.0 }
    val unitFlow: Flow<String> = dataStore.data.map { it[KEY_UNIT] ?: "ml" }
    val dateKeyFlow: Flow<String> = dataStore.data.map { it[KEY_DATE_KEY] ?: "" }
    val todayIntakeFlow: Flow<Double> = dataStore.data.map { it[KEY_TODAY_INTAKE] ?: 0.0 }
    val entriesFlow: Flow<List<IntakeEntry>> = dataStore.data.map { prefs ->
        parseEntries(prefs[KEY_ENTRIES_JSON] ?: "[]")
    }

    // MARK: - Snapshot reads (suspend)

    suspend fun getDateKey(): String = dataStore.data.first()[KEY_DATE_KEY] ?: ""
    suspend fun getDailyGoal(): Double = dataStore.data.first()[KEY_DAILY_GOAL] ?: 0.0
    suspend fun getUnit(): String = dataStore.data.first()[KEY_UNIT] ?: "ml"
    suspend fun getTodayIntake(): Double = dataStore.data.first()[KEY_TODAY_INTAKE] ?: 0.0
    suspend fun getEntries(): List<IntakeEntry> = parseEntries(
        dataStore.data.first()[KEY_ENTRIES_JSON] ?: "[]"
    )

    // MARK: - Writes

    suspend fun setDailyGoal(goal: Double) {
        dataStore.edit { it[KEY_DAILY_GOAL] = goal }
    }

    suspend fun setUnit(unit: String) {
        dataStore.edit { it[KEY_UNIT] = unit }
    }

    suspend fun setDateKey(dateKey: String) {
        dataStore.edit { it[KEY_DATE_KEY] = dateKey }
    }

    suspend fun setTodayIntake(intake: Double) {
        dataStore.edit { it[KEY_TODAY_INTAKE] = intake }
    }

    suspend fun setEntries(entries: List<IntakeEntry>) {
        dataStore.edit { it[KEY_ENTRIES_JSON] = entriesToJson(entries) }
    }

    /** Bulk reset: sets intake to 0, clears entries, and updates dateKey. */
    suspend fun resetDay(newDateKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TODAY_INTAKE] = 0.0
            prefs[KEY_ENTRIES_JSON] = "[]"
            prefs[KEY_DATE_KEY] = newDateKey
        }
    }

    // MARK: - Dark Mode

    suspend fun getIsDarkModeSet(): Boolean = dataStore.data.first()[KEY_IS_DARK_MODE_SET] ?: false

    suspend fun getIsDarkMode(): Boolean = dataStore.data.first()[KEY_IS_DARK_MODE] ?: false

    suspend fun setIsDarkMode(dark: Boolean) {
        dataStore.edit {
            it[KEY_IS_DARK_MODE] = dark
            it[KEY_IS_DARK_MODE_SET] = true
        }
    }

    // MARK: - App Language

    suspend fun getAppLanguage(): String = dataStore.data.first()[KEY_APP_LANGUAGE] ?: "device"

    suspend fun setAppLanguage(lang: String) {
        dataStore.edit { it[KEY_APP_LANGUAGE] = lang }
    }

    // MARK: - Bottle Size

    suspend fun getBottleSize(): Double = dataStore.data.first()[KEY_BOTTLE_SIZE] ?: 0.0

    suspend fun setBottleSize(size: Double) {
        dataStore.edit { it[KEY_BOTTLE_SIZE] = size }
    }

    // MARK: - History

    suspend fun getHistory(): List<DailyRecord> = parseHistory(
        dataStore.data.first()[KEY_HISTORY_JSON] ?: "[]"
    )

    suspend fun setHistory(records: List<DailyRecord>) {
        dataStore.edit { it[KEY_HISTORY_JSON] = historyToJson(records) }
    }

    // MARK: - JSON helpers

    /** Safely parse entries JSON; falls back to empty list on any error. */
    private fun parseEntries(json: String): List<IntakeEntry> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                IntakeEntry(
                    id = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    amount = obj.getDouble("amount")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun entriesToJson(entries: List<IntakeEntry>): String {
        val arr = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("timestamp", entry.timestamp)
                put("amount", entry.amount)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    /** Safely parse history JSON; falls back to empty list on any error. */
    private fun parseHistory(json: String): List<DailyRecord> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                DailyRecord(
                    dateKey = obj.getString("dateKey"),
                    totalIntake = obj.getDouble("totalIntake"),
                    goal = obj.getDouble("goal"),
                    unit = obj.getString("unit")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun historyToJson(records: List<DailyRecord>): String {
        val arr = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("dateKey", record.dateKey)
                put("totalIntake", record.totalIntake)
                put("goal", record.goal)
                put("unit", record.unit)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
