import SwiftUI
import UserNotifications

struct PermissionsStep: View {

    let onNext: () -> Void

    @EnvironmentObject var calendarService: CalendarService
    @State private var calendarGranted    = false
    @State private var notifGranted       = false
    @State private var requesting         = false

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Text("Permisos necesarios")
                .font(.system(size: 28, weight: .heavy, design: .rounded))
                .foregroundStyle(Color.wkYellow)
                .padding(.bottom, 8)

            Text("WakeyWakey necesita leer tu calendario\ny enviarte alertas antes de las reuniones.")
                .font(.system(size: 16))
                .foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            VStack(spacing: 16) {
                PermissionRow(
                    icon: "calendar",
                    title: "Calendario",
                    description: "Para ver tus reuniones",
                    granted: calendarGranted
                )
                PermissionRow(
                    icon: "bell.fill",
                    title: "Notificaciones",
                    description: "Para avisarte antes de empezar",
                    granted: notifGranted
                )
            }
            .padding(.horizontal, 24)
            .padding(.top, 40)

            Spacer()

            if calendarGranted && notifGranted {
                WKButton("Continuar", action: onNext)
                    .padding(.horizontal, 32)
                    .padding(.bottom, 48)
            } else {
                WKButton(requesting ? "Solicitando…" : "Conceder permisos") {
                    Task { await requestAll() }
                }
                .disabled(requesting)
                .padding(.horizontal, 32)
                .padding(.bottom, 48)
            }
        }
    }

    private func requestAll() async {
        requesting = true
        calendarGranted = await calendarService.requestCalendarAccess()
        notifGranted    = await requestNotifications()
        requesting = false
    }

    private func requestNotifications() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
    }
}

private struct PermissionRow: View {
    let icon: String
    let title: String
    let description: String
    let granted: Bool

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundStyle(Color.wkYellow)
                .frame(width: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                Text(description)
                    .font(.system(size: 13))
                    .foregroundStyle(.white.opacity(0.55))
            }

            Spacer()

            Image(systemName: granted ? "checkmark.circle.fill" : "circle")
                .font(.system(size: 22))
                .foregroundStyle(granted ? Color.wkYellow : .white.opacity(0.3))
        }
        .padding(16)
        .background(Color.white.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
