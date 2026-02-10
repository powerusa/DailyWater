//
//  DailyWater_Watch_App.swift
//  DailyWater Watch App
//
//  Created by Pawel Palka on 2/8/26.
//

import AppIntents

struct DailyWater_Watch_App: AppIntent {
    static var title: LocalizedStringResource { "DailyWater Watch App" }
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}
