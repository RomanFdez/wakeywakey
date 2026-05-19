import SwiftUI

// MARK: - Colors

extension Color {
    static let wkYellow     = Color(hex: "#FFE03A")
    static let wkNavy       = Color(hex: "#1A1A2E")
    static let wkCoral      = Color(hex: "#FF6B6B")
    static let wkAlertNavy  = Color(hex: "#1B3A6B") // azul marino visible sobre fondo amarillo
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

// MARK: - Toggle style

struct WKToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            configuration.label
            Spacer()
            RoundedRectangle(cornerRadius: 16)
                .fill(configuration.isOn ? Color.wkYellow : Color.white.opacity(0.18))
                .frame(width: 51, height: 31)
                .overlay(
                    Circle()
                        .fill(.white)
                        .shadow(radius: 1)
                        .padding(3)
                        .offset(x: configuration.isOn ? 10 : -10)
                )
                .onTapGesture {
                    withAnimation(.spring(response: 0.25, dampingFraction: 0.8)) {
                        configuration.isOn.toggle()
                    }
                }
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
