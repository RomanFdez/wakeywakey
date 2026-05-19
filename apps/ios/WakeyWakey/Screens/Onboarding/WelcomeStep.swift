import SwiftUI

struct WelcomeStep: View {

    let onNext: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Image(systemName: "alarm.fill")
                .font(.system(size: 80))
                .foregroundStyle(Color.wkYellow)
                .padding(.bottom, 32)

            Text("WakeyWakey")
                .font(.system(size: 38, weight: .heavy, design: .rounded))
                .foregroundStyle(Color.wkYellow)

            Text("Nunca más llegues tarde\na una reunión")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(.white.opacity(0.75))
                .multilineTextAlignment(.center)
                .padding(.top, 12)
                .padding(.horizontal, 40)

            Spacer()

            WKButton("Empezar", action: onNext)
                .padding(.horizontal, 32)
                .padding(.bottom, 48)
        }
    }
}
