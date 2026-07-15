import SwiftUI
import RevenueCat

private extension Color {
    static let wkNavy   = Color(red: 0.098, green: 0.098, blue: 0.176)
    static let wkYellow = Color(red: 1.0,   green: 0.878, blue: 0.227)
}

/// Paywall (se muestra al expirar el trial, y desde Settings).
struct PaywallView: View {
    var onClose: () -> Void
    @StateObject private var entitlement = MacEntitlementManager.shared

    // Requeridos por la review 3.1.2(c): enlaces funcionales a privacidad y EULA.
    static let privacyPolicyURL = URL(string: "https://www.sierraespada.com/legal/privacy/")!
    static let termsOfUseURL    = URL(string: "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")!

    private let perks = [
        "Full-screen meeting alerts you can't miss",
        "Unlimited calendars",
        "All alert sounds",
        "Working hours scheduling",
        "Custom snooze",
    ]

    var body: some View {
        VStack(spacing: 16) {
            Text("⏰").font(.system(size: 48))
            Text("Unlock WakeyWakey Pro")
                .font(.system(size: 22, weight: .heavy)).foregroundStyle(.white)
            Text(entitlement.trialDaysLeft > 0 ? "Try free for \(entitlement.trialDaysLeft) more days"
                                               : "Your free trial has ended")
                .font(.system(size: 13)).foregroundStyle(.white.opacity(0.6))

            VStack(alignment: .leading, spacing: 8) {
                ForEach(perks, id: \.self) { p in
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(Color.wkYellow)
                        Text(p).font(.system(size: 13)).foregroundStyle(.white.opacity(0.85))
                        Spacer()
                    }
                }
            }
            .padding(.vertical, 8)

            if let packages = entitlement.offerings?.current?.availablePackages, !packages.isEmpty {
                ForEach(packages, id: \.identifier) { pkg in
                    Button { Task { await entitlement.purchase(pkg); if entitlement.isPro { onClose() } } } label: {
                        HStack {
                            Text(pkg.storeProduct.localizedTitle).fontWeight(.semibold)
                            Spacer()
                            // 3.1.2(c): precio + duración de la suscripción visibles.
                            Text(pkg.storeProduct.localizedPriceString + Self.periodSuffix(pkg))
                                .fontWeight(.bold)
                        }
                        .foregroundStyle(Color.wkNavy)
                        .padding(.horizontal, 16).padding(.vertical, 11)
                        .frame(maxWidth: .infinity)
                        .background(Color.wkYellow).clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            } else {
                Text("Subscription options will appear once configured in App Store Connect.")
                    .font(.system(size: 11)).foregroundStyle(.white.opacity(0.4))
                    .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            }

            HStack(spacing: 16) {
                Button("Restore") { Task { await entitlement.restore() } }
                    .buttonStyle(.plain).foregroundStyle(Color.wkYellow).font(.system(size: 12, weight: .semibold))
                Button("Maybe later") { onClose() }
                    .buttonStyle(.plain).foregroundStyle(.white.opacity(0.5)).font(.system(size: 12))
            }

            // 3.1.2(c): nota de renovación + enlaces funcionales a privacidad y EULA.
            VStack(spacing: 6) {
                Text("Subscriptions renew automatically unless cancelled at least 24 hours before the end of the period. Manage or cancel anytime in your App Store account settings.")
                    .font(.system(size: 10)).foregroundStyle(.white.opacity(0.35))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 14) {
                    Link("Privacy Policy", destination: Self.privacyPolicyURL)
                    Link("Terms of Use (EULA)", destination: Self.termsOfUseURL)
                }
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(.white.opacity(0.55))
            }
            .padding(.top, 2)
        }
        .frame(width: 380)
        .padding(28)
        .background(Color.wkNavy)
    }

    /// Sufijo de duración para el botón de compra ("/ month", "/ year"; lifetime sin sufijo).
    private static func periodSuffix(_ pkg: Package) -> String {
        switch pkg.packageType {
        case .monthly:  return " / month"
        case .annual:   return " / year"
        case .weekly:   return " / week"
        case .lifetime: return ""
        default:
            guard let unit = pkg.storeProduct.subscriptionPeriod?.unit else { return "" }
            switch unit {
            case .month: return " / month"
            case .year:  return " / year"
            case .week:  return " / week"
            case .day:   return " / day"
            }
        }
    }
}
