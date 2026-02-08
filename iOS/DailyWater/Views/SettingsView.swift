import SwiftUI

/// Settings sheet: set daily goal, unit preference, and bottle mode.
struct SettingsView: View {
    @EnvironmentObject var vm: WaterViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var goalText: String = ""
    @State private var selectedUnit: String = "ml"
    @State private var bottleSizeText: String = ""
    @State private var bottleBaseUnit: String = "oz"

    var body: some View {
        NavigationStack {
            Form {
                Section(L10n.t("daily_goal", vm.appLanguage)) {
                    TextField(L10n.t("enter_goal", vm.appLanguage), text: $goalText)
                        .keyboardType(.decimalPad)
                }

                Section(L10n.t("unit", vm.appLanguage)) {
                    Picker(L10n.t("unit", vm.appLanguage), selection: $selectedUnit) {
                        Text("ml").tag("ml")
                        Text("oz").tag("oz")
                        Text("🍶 Bottle").tag("bottle")
                    }
                    .pickerStyle(.segmented)
                }

                if selectedUnit == "bottle" {
                    Section(L10n.t("bottle_setup", vm.appLanguage)) {
                        HStack {
                            Text("1 bottle =")
                            TextField("Size", text: $bottleSizeText)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                            Picker("", selection: $bottleBaseUnit) {
                                Text("oz").tag("oz")
                                Text("ml").tag("ml")
                            }
                            .pickerStyle(.menu)
                            .frame(width: 60)
                        }
                        Text("Goal and intake will be tracked in \(bottleBaseUnit). Quick-add shows bottle fractions (¼, ½, ¾, 1).")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Section(L10n.t("appearance", vm.appLanguage)) {
                    Toggle(L10n.t("dark_mode", vm.appLanguage), isOn: $vm.isDarkMode)
                }

                Section(L10n.t("language", vm.appLanguage)) {
                    Picker(L10n.t("language", vm.appLanguage), selection: $vm.appLanguage) {
                        ForEach(L10n.supportedLanguages, id: \.code) { lang in
                            Text(lang.name).tag(lang.code)
                        }
                    }
                }

                Section {
                    Button(L10n.t("save", vm.appLanguage)) {
                        let goal = Double(goalText) ?? 0
                        vm.setGoal(goal)
                        if selectedUnit == "bottle" {
                            let bSize = Double(bottleSizeText) ?? 0
                            vm.setBottleSize(bSize)
                            vm.setUnit("bottle")
                        } else {
                            vm.setUnit(selectedUnit)
                        }
                        dismiss()
                    }
                    .frame(maxWidth: .infinity)
                    .font(.headline)
                }
            }
            .navigationTitle(L10n.t("settings", vm.appLanguage))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(L10n.t("cancel", vm.appLanguage)) { dismiss() }
                }
            }
            .onAppear {
                goalText = vm.dailyGoal > 0 ? formatAmount(vm.dailyGoal) : ""
                selectedUnit = vm.unit
                bottleSizeText = vm.bottleSize > 0 ? formatAmount(vm.bottleSize) : "24"
            }
        }
    }

    private func formatAmount(_ value: Double) -> String {
        if value.truncatingRemainder(dividingBy: 1) == 0 {
            return String(format: "%.0f", value)
        }
        return String(format: "%.1f", value)
    }
}

#Preview {
    SettingsView()
        .environmentObject(WaterViewModel())
}
