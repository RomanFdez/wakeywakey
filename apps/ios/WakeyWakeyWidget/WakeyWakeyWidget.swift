import WidgetKit
import SwiftUI

@main
struct WakeyWakeyWidgetBundle: WidgetBundle {
    var body: some Widget {
        NextMeetingWidget()
        if #available(iOSApplicationExtension 16.2, *) {
            MeetingLiveActivity()
        }
    }
}
