import Foundation
import SwiftUI

/// MVVM ViewModel that owns all water-tracking state.
/// Publishes changes so SwiftUI views update automatically.
final class WaterViewModel: ObservableObject {

    // MARK: - Published State

    @Published var dailyGoal: Double {
        didSet { storage.dailyGoal = dailyGoal }
    }
    @Published var unit: String {
        didSet {
            storage.unit = unit
            // Re-set goal to a sensible default when switching units if goal is 0
        }
    }
    @Published var todayIntake: Double {
        didSet { storage.todayIntake = todayIntake }
    }
    @Published var intakeEntries: [IntakeEntry] {
        didSet { storage.intakeEntries = intakeEntries }
    }
    @Published var history: [DailyRecord] = []
    @Published var bottleSize: Double {
        didSet { storage.bottleSize = bottleSize }
    }
    @Published var isDarkMode: Bool {
        didSet { storage.isDarkMode = isDarkMode }
    }
    /// Whether the user has ever explicitly toggled dark mode.
    var isDarkModeSet: Bool { storage.isDarkModeSet }
    @Published var appLanguage: String {
        didSet { storage.appLanguage = appLanguage }
    }

    // MARK: - Computed

    /// Progress clamped to [0, 1]. Returns 0 when goal is 0 or unset.
    var progress: Double {
        guard dailyGoal > 0 else { return 0 }
        return min(todayIntake / dailyGoal, 1.0)
    }

    /// Remaining amount floored at 0.
    var remaining: Double {
        max(dailyGoal - todayIntake, 0)
    }

    /// Whether the user has reached or exceeded the daily goal.
    var goalReached: Bool {
        dailyGoal > 0 && todayIntake >= dailyGoal
    }

    /// Quick-add amounts adapt to the selected unit.
    /// In bottle mode, shows bottle fractions instead.
    var quickAddAmounts: [Double] {
        if isBottleMode, bottleSize > 0 {
            return [bottleSize * 0.25, bottleSize * 0.5, bottleSize * 0.75, bottleSize]
        }
        return unit == "ml" ? [100, 200, 300, 500] : [4, 8, 12, 16]
    }

    /// Whether bottle mode is active (unit == "bottle").
    var isBottleMode: Bool {
        unit == "bottle"
    }

    /// Display unit label: shows the underlying unit for bottles.
    var displayUnit: String {
        isBottleMode ? "bottles" : unit
    }

    /// Current streak: consecutive days (ending yesterday or today) where goal was met.
    var currentStreak: Int {
        var streak = 0
        let cal = Calendar.current
        let todayKey = WaterStorage.todayDateKey()
        // Check if today counts
        if dailyGoal > 0 && todayIntake >= dailyGoal {
            streak += 1
        }
        // Walk backwards through history
        let sortedHistory = history.sorted { $0.dateKey > $1.dateKey }
        var expectedDate = cal.date(byAdding: .day, value: -1, to: Date())!
        for record in sortedHistory {
            let expectedKey = WaterStorage.dateKeyFor(date: expectedDate)
            if record.dateKey == todayKey { continue }
            if record.dateKey == expectedKey && record.goalMet {
                streak += 1
                expectedDate = cal.date(byAdding: .day, value: -1, to: expectedDate)!
            } else if record.dateKey < expectedKey {
                break
            }
        }
        return streak
    }

    // MARK: - Hydration Timing

    /// Breakdown of today's intake by time of day.
    var morningIntake: Double {
        intakeByPeriod(startHour: 5, endHour: 12)
    }
    var afternoonIntake: Double {
        intakeByPeriod(startHour: 12, endHour: 17)
    }
    var eveningIntake: Double {
        intakeByPeriod(startHour: 17, endHour: 29) // 17-05 next day (wraps)
    }

    private func intakeByPeriod(startHour: Int, endHour: Int) -> Double {
        let cal = Calendar.current
        return intakeEntries.reduce(0.0) { sum, entry in
            let hour = cal.component(.hour, from: entry.timestamp)
            if endHour <= 24 {
                if hour >= startHour && hour < endHour { return sum + entry.amount }
            } else {
                // Wraps past midnight: evening = 17..24 + 0..5
                if hour >= startHour || hour < (endHour - 24) { return sum + entry.amount }
            }
            return sum
        }
    }

