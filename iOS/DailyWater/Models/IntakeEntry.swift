import Foundation

/// A single water intake record.
struct IntakeEntry: Identifiable, Codable, Equatable {
    let id: UUID
    let timestamp: Date
    let amount: Double

    init(id: UUID = UUID(), timestamp: Date = Date(), amount: Double) {
        self.id = id
        self.timestamp = timestamp
        self.amount = amount
    }
}
