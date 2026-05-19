import SwiftUI

struct TrialStep: View {

    let onFinish: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            ZStack {
                Circle()
                    .fill(Color.wkYellow.opacity(0.15))
                    .frame(width: 120, height: 120)
                Image(systemName: "star.fill")
                    .font(.system(size: 52))
                    .foregroundStyle(Color.wkYellow)
            }
            .padding(.bottom, 32)

            Text("7 días Pro gratis")
                .font(.system(size: 34, weight: .heavy, design: .rounded))
                .foregroundStyle(Color.wkYellow)

            Text("Acceso completo a todas las funciones.\nSin tarjeta de crédito.")
                .font(.system(size: 17))
                .foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center)
                .padding(.top, 12)
                .padding(.horizontal, 40)

            VStack(alignment: .leading, spacing: 14) {
                FeatureRow(icon: "bell.badge.fill",  text: "Alertas antes de cada reunión")
                FeatureRow(icon: "link",             text: "Join directo a Meet, Zoom, Teams…")
                FeatureRow(icon: "moon.fill",        text: "Modo no molestar por horas")
            }
            .padding(.top, 36)
            .padding(.horizontal, 40)

            Spacer()

            WKButton("Activar prueba gratuita", action: onFinish)
                .padding(.horizontal, 32)
                .padding(.bottom, 48)
        }
    }
}

private struct FeatureRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(Color.wkYellow)
                .frame(width: 26)
            Text(text)
                .font(.system(size: 16))
                .foregroundStyle(.white.opacity(0.85))
        }
    }
}
