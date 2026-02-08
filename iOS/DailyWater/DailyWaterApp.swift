import SwiftUI

@main
struct DailyWaterApp: App {
    @StateObject private var viewModel = WaterViewModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
                .preferredColorScheme(viewModel.isDarkModeSet ? (viewModel.isDarkMode ? .dark : .light) : nil)
                .onChange(of: scenePhase) { newPhase in
                    if newPhase == .active {
                        // Daily reset check: fires every time the app comes to foreground.
                        // Compares stored dateKey (yyyy-MM-dd in local TZ) to today's date.
                        viewModel.checkForNewDayAndResetIfNeeded()
                    }
                }
        }
    }
}
