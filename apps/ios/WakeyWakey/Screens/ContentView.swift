import SwiftUI

// Slice 0 bootstrap — placeholder hasta Slice 2 (HomeScreen)
struct ContentView: View {
    var body: some View {
        ZStack {
            Color(hex: "#1A1A2E").ignoresSafeArea()

            VStack(spacing: 20) {
                Image(systemName: "alarm.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(Color(hex: "#FFE03A"))

                Text("WakeyWakey")
                    .font(.system(size: 36, weight: .heavy, design: .rounded))
                    .foregroundStyle(Color(hex: "#FFE03A"))

                Text("iOS · Slice 0 ✓")
                    .font(.system(size: 15))
                    .foregroundStyle(.white.opacity(0.6))
            }
        }
    }
}

// MARK: - Color hex helper

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255
        let g = Double((int >> 8)  & 0xFF) / 255
        let b = Double( int        & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}
