import Foundation
import SwiftUI

/// Alimenta el texto del icono de la barra de menú según los ajustes de "Menu Bar".
@MainActor
final class MenuBarController: ObservableObject {

    static let shared = MenuBarController()

    @Published var menuBarTitle: String = ""

    private let calendar = CalendarService.shared
    private let settings = SettingsStore.shared
    private var timer: Timer?
    private var lastLoad = Date.distantPast

    private init() {}

    func start() {
        Task {
            if !calendar.isAuthorized { _ = await calendar.requestCalendarAccess() }
            calendar.loadWeekEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
            lastLoad = Date()
            tick()
        }
        let t = Timer(timeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
        RunLoop.main.add(t, forMode: .common)
        timer = t
    }

    private func tick() {
        let now = Date()
        if calendar.isAuthorized, now.timeIntervalSince(lastLoad) > 30 {
            calendar.loadWeekEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
            lastLoad = now
        }

        let cal = Calendar.current
        let m = MacSettings.shared
        let upcoming = (calendar.weekEvents
                        + ManualEventsStore.shared.weekEvents.map { AnyMeeting(manual: $0) })
            .filter { $0.endDate > now }
            .sorted { $0.startDate < $1.startDate }

        guard let next = upcoming.first else { menuBarTitle = ""; return }

        let isToday    = cal.isDateInToday(next.startDate)
        let isTomorrow = cal.isDateInTomorrow(next.startDate)
        if !isToday && !(isTomorrow && m.includeTomorrow) { menuBarTitle = ""; return }

        menuBarTitle = Self.composeTitle(name: next.title, startDate: next.startDate,
                                         now: now, isToday: isToday, isTomorrow: isTomorrow, s: m)
        #if DEBUG
        NSLog("WW-MB upcoming=\(upcoming.count) next=\(next.title) isToday=\(isToday) isTomorrow=\(isTomorrow) title='\(menuBarTitle)'")
        #endif
    }

    /// Construye el texto del icono (compartido con el preview de Settings).
    static func composeTitle(name: String, startDate: Date, now: Date,
                             isToday: Bool, isTomorrow: Bool, s: MacSettings) -> String {
        var parts: [String] = []
        if s.showMeetingName {
            parts.append(truncate(name, to: s.titleLength, middle: s.truncateMiddle))
        }
        if isToday {
            if s.showTimeRemaining {
                parts.append(countdown(startDate: startDate, now: now, minutesOnly: s.minutesOnly))
            }
        } else if isTomorrow {
            parts.append("Tomorrow")
        }
        return parts.joined(separator: " · ")
    }

    static func truncate(_ s: String, to len: Int, middle: Bool) -> String {
        guard s.count > len, len > 1 else { return s }
        if middle {
            let head = max(1, len / 2 - 1)
            let tail = max(1, len - head - 1)
            return String(s.prefix(head)) + "…" + String(s.suffix(tail))
        }
        return String(s.prefix(len - 1)) + "…"
    }

    static func countdown(startDate: Date, now: Date, minutesOnly: Bool) -> String {
        let secs = Int(startDate.timeIntervalSince(now))
        if secs <= 0 { return "now" }
        if secs < 3600 {
            // Redondeo hacia arriba: mientras no haya empezado nunca muestra 0m.
            if minutesOnly { return "\((secs + 59) / 60)m" }
            let mm = secs / 60, ss = secs % 60
            return mm > 0 ? "\(mm)m \(ss)s" : "\(ss)s"
        }
        let h = secs / 3600, mm = (secs % 3600) / 60
        return mm == 0 ? "\(h)h" : "\(h)h \(mm)m"
    }
}
