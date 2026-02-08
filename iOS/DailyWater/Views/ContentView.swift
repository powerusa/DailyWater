import SwiftUI

/// Home screen: progress ring, stats, quick-add buttons, undo, reset, settings.
struct ContentView: View {
    @EnvironmentObject var vm: WaterViewModel

    @State private var showSettings = false
    @State private var showCustomAdd = false
    @State private var showResetConfirmation = false
    @State private var showHistory = false
    @State private var showCharts = false
    @State private var animatedProgress: Double = 0

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemGroupedBackground).ignoresSafeArea()

                if vm.dailyGoal <= 0 {
                    emptyStateView
                } else {
                    mainContentView
                }
            }
            .navigationTitle(L10n.t("app_name", vm.appLanguage))
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button { showHistory = true } label: {
                        Image(systemName: "calendar")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
            .sheet(isPresented: $showHistory) {
                HistoryView()
            }
            .sheet(isPresented: $showCharts) {
                ChartsView()
            }
            .sheet(isPresented: $showCustomAdd) {
                CustomAddSheet()
            }
            .alert(L10n.t("reset_today", vm.appLanguage), isPresented: $showResetConfirmation) {
                Button(L10n.t("cancel", vm.appLanguage), role: .cancel) { }
                Button(L10n.t("reset", vm.appLanguage), role: .destructive) { vm.resetToday() }
            } message: {
                Text(L10n.t("reset_today_msg", vm.appLanguage))
            }
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "drop.fill")
                .font(.system(size: 64))
                .foregroundColor(.blue.opacity(0.4))
            Text(L10n.t("set_your_daily_goal", vm.appLanguage))
                .font(.title2.bold())
            Text(L10n.t("set_goal_prompt", vm.appLanguage))
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
                .padding(.horizontal, 40)
            Button {
                showSettings = true
            } label: {
                Label(L10n.t("set_goal", vm.appLanguage), systemImage: "gearshape")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: 200)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(14)
            }
        }
    }

    // MARK: - Main Content

    private var mainContentView: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Goal Reached Banner
                if vm.goalReached {
                    Text(L10n.t("goal_reached", vm.appLanguage))
                        .font(.title3.bold())
                        .foregroundColor(.green)
                        .padding(.top, 8)
                        .transition(.scale)
                }

                // Progress Ring
                progressRing
                    .padding(.top, 8)

                // Streak badge
                if vm.currentStreak > 0 {
                    HStack(spacing: 6) {
                        Image(systemName: "flame.fill")
                            .foregroundColor(.orange)
                        Text(L10n.dayStreak(vm.currentStreak, vm.appLanguage))
                            .font(.subheadline.bold())
                            .foregroundColor(.orange)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(20)
                }

                // Stats
                statsRow

                // Hydration Timing
                timingSection

                // Charts button
                Button {
                    showCharts = true
                } label: {
                    Label(L10n.t("weekly_monthly_charts", vm.appLanguage), systemImage: "chart.bar.fill")
                        .font(.subheadline.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.purple.opacity(0.1))
                        .foregroundColor(.purple)
                        .cornerRadius(12)
                }
                .padding(.horizontal)

                // Quick Add Buttons
                quickAddSection

                // Custom Add
                Button {
                    showCustomAdd = true
                } label: {
                    Label(L10n.t("custom_amount", vm.appLanguage), systemImage: "plus")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue.opacity(0.1))
                        .foregroundColor(.blue)
                        .cornerRadius(14)
                }
                .padding(.horizontal)

                // Undo & Reset
                HStack(spacing: 16) {
                    Button {
                        vm.undoLastAdd()
                    } label: {
                        Label(L10n.t("undo", vm.appLanguage), systemImage: "arrow.uturn.backward")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.orange.opacity(0.1))
                            .foregroundColor(.orange)
                            .cornerRadius(14)
                    }
                    .disabled(vm.intakeEntries.isEmpty)

                    Button {
                        showResetConfirmation = true
                    } label: {
                        Label(L10n.t("reset", vm.appLanguage), systemImage: "arrow.uturn.left")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .foregroundColor(.red)
                            .cornerRadius(14)
                    }
                }
                .padding(.horizontal)

                Spacer(minLength: 40)
            }
        }
        .onAppear { animatedProgress = vm.progress }
        .onChange(of: vm.progress) { newValue in
            withAnimation(.easeInOut(duration: 0.5)) {
                animatedProgress = newValue
            }
        }
    }

    // MARK: - Progress Ring

    private var progressRing: some View {
        ZStack {
            // Background track
            Circle()
                .stroke(Color.blue.opacity(0.15), style: StrokeStyle(lineWidth: 20, lineCap: .round))
                .frame(width: 200, height: 200)

            // Foreground arc
            Circle()
                .trim(from: 0, to: animatedProgress)
                .stroke(
                    vm.goalReached ? Color.green : Color.blue,
                    style: StrokeStyle(lineWidth: 20, lineCap: .round)
                )
                .frame(width: 200, height: 200)
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.5), value: animatedProgress)

            // Center label
            VStack(spacing: 4) {
                Image(systemName: vm.isBottleMode ? "waterbottle.fill" : "drop.fill")
                    .font(.title)
                    .foregroundColor(.blue)
                Text(formatAmount(vm.todayIntake))
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                Text(vm.displayUnit)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Stats

    private var statsRow: some View {
        HStack(spacing: 0) {
            statItem(title: L10n.t("today", vm.appLanguage), value: formatAmount(vm.todayIntake))
            Divider().frame(height: 40)
            statItem(title: L10n.t("goal", vm.appLanguage), value: formatAmount(vm.dailyGoal))
            Divider().frame(height: 40)
            statItem(title: L10n.t("remaining", vm.appLanguage), value: formatAmount(vm.remaining))
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .padding(.horizontal)
    }

    private func statItem(title: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.headline)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Hydration Timing

    private var timingSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.t("todays_timing", vm.appLanguage))
                .font(.subheadline.bold())
                .foregroundColor(.secondary)
                .padding(.horizontal)

            HStack(spacing: 12) {
                timingCard(icon: "sunrise.fill", label: L10n.t("morning", vm.appLanguage), amount: vm.morningIntake, color: .orange)
                timingCard(icon: "sun.max.fill", label: L10n.t("afternoon", vm.appLanguage), amount: vm.afternoonIntake, color: .yellow)
                timingCard(icon: "moon.fill", label: L10n.t("evening", vm.appLanguage), amount: vm.eveningIntake, color: .indigo)
            }
            .padding(.horizontal)
        }
    }

    private func timingCard(icon: String, label: String, amount: Double, color: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(formatAmount(amount))
                .font(.subheadline.bold())
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(color.opacity(0.08))
        .cornerRadius(12)
    }

    // MARK: - Quick Add

    private var quickAddSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.t("quick_add", vm.appLanguage))
                .font(.subheadline.bold())
                .foregroundColor(.secondary)
                .padding(.horizontal)

            HStack(spacing: 12) {
                ForEach(vm.quickAddAmounts, id: \.self) { amount in
                    Button {
                        vm.addIntake(amount)
                    } label: {
                        Text(vm.isBottleMode ? quickAddBottleLabel(amount) : "+\(formatAmount(amount))")
                            .font(.subheadline.bold())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                }
            }
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

    private func quickAddBottleLabel(_ amount: Double) -> String {
        guard vm.bottleSize > 0 else { return "+\(formatAmount(amount))" }
        let ratio = amount / vm.bottleSize
        if abs(ratio - 0.25) < 0.01 { return "+¼" }
        if abs(ratio - 0.5) < 0.01 { return "+½" }
        if abs(ratio - 0.75) < 0.01 { return "+¾" }
        if abs(ratio - 1.0) < 0.01 { return "+1🍶" }
        return "+\(formatAmount(amount))"
    }
}

#Preview {
    ContentView()
        .environmentObject(WaterViewModel())
}
