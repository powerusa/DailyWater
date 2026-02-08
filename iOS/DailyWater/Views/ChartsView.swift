import SwiftUI

/// Weekly and monthly hydration trend charts using native SwiftUI drawing.
struct ChartsView: View {
    @EnvironmentObject var vm: WaterViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var selectedRange: ChartRange = .week

    enum ChartRange: String, CaseIterable {
        case week = "seven_days"
        case twoWeeks = "fourteen_days"
        case month = "thirty_days"

        var days: Int {
            switch self {
            case .week: return 7
            case .twoWeeks: return 14
            case .month: return 30
            }
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Range picker
                    Picker("Range", selection: $selectedRange) {
                        ForEach(ChartRange.allCases, id: \.self) { range in
                            Text(L10n.t(range.rawValue, vm.appLanguage)).tag(range)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)

                    // Summary stats
                    summaryCards

                    // Bar chart
                    chartSection

                    Spacer(minLength: 40)
                }
                .padding(.top)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(L10n.t("trends", vm.appLanguage))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(L10n.t("done", vm.appLanguage)) { dismiss() }
                }
            }
        }
    }

    // MARK: - Data

    private var data: [(dateKey: String, intake: Double, goal: Double)] {
        vm.chartData(lastDays: selectedRange.days)
    }

    private var avgIntake: Double {
        let vals = data.map(\.intake)
        guard !vals.isEmpty else { return 0 }
        return vals.reduce(0, +) / Double(vals.count)
    }

    private var daysGoalMet: Int {
        data.filter { $0.goal > 0 && $0.intake >= $0.goal }.count
    }

    private var maxIntake: Double {
        max(data.map(\.intake).max() ?? 0, data.first?.goal ?? 0, 1)
    }

    // MARK: - Summary

    private var summaryCards: some View {
        HStack(spacing: 12) {
            summaryCard(title: L10n.t("average", vm.appLanguage), value: formatAmount(avgIntake), icon: "chart.line.uptrend.xyaxis", color: .blue)
            summaryCard(title: L10n.t("goals_met", vm.appLanguage), value: "\(daysGoalMet)/\(selectedRange.days)", icon: "checkmark.circle", color: .green)
            summaryCard(title: L10n.t("streak", vm.appLanguage), value: "\(vm.currentStreak)d", icon: "flame.fill", color: .orange)
        }
        .padding(.horizontal)
    }

    private func summaryCard(title: String, value: String, icon: String, color: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(value)
                .font(.headline)
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }

    // MARK: - Bar Chart

    private var chartSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.t("daily_intake", vm.appLanguage))
                .font(.subheadline.bold())
                .foregroundColor(.secondary)
                .padding(.horizontal)

            GeometryReader { geo in
                let barWidth = max((geo.size.width - CGFloat(data.count - 1) * 2 - 32) / CGFloat(data.count), 4)
                let chartHeight = geo.size.height - 30

                VStack(spacing: 0) {
                    // Bars
                    HStack(alignment: .bottom, spacing: 2) {
                        ForEach(Array(data.enumerated()), id: \.offset) { index, item in
                            VStack(spacing: 2) {
                                // Bar
                                let barHeight = maxIntake > 0 ? CGFloat(item.intake / maxIntake) * chartHeight : 0
                                let goalMet = item.goal > 0 && item.intake >= item.goal

                                RoundedRectangle(cornerRadius: 3)
                                    .fill(goalMet ? Color.green : Color.blue)
                                    .frame(width: barWidth, height: max(barHeight, 2))

                                // Goal line marker (thin line at goal level)
                                // Date label
                                Text(shortDateLabel(item.dateKey, totalDays: selectedRange.days))
                                    .font(.system(size: selectedRange.days > 14 ? 6 : 8))
                                    .foregroundColor(.secondary)
                                    .frame(width: barWidth + 2)
                                    .lineLimit(1)
                            }
                        }
                    }
                    .padding(.horizontal, 16)

                    // Goal line label
                    if let goalValue = data.first?.goal, goalValue > 0 {
                        HStack {
                            Rectangle()
                                .fill(Color.red.opacity(0.4))
                                .frame(height: 1)
                            Text("\(L10n.t("goal_colon", vm.appLanguage)) \(formatAmount(goalValue))")
                                .font(.system(size: 9))
                                .foregroundColor(.red.opacity(0.7))
                        }
                        .padding(.horizontal, 16)
                        .offset(y: -(CGFloat(goalValue / maxIntake) * chartHeight) - 14)
                    }
                }
            }
            .frame(height: 220)
            .padding(.horizontal)
        }
    }

    // MARK: - Helpers

    private func formatAmount(_ value: Double) -> String {
        if value.truncatingRemainder(dividingBy: 1) == 0 {
            return String(format: "%.0f", value)
        }
        return String(format: "%.1f", value)
    }

    private func shortDateLabel(_ dateKey: String, totalDays: Int) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        guard let date = formatter.date(from: dateKey) else { return "" }
        if totalDays <= 7 {
            formatter.dateFormat = "EEE"
        } else {
            formatter.dateFormat = "d"
        }
        return formatter.string(from: date)
    }
}

#Preview {
    ChartsView()
        .environmentObject(WaterViewModel())
}