    // MARK: - Private

    private let storage = WaterStorage()

    // MARK: - Init

    init() {
        self.dailyGoal = storage.dailyGoal
        self.unit = storage.unit
        self.bottleSize = storage.bottleSize
        self.isDarkMode = storage.isDarkMode
        self.appLanguage = storage.appLanguage
        self.todayIntake = storage.todayIntake
        self.intakeEntries = storage.intakeEntries
        self.history = storage.history

        // Immediately check on launch
        checkForNewDayAndResetIfNeeded()
    }

    // MARK: - Daily Reset Logic

    /// Compares the stored dateKey (yyyy-MM-dd in LOCAL timezone) to today's
    /// local date key. If they differ the calendar day has changed, so we
    /// reset todayIntake and intakeEntries and store the new dateKey.
    func checkForNewDayAndResetIfNeeded() {
        let todayKey = WaterStorage.todayDateKey()
        let storedKey = storage.dateKey
        if storedKey != todayKey {
            // Archive the previous day's data before resetting,
            // but only if there was actual intake to record.
            if !storedKey.isEmpty && todayIntake > 0 {
                archiveDay(dateKey: storedKey, intake: todayIntake, goal: dailyGoal, unit: unit)
            }
            todayIntake = 0
            intakeEntries = []
            storage.dateKey = todayKey
        }
    }

    // MARK: - Actions

    /// Add an intake amount. Appends an entry and increases todayIntake.
    func addIntake(_ amount: Double) {
        let entry = IntakeEntry(amount: amount)
        intakeEntries.append(entry)
        todayIntake += amount

        // Haptic feedback
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
    }

    /// Undo the most recent intake entry. Subtracts its amount (floor at 0).
    func undoLastAdd() {
        guard let last = intakeEntries.last else { return }
        intakeEntries.removeLast()
        todayIntake = max(todayIntake - last.amount, 0)
    }

    /// Manual reset for today (caller should confirm first).
    func resetToday() {
        todayIntake = 0
        intakeEntries = []
        storage.dateKey = WaterStorage.todayDateKey()
    }

    // MARK: - History

    /// Archive a completed day into the history list.
    /// Replaces any existing record for the same dateKey.
    private func archiveDay(dateKey: String, intake: Double, goal: Double, unit: String) {
        let record = DailyRecord(dateKey: dateKey, totalIntake: intake, goal: goal, unit: unit)
        var h = history
        h.removeAll { $0.dateKey == dateKey }
        h.append(record)
        h.sort { $0.dateKey > $1.dateKey } // newest first
        history = h
        storage.history = h
    }

    /// Also archive today manually (e.g. when the user wants a snapshot saved now).
    func archiveToday() {
        let todayKey = WaterStorage.todayDateKey()
        if todayIntake > 0 {
            archiveDay(dateKey: todayKey, intake: todayIntake, goal: dailyGoal, unit: unit)
        }
    }

    /// Update the daily goal value.
    func setGoal(_ goal: Double) {
        dailyGoal = max(goal, 0)
    }

    /// Update the unit preference.
    func setUnit(_ newUnit: String) {
        unit = newUnit
    }

    /// Update the bottle size.
    func setBottleSize(_ size: Double) {
        bottleSize = max(size, 0)
    }

    // MARK: - Chart Data

    /// Returns daily totals for the last N days (oldest first), for chart rendering.
    func chartData(lastDays: Int) -> [(dateKey: String, intake: Double, goal: Double)] {
        let cal = Calendar.current
        let todayKey = WaterStorage.todayDateKey()
        let historyMap = Dictionary(uniqueKeysWithValues: history.map { ($0.dateKey, $0) })
        var result: [(String, Double, Double)] = []
        for offset in stride(from: -(lastDays - 1), through: 0, by: 1) {
            let date = cal.date(byAdding: .day, value: offset, to: Date())!
            let key = WaterStorage.dateKeyFor(date: date)
            if key == todayKey {
                result.append((key, todayIntake, dailyGoal))
            } else if let record = historyMap[key] {
                result.append((key, record.totalIntake, record.goal))
            } else {
                result.append((key, 0, dailyGoal))
            }
        }
        return result
    }
}
