import Foundation

struct ManualEvent: Identifiable, Codable {
    let id: UUID
    var title: String
    var startDate: Date
    var endDate: Date
    var meetingURL: String

    init(id: UUID = UUID(), title: String, startDate: Date, endDate: Date, meetingURL: String = "") {
        self.id = id
        self.title = title
        self.startDate = startDate
        self.endDate = endDate
        self.meetingURL = meetingURL
    }
}

@MainActor
class ManualEventsStore: ObservableObject {

    static let shared = ManualEventsStore()

    @Published private(set) var events: [ManualEvent] = []

    private let key = "manual_events"

    init() { load() }

    func add(_ event: ManualEvent) {
        events.append(event)
        save()
    }

    func delete(id: UUID) {
        events.removeAll { $0.id == id }
        save()
    }

    var todayEvents: [ManualEvent] {
        let cal = Calendar.current
        let start = cal.startOfDay(for: Date())
        let end   = cal.date(byAdding: .day, value: 1, to: start)!
        return events.filter { $0.startDate >= start && $0.startDate < end }
            .sorted { $0.startDate < $1.startDate }
    }

    private func save() {
        if let data = try? JSONEncoder().encode(events) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([ManualEvent].self, from: data)
        else { return }
        events = decoded
    }
}
