import SwiftUI

/// Sheet that lets the user type a custom water amount and add it.
struct CustomAddSheet: View {
    @EnvironmentObject var vm: WaterViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var amountText: String = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                Image(systemName: "drop.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.blue)

                Text("Add Custom Amount")
                    .font(.title2.bold())

                HStack {
                    TextField("Amount", text: $amountText)
                        .keyboardType(.decimalPad)
                        .font(.title)
                        .multilineTextAlignment(.center)
                        .padding()
                        .background(Color(.secondarySystemGroupedBackground))
                        .cornerRadius(12)

                    Text(vm.unit)
                        .font(.title2)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 40)

                Button {
                    if let amount = Double(amountText), amount > 0 {
                        vm.addIntake(amount)
                        dismiss()
                    }
                } label: {
                    Text("Add")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 40)

                Spacer()
                Spacer()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

#Preview {
    CustomAddSheet()
        .environmentObject(WaterViewModel())
}
