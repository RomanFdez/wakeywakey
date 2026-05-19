import SwiftUI

// MARK: - Colors

extension Color {
    static let wkYellow = Color(hex: "#FFE03A")
    static let wkNavy   = Color(hex: "#1A1A2E")
    static let wkCoral  = Color(hex: "#FF6B6B")
}

// MARK: - Primary button

struct WKButton: View {
    let title: String
    let action: () -> Void

    init(_ title: String, action: @escaping () -> Void) {
        self.title  = title
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 18, weight: .bold, design: .rounded))
                .foregroundStyle(Color.wkNavy)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.wkYellow)
                .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}

// MARK: - Hex color init

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
