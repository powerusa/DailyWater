import Foundation

/// Persists water tracking data in UserDefaults using JSON encoding.
/// All dates use the device's local timezone so the dateKey (yyyy-MM-dd)
/// always reflects the user's calendar day.
final class WaterStorage {

    private let defaults = UserDefaults.standard

    // MARK: - Keys
    private enum Key {
        static let dailyGoal = "dailyGoal"
        static let unit = "unit"
        static let dateKey = "dateKey"
        static let todayIntake = "todayIntake"
        static let intakeEntries = "intakeEntries"
        static let history = "history"
        static let bottleSize = "bottleSize"
        static let isDarkMode = "isDarkMode"
        static let isDarkModeSet = "isDarkModeSet"
        static let appLanguage = "appLanguage"
    }

    // MARK: - Daily Goal

    var dailyGoal: Double {
        get { defaults.double(forKey: Key.dailyGoal) }
        set { defaults.set(newValue, forKey: Key.dailyGoal) }
    }

    // MARK: - Unit (ml / oz)

    var unit: String {
        get { defaults.string(forKey: Key.unit) ?? "ml" }
        set { defaults.set(newValue, forKey: Key.unit) }
    }

    // MARK: - Date Key (yyyy-MM-dd local)

    var dateKey: String {
        get { defaults.string(forKey: Key.dateKey) ?? "" }
        set { defaults.set(newValue, forKey: Key.dateKey) }
    }

    // MARK: - Today Intake

    var todayIntake: Double {
        get { defaults.double(forKey: Key.todayIntake) }
        set { defaults.set(newValue, forKey: Key.todayIntake) }
    }

    // MARK: - Intake Entries (JSON)

    var intakeEntries: [IntakeEntry] {
        get {
            guard let data = defaults.data(forKey: Key.intakeEntries) else { return [] }
            return (try? JSONDecoder().decode([IntakeEntry].self, from: data)) ?? []
        }
        set {
            let data = try? JSONEncoder().encode(newValue)
            defaults.set(data, forKey: Key.intakeEntries)
        }
    }

    // MARK: - Dark Mode

    /// Whether the user has explicitly set a dark mode preference.
    var isDarkModeSet: Bool {
        get { defaults.bool(forKey: Key.isDarkModeSet) }
        set { defaults.set(newValue, forKey: Key.isDarkModeSet) }
    }

    var isDarkMode: Bool {
        get { defaults.bool(forKey: Key.isDarkMode) }
        set {
            defaults.set(newValue, forKey: Key.isDarkMode)
            isDarkModeSet = true
        }
    }

    // MARK: - App Language

    var appLanguage: String {
        get { defaults.string(forKey: Key.appLanguage) ?? "device" }
        set { defaults.set(newValue, forKey: Key.appLanguage) }
    }

    // MARK: - Bottle Size (amount per bottle in the current unit)

    var bottleSize: Double {
        get { defaults.double(forKey: Key.bottleSize) }
        set { defaults.set(newValue, forKey: Key.bottleSize) }
    }

    // MARK: - History (array of DailyRecord archived per day)

    var history: [DailyRecord] {
        get {
            guard let data = defaults.data(forKey: Key.history) else { return [] }
            return (try? JSONDecoder().decode([DailyRecord].self, from: data)) ?? []
        }
        set {
            let data = try? JSONEncoder().encode(newValue)
            defaults.set(data, forKey: Key.history)
        }
    }

    // MARK: - Helpers

    /// Returns today's date key in yyyy-MM-dd using the current calendar & local timezone.
    static func todayDateKey() -> String {
        dateKeyFor(date: Date())
    }

    /// Returns a date key for any given date.
    static func dateKeyFor(date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.calendar = Calendar.current
        formatter.timeZone = TimeZone.current
        return formatter.string(from: date)
    }
}
