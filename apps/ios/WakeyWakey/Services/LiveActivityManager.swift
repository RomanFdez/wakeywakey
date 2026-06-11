import ActivityKit
import Foundation

@available(iOS 16.2, *)
@MainActor
class LiveActivityManager {

    static let shared = LiveActivityManager()
    private var currentActivity: Activity<MeetingActivityAttributes>?
    private var updateTask: Task<Void, Never>?

    func start(for meeting: AnyMeeting) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        let state = MeetingActivityAttributes.ContentState(
            title: meeting.title,
            startTime: meeting.startDate,
            meetingURL: meeting.meetingURL?.absoluteString,
            isOngoing: meeting.startDate <= Date()
        )
        let attrs = MeetingActivityAttributes(calendarName: meeting.calendarName)

        do {
            if #available(iOS 17.0, *) {
                currentActivity = try Activity.request(
                    attributes: attrs,
                    content: ActivityContent(state: state, staleDate: nil),
                    pushType: nil
                )
            } else {
                currentActivity = try Activity.request(
                    attributes: attrs,
                    contentState: state,
                    pushType: nil
                )
            }
        } catch {
            // Live Activities not supported or limit reached
        }
    }

    func update(for meeting: AnyMeeting) {
        guard let activity = currentActivity else { start(for: meeting); return }
        let state = MeetingActivityAttributes.ContentState(
            title: meeting.title,
            startTime: meeting.startDate,
            meetingURL: meeting.meetingURL?.absoluteString,
            isOngoing: meeting.startDate <= Date()
        )
        pushUpdate(state, to: activity)
    }

    /// Call when the meeting notification fires — marks the activity as started
    /// without needing the full AnyMeeting object.
    func markOngoing() {
        guard let activity = currentActivity else { return }
        let current = activity.contentState
        let state = MeetingActivityAttributes.ContentState(
            title: current.title,
            startTime: current.startTime,
            meetingURL: current.meetingURL,
            isOngoing: true
        )
        pushUpdate(state, to: activity)
    }

    func end() {
        guard let activity = currentActivity else { return }
        updateTask?.cancel()
        if #available(iOS 17.0, *) {
            Task { await activity.end(ActivityContent(state: activity.contentState, staleDate: nil),
                                      dismissalPolicy: .immediate) }
        } else {
            Task { await activity.end(dismissalPolicy: .immediate) }
        }
        currentActivity = nil
    }

    func sync(nextMeeting: AnyMeeting?) {
        guard let meeting = nextMeeting else { end(); return }

        let secsUntil   = meeting.startDate.timeIntervalSince(Date())
        let secsElapsed = Date().timeIntervalSince(meeting.startDate)

        if secsElapsed > 2 * 60 { end(); return }
        guard secsUntil < 5 * 60 || meeting.startDate <= Date() else { end(); return }

        if currentActivity != nil { update(for: meeting) } else { start(for: meeting) }
    }

    // MARK: - Private

    private func pushUpdate(_ state: MeetingActivityAttributes.ContentState,
                            to activity: Activity<MeetingActivityAttributes>) {
        updateTask?.cancel()
        updateTask = Task {
            if #available(iOS 17.0, *) {
                await activity.update(ActivityContent(state: state, staleDate: nil))
            } else {
                await activity.update(using: state)
            }
        }
    }
}
