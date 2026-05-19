import SwiftUI
import EventKit

struct CalendarPickerStep: View {

    let onNext: () -> Void

    @EnvironmentObject var settings: SettingsStore
    @EnvironmentObject var calendarService: CalendarService

    var body: some View {
        VStack(spacing: 0) {
            Text("Elige tus calendarios")
                .font(.system(size: 28, weight: .heavy, design: .rounded))
                .foregroundStyle(Color.wkYellow)
                .padding(.top, 60)
                .padding(.bottom, 8)

            Text("Solo recibirás alertas de los calendarios\nque actives aquí.")
                .font(.system(size: 16))
                .foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 36)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach(calendarService.availableCalendars, id: \.calendarIdentifier) { cal in
                        CalendarRow(calendar: cal)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)
            }

            WKButton("Continuar", action: onNext)
                .padding(.horizontal, 32)
                .padding(.vertical, 24)
        }
    }
}

private struct CalendarRow: View {

    let calendar: EKCalendar
    @EnvironmentObject var settings: SettingsStore

    private var isEnabled: Bool {
        settings.enabledCalendarIds.contains(calendar.calendarIdentifier)
    }

    var body: some View {
        Button {
            if isEnabled {
                settings.enabledCalendarIds.remove(calendar.calendarIdentifier)
            } else {
                settings.enabledCalendarIds.insert(calendar.calendarIdentifier)
            }
        } label: {
            HStack(spacing: 14) {
                Circle()
                    .fill(Color(cgColor: calendar.cgColor))
                    .frame(width: 14, height: 14)

                VStack(alignment: .leading, spacing: 2) {
                    Text(calendar.title)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(.white)
                    Text(calendar.source?.title ?? "")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.45))
                }

                Spacer()

                Image(systemName: isEnabled ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22))
                    .foregroundStyle(isEnabled ? Color.wkYellow : .white.opacity(0.3))
            }
            .padding(16)
            .background(Color.white.opacity(isEnabled ? 0.12 : 0.06))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }
}
