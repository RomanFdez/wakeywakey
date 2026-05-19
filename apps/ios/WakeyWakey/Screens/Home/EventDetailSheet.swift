import SwiftUI

struct EventDetailSheet: View {

    let meeting: AnyMeeting
    let onDelete: (() -> Void)?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                Color.wkNavy.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {

                        // ── Cabecera con color del calendario ────────────
                        HStack(spacing: 10) {
                            if let cgColor = meeting.calendarColor {
                                Circle()
                                    .fill(Color(cgColor: cgColor))
                                    .frame(width: 10, height: 10)
                            }
                            Text(meeting.calendarName ?? "")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(.white.opacity(0.5))
                        }
                        .padding(.bottom, 10)

                        // ── Título ───────────────────────────────────────
                        Text(meeting.title)
                            .font(.system(size: 26, weight: .heavy, design: .rounded))
                            .foregroundStyle(.white)
                            .padding(.bottom, 16)

                        // ── Hora ─────────────────────────────────────────
                        detailRow(icon: "clock.fill") {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(dateString(meeting.startDate))
                                    .foregroundStyle(.white)
                                Text("\(timeString(meeting.startDate)) – \(timeString(meeting.endDate))  ·  \(durationText)")
                                    .foregroundStyle(.white.opacity(0.55))
                                    .font(.system(size: 14))
                            }
                        }

                        // ── Ubicación ────────────────────────────────────
                        if let loc = meeting.location {
                            Divider().background(Color.white.opacity(0.08)).padding(.vertical, 12)
                            detailRow(icon: "location.fill") {
                                Text(loc)
                                    .foregroundStyle(.white)
                            }
                        }

                        // ── Link de reunión ──────────────────────────────
                        if let url = meeting.meetingURL {
                            Divider().background(Color.white.opacity(0.08)).padding(.vertical, 12)
                            detailRow(icon: "video.fill") {
                                Button {
                                    UIApplication.shared.open(url)
                                    dismiss()
                                } label: {
                                    Text(url.host ?? url.absoluteString)
                                        .foregroundStyle(Color.wkYellow)
                                        .underline()
                                }
                            }
                        }

                        // ── Notas ────────────────────────────────────────
                        if let notes = meeting.notes {
                            Divider().background(Color.white.opacity(0.08)).padding(.vertical, 12)
                            detailRow(icon: "note.text") {
                                Text(notes)
                                    .foregroundStyle(.white.opacity(0.75))
                                    .font(.system(size: 14))
                            }
                        }

                        // ── Join button ──────────────────────────────────
                        if let url = meeting.meetingURL {
                            Button {
                                UIApplication.shared.open(url)
                                dismiss()
                            } label: {
                                Label("Join now", systemImage: "video.fill")
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundStyle(Color.wkNavy)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 54)
                                    .background(Color.wkYellow)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .padding(.top, 24)
                        }

                        // ── Eliminar (solo manual) ───────────────────────
                        if let onDelete {
                            Button(role: .destructive) {
                                onDelete()
                                dismiss()
                            } label: {
                                Label("Eliminar evento", systemImage: "trash")
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundStyle(Color.wkCoral)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color.wkCoral.opacity(0.12))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .padding(.top, 12)
                        }

                        Spacer(minLength: 32)
                    }
                    .padding(24)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Cerrar") { dismiss() }
                        .foregroundStyle(.white.opacity(0.6))
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Helpers

    private func detailRow<Content: View>(icon: String, @ViewBuilder content: () -> Content) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 15))
                .foregroundStyle(Color.wkYellow)
                .frame(width: 20)
                .padding(.top, 2)
            content()
            Spacer()
        }
    }

    private func dateString(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "EEEE, d MMMM"
        f.locale = Locale(identifier: "es")
        return f.string(from: date).capitalized
    }

    private func timeString(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: date)
    }

    private var durationText: String {
        let mins = Int(meeting.endDate.timeIntervalSince(meeting.startDate) / 60)
        if mins < 60 { return "\(mins)m" }
        let h = mins / 60; let m = mins % 60
        return m == 0 ? "\(h)h" : "\(h)h \(m)m"
    }
}
