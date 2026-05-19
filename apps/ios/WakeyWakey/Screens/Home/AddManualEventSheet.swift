import SwiftUI

struct AddManualEventSheet: View {

    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: ManualEventsStore

    @State private var title      = ""
    @State private var startDate  = Date().nearestFutureHalfHour
    @State private var endDate    = Date().nearestFutureHalfHour.addingTimeInterval(3600)
    @State private var meetingURL = ""

    private var canSave: Bool { !title.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.wkNavy.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 16) {

                        // ── Nombre ───────────────────────────────────────
                        field(label: "Nombre del evento") {
                            TextField("Standup, revisión…", text: $title)
                                .foregroundStyle(.white)
                        }

                        // ── Inicio / Fin ─────────────────────────────────
                        VStack(spacing: 0) {
                            dateRow(label: "Inicio", selection: $startDate)
                            Divider().background(Color.white.opacity(0.1))
                            dateRow(label: "Fin", selection: Binding(
                                get: { endDate },
                                set: { endDate = max($0, startDate.addingTimeInterval(60)) }
                            ))
                        }
                        .background(Color.white.opacity(0.07))
                        .clipShape(RoundedRectangle(cornerRadius: 14))

                        // ── URL ──────────────────────────────────────────
                        field(label: "URL de videollamada (opcional)") {
                            TextField("https://meet.google.com/…", text: $meetingURL)
                                .foregroundStyle(.white)
                                .keyboardType(.URL)
                                .autocorrectionDisabled()
                                .textInputAutocapitalization(.never)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 24)
                }
            }
            .navigationTitle("Añadir evento")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancelar") { dismiss() }
                        .foregroundStyle(.white.opacity(0.6))
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Guardar") { save() }
                        .foregroundStyle(canSave ? Color.wkYellow : Color.wkYellow.opacity(0.4))
                        .fontWeight(.semibold)
                        .disabled(!canSave)
                }
            }
        }
    }

    // MARK: - Sub-views

    private func field<Content: View>(label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(.white.opacity(0.5))
                .kerning(1.0)

            content()
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(Color.white.opacity(0.07))
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    private func dateRow(label: String, selection: Binding<Date>) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(.white)
                .frame(width: 50, alignment: .leading)
            Spacer()
            DatePicker("", selection: selection, displayedComponents: [.date, .hourAndMinute])
                .labelsHidden()
                .tint(Color.wkYellow)
                .colorScheme(.dark)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: - Actions

    private func save() {
        store.add(ManualEvent(
            title: title.trimmingCharacters(in: .whitespaces),
            startDate: startDate,
            endDate: endDate,
            meetingURL: meetingURL.trimmingCharacters(in: .whitespaces)
        ))
        dismiss()
    }
}

private extension Date {
    var nearestFutureHalfHour: Date {
        let cal = Calendar.current
        let mins = cal.component(.minute, from: self)
        let add = mins < 30 ? 30 - mins : 60 - mins
        return cal.date(byAdding: .minute, value: add, to: self)!
    }
}
