import SwiftUI

private extension Color {
    static let wkNavy    = Color(red: 0.098, green: 0.098, blue: 0.176)
    static let wkYellow  = Color(red: 1.0,   green: 0.878, blue: 0.227)
}

/// Formulario para crear una alerta ad-hoc (NO se guarda en el calendario del
/// sistema; se almacena en `ManualEventsStore`).
struct AddEventView: View {
    var onSave: (ManualEvent) -> Void
    var onCancel: () -> Void

    @State private var title = ""
    @State private var date: Date = Calendar.current.date(
        bySettingHour: 15, minute: 0, second: 0, of: Date()
    ) ?? Date()

    private var canSave: Bool { !title.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Add event")
                .font(.system(size: 20, weight: .heavy))
                .foregroundStyle(Color.wkYellow)

            VStack(alignment: .leading, spacing: 6) {
                fieldLabel("Title")
                TextField("Event name", text: $title)
                    .textFieldStyle(.plain)
                    .foregroundStyle(.white)
                    .padding(10)
                    .background(RoundedRectangle(cornerRadius: 8).stroke(.white.opacity(0.15), lineWidth: 1))
            }

            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    fieldLabel("Date")
                    DatePicker("", selection: $date, displayedComponents: .date)
                        .labelsHidden()
                        .datePickerStyle(.field)
                }
                VStack(alignment: .leading, spacing: 6) {
                    fieldLabel("Time")
                    DatePicker("", selection: $date, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                        .datePickerStyle(.field)
                }
            }

            HStack(spacing: 14) {
                Spacer()
                Button("Cancel", action: onCancel)
                    .buttonStyle(.plain)
                    .foregroundStyle(.white.opacity(0.7))
                Button {
                    let t = title.trimmingCharacters(in: .whitespaces)
                    guard !t.isEmpty else { return }
                    onSave(ManualEvent(title: t, startDate: date, endDate: date.addingTimeInterval(1800)))
                } label: {
                    Text("Save")
                        .fontWeight(.bold)
                        .foregroundStyle(Color.wkNavy)
                        .padding(.horizontal, 20).padding(.vertical, 8)
                        .background(Color.wkYellow.opacity(canSave ? 1 : 0.4))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .disabled(!canSave)
            }
            .padding(.top, 4)
        }
        .padding(22)
        .frame(width: 340)
        .background(Color.wkNavy)
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12))
            .foregroundStyle(.white.opacity(0.6))
    }
}
