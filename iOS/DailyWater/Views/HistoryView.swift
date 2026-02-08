import SwiftUI

/// Calendar-based history view showing past water intake days.
/// Each day cell shows a mini progress ring and intake amount.
struct HistoryView: View {
    @EnvironmentObject var vm: WaterViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var displayedMonth: Date = Date()

    private let calendar = Calendar.current
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
    private let weekdaySymbols = Calendar.current.shortWeekdaySymbols

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Streak banner
                    if vm.currentStreak > 0 {
                        HStack(spacing: 8) {
                            Image(systemName: "flame.fill")
                                .foregroundColor(.orange)
                                .font(.title2)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(L10n.dayStreak(vm.currentStreak, vm.appLanguage))
                                    .font(.headline)
                                    .foregroundColor(.orange)
                                Text(L10n.t("keep_it_going", vm.appLanguage))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                        }
                        .padding()
                        .background(Color.orange.opacity(0.08))
                        .cornerRadius(14)
                    }

                    // Month navigation
                    monthHeader

                    // Weekday labels
                    weekdayHeader

                    // Day grid
                    calendarGrid

                    // Recent history list
                    if !vm.history.isEmpty {
                        recentList
                    }

                    Spacer(minLength: 40)
                }
                .padding(.horizontal)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(L10n.t("history", vm.appLanguage))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(L10n.t("done", vm.appLanguage)) { dismiss() }
                }
            }
        }
    }

    // MARK: - Month Header

    private var monthHeader: some View {
        HStack {
            Button {
                shiftMonth(by: -1)
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title3.bold())
            }

            Spacer()

            Text(monthYearString(from: displayedMonth))
                .font(.title3.bold())

            Spacer()

            Button {
                shiftMonth(by: 1)
            } label: {
                Image(systemName: "chevron.right")
                    .font(.title3.bold())
            }
            .disabled(isCurrentMonth)
        }
        .padding(.top, 8)
    }

    // MARK: - Weekday Header

    private var weekdayHeader: some View {
        LazyVGrid(columns: columns, spacing: 4) {
            ForEach(weekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .font(.caption2.bold())
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - Calendar Grid

    private var calendarGrid: some View {
        let days = daysInMonth()
        return LazyVGrid(columns: columns, spacing: 6) {
            ForEach(days, id: \.self) { date in
                if let date = date {
                    dayCell(for: date)
                } else {
                    Color.clear
                        .frame(height: 56)
                }
            }
        }
    }

    private func dayCell(for date: Date) -> some View {
        let dateKey = dateKeyString(from: date)
        let todayKey = WaterStorage.todayDateKey()
        let isToday = dateKey == todayKey
        let record = recordFor(dateKey: dateKey)
        let isFuture = date > Date()

        // For today, use live data
        let intake: Double
        let goal: Double
        let progress: Double

        if isToday {
            intake = vm.todayIntake
            goal = vm.dailyGoal
            progress = vm.progress
        } else if let r = record {
            intake = r.totalIntake
            goal = r.goal
            progress = r.progress
        } else {
            intake = 0
            goal = 0
            progress = 0
        }

        let dayNumber = calendar.component(.day, from: date)

        return VStack(spacing: 2) {
            // Mini progress ring
            ZStack {
                Circle()
                    .stroke(Color.blue.opacity(0.1), lineWidth: 3)
                    .frame(width: 30, height: 30)

                if progress > 0 {
                    Circle()
                        .trim(from: 0, to: progress)
                        .stroke(
                            progress >= 1.0 ? Color.green : Color.blue,
                            style: StrokeStyle(lineWidth: 3, lineCap: .round)
                        )
                        .frame(width: 30, height: 30)
                        .rotationEffect(.degrees(-90))
                }

                Text("\(dayNumber)")
                    .font(.system(size: 11, weight: isToday ? .bold : .regular))
                    .foregroundColor(isFuture ? .secondary.opacity(0.4) : (isToday ? .blue : .primary))
            }

            // Intake label
            if intake > 0 {
                Text(shortAmount(intake))
                    .font(.system(size: 8))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            } else {
                Text(" ")
                    .font(.system(size: 8))
            }
        }
        .frame(height: 56)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(isToday ? Color.blue.opacity(0.08) : Color.clear)
        )
    }

    // MARK: - Recent History List

    private var recentList: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.t("recent_days", vm.appLanguage))
                .font(.headline)
                .padding(.top, 8)

            ForEach(vm.history.prefix(30)) { record in
                HStack {
                    // Date
                    VStack(alignment: .leading, spacing: 2) {
                        Text(formattedDate(record.dateKey))
                            .font(.subheadline.bold())
                        Text(record.unit)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    // Intake vs Goal
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("\(formatAmount(record.totalIntake)) / \(formatAmount(record.goal))")
                            .font(.subheadline)
                        HStack(spacing: 4) {
                            if record.goalMet {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                    .font(.caption)
                                Text(L10n.t("goal_met", vm.appLanguage))
                                    .font(.caption)
                                    .foregroundColor(.green)
                            } else {
                                Text("\(Int(record.progress * 100))%")
                                    .font(.caption)
                                    .foregroundColor(.orange)
                            }
                        }
                    }

                    // Mini progress bar
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.blue.opacity(0.1))
                            .frame(width: 40, height: 6)
                        RoundedRectangle(cornerRadius: 3)
                            .fill(record.goalMet ? Color.green : Color.blue)
                            .frame(width: 40 * record.progress, height: 6)
                    }
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(12)
            }
        }
    }

    // MARK: - Helpers

    private var isCurrentMonth: Bool {
        calendar.isDate(displayedMonth, equalTo: Date(), toGranularity: .month)
    }

    private func shiftMonth(by value: Int) {
        if let newDate = calendar.date(byAdding: .month, value: value, to: displayedMonth) {
            displayedMonth = newDate
        }
    }

    private func monthYearString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: date)
    }

    private func dateKeyString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.calendar = Calendar.current
        formatter.timeZone = TimeZone.current
        return formatter.string(from: date)
    }

    /// Returns array of optional Dates for the calendar grid.
    /// nil entries are blank leading cells before day 1.
    private func daysInMonth() -> [Date?] {
        guard let range = calendar.range(of: .day, in: .month, for: displayedMonth),
              let firstOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: displayedMonth))
        else { return [] }

        let firstWeekday = calendar.component(.weekday, from: firstOfMonth)
        let leadingBlanks = firstWeekday - calendar.firstWeekday
        let adjustedBlanks = (leadingBlanks + 7) % 7

        var days: [Date?] = Array(repeating: nil, count: adjustedBlanks)
        for day in range {
            if let date = calendar.date(byAdding: .day, value: day - 1, to: firstOfMonth) {
                days.append(date)
            }
        }
        return days
    }

    private func recordFor(dateKey: String) -> DailyRecord? {
        vm.history.first { $0.dateKey == dateKey }
    }

    private func formattedDate(_ dateKey: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        guard let date = formatter.date(from: dateKey) else { return dateKey }
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }

    private func formatAmount(_ value: Double) -> String {
        if value.truncatingRemainder(dividingBy: 1) == 0 {
            return String(format: "%.0f", value)
        }
        return String(format: "%.1f", value)
    }

    private func shortAmount(_ value: Double) -> String {
        if value >= 1000 {
            return String(format: "%.1fk", value / 1000)
        }
        return formatAmount(value)
    }
}

#Preview {
    HistoryView()
        .environmentObject(WaterViewModel())
}
