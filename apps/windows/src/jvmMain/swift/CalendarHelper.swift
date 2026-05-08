// WakeyWakey CalendarHelper
// Reads macOS Calendar events via EventKit and prints pipe-delimited rows to stdout.
// Run by MacSystemCalendarRepository as a subprocess — TCC attributes EventKit access
// to the responsible process (WakeyWakey.app), not to this binary.
//
// Usage:
//   CalendarHelper calendars
//   CalendarHelper events <fromUnixMs> <toUnixMs> <includeAllDay:0|1>
//
// Output format:
//   calendars → name|id\n per calendar
//   events    → title|startMs|endMs|location|url|calName|calId|isAllDay\n per event

import Foundation
import EventKit
import AppKit

let store = EKEventStore()
let args  = CommandLine.arguments

guard args.count >= 2 else {
    fputs("usage: CalendarHelper calendars | events <fromMs> <toMs> <allDay>\n", stderr)
    exit(1)
}

// ── Request access ────────────────────────────────────────────────────────────

let sem = DispatchSemaphore(value: 0)
var accessGranted = false

if #available(macOS 14.0, *) {
    store.requestFullAccessToEvents { granted, _ in
        accessGranted = granted
        sem.signal()
    }
} else {
    store.requestAccess(to: .event) { granted, _ in
        accessGranted = granted
        sem.signal()
    }
}
sem.wait()

guard accessGranted else {
    fputs("error: calendar access denied\n", stderr)
    exit(2)
}

// ── Command dispatch ──────────────────────────────────────────────────────────

let cmd = args[1]

if cmd == "calendars" {
    let calendars = store.calendars(for: .event)
    for cal in calendars {
        let colorHex: String = {
            guard let c = cal.color.usingColorSpace(NSColorSpace.sRGB) else { return "" }
            let r = Int(max(0, min(1, c.redComponent))   * 255)
            let g = Int(max(0, min(1, c.greenComponent)) * 255)
            let b = Int(max(0, min(1, c.blueComponent))  * 255)
            return String(format: "%02X%02X%02X", r, g, b)
        }()
        // source.title da el nombre de cuenta: "Gmail", "iCloud", nombre de exchange, etc.
        let accountName = cal.source?.title ?? "Calendar"
        print("\(cal.title)|\(cal.calendarIdentifier)|\(colorHex)|\(accountName)")
    }
    exit(0)
}

if cmd == "events" {
    guard args.count >= 5,
          let fromMs  = Int64(args[2]),
          let toMs    = Int64(args[3]),
          let allDayI = Int(args[4]) else {
        fputs("usage: CalendarHelper events <fromMs> <toMs> <allDay:0|1>\n", stderr)
        exit(1)
    }
    let includeAllDay = allDayI != 0
    let fromDate = Date(timeIntervalSince1970: Double(fromMs) / 1000.0)
    let toDate   = Date(timeIntervalSince1970: Double(toMs)   / 1000.0)

    let predicate = store.predicateForEvents(withStart: fromDate, end: toDate, calendars: nil)
    let events    = store.events(matching: predicate)

    for ev in events {
        guard let cal = ev.calendar else { continue }
        if ev.isAllDay && !includeAllDay { continue }
        let title   = ev.title ?? ""
        let startMs = Int64((ev.startDate?.timeIntervalSince1970 ?? 0) * 1000)
        let endMs   = Int64((ev.endDate?.timeIntervalSince1970   ?? 0) * 1000)
        let loc     = ev.location ?? ""
        let calName = cal.title
        let calId   = cal.calendarIdentifier

        // Try ev.url first; fall back to extracting a video-call URL from notes.
        // Teams, Zoom, Meet etc. embed the join link in the event body/notes.
        let url: String = {
            if let u = ev.url?.absoluteString, !u.isEmpty { return u }
            let notes = ev.notes ?? ""
            // Patterns for common video platforms
            let patterns = [
                "https://teams\\.microsoft\\.com/l/meetup-join/[^\\s<\"']+",
                "https://teams\\.live\\.com/meet/[^\\s<\"']+",
                "https://[a-z0-9]+\\.zoom\\.us/[^\\s<\"']+",
                "https://meet\\.google\\.com/[^\\s<\"']+",
                "https://whereby\\.com/[^\\s<\"']+",
                "https://[a-z0-9]+\\.webex\\.com/[^\\s<\"']+",
            ]
            for pattern in patterns {
                if let range = notes.range(of: pattern, options: .regularExpression) {
                    var match = String(notes[range])
                    // Strip trailing punctuation that may have been captured
                    while let last = match.last, ".>,;)".contains(last) { match.removeLast() }
                    return match
                }
            }
            return ""
        }()
        let allDay  = ev.isAllDay ? "true" : "false"
        // Sanitise pipe chars in text fields
        func clean(_ s: String) -> String { s.replacingOccurrences(of: "|", with: "｜")
                                              .replacingOccurrences(of: "\n", with: " ") }
        let colorHex: String = {
            guard let c = cal.color.usingColorSpace(NSColorSpace.sRGB) else { return "" }
            let r = Int(max(0, min(1, c.redComponent))   * 255)
            let g = Int(max(0, min(1, c.greenComponent)) * 255)
            let b = Int(max(0, min(1, c.blueComponent))  * 255)
            return String(format: "%02X%02X%02X", r, g, b)
        }()
        let recurring = ev.hasRecurrenceRules ? "true" : "false"
        print("\(clean(title))|\(startMs)|\(endMs)|\(clean(loc))|\(clean(url))|\(clean(calName))|\(calId)|\(allDay)|\(colorHex)|\(recurring)")
    }
    exit(0)
}

fputs("unknown command: \(cmd)\n", stderr)
exit(1)
