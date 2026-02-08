import Foundation

/// Archived summary for one completed day of water tracking.
struct DailyRecord: Codable, Identifiable, Equatable {
    var id: String { dateKey }
    let dateKey: String      // yyyy-MM-dd
    let totalIntake: Double
    let goal: Double
    let unit: String

    /// Progress ratio clamped to [0, 1].
    var progress: Double {
        guard goal > 0 else { return 0 }
        return min(totalIntake / goal, 1.0)
    }

    /// Whether the goal was met on this day.
    var goalMet: Bool {
        goal > 0 && totalIntake >= goal
    }

    /// Parse dateKey back into a Date (start of day, local TZ).
    var date: Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.calendar = Calendar.current
        formatter.timeZone = TimeZone.current
        return formatter.date(from: dateKey)
    }
}
