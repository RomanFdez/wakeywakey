import SwiftUI
import RevenueCat

struct PaywallView: View {

    @EnvironmentObject private var entitlements: EntitlementManager
    @Environment(\.dismiss)  private var dismiss

    // Plan seleccionado (anual por defecto — mejor valor)
    @State private var selectedPackage: Package?
    @State private var didBecomeProDuringSession = false

    private var current:  Offering?  { entitlements.offerings?.current }
    private var monthly:  Package?   { current?.monthly }
    private var annual:   Package?   { current?.annual }
    private var lifetime: Package?   { current?.lifetime }

    var body: some View {
        ZStack {
            Color.wkNavy.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {

                    // ── Cerrar (solo durante el trial, no cuando ha expirado) ──
                    HStack {
                        Spacer()
                        if entitlements.trialDaysLeft > 0 {
                            Button { dismiss() } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(.white.opacity(0.4))
                                    .padding(12)
                            }
                        } else {
                            // Espacio para mantener el layout consistente
                            Color.clear.frame(width: 40, height: 40)
                        }
                    }
                    .padding(.horizontal, 8)

                    // ── Hero ───────────────────────────────────────────────
                    Text("⭐")
                        .font(.system(size: 52))
                    Spacer().frame(height: 12)
                    Text("WakeyWakey Pro")
                        .font(.system(size: 28, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.wkYellow)
                    Spacer().frame(height: 6)
                    Text("Nunca más llegues tarde a una reunión")
                        .font(.system(size: 15))
                        .foregroundStyle(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)

                    // ── Banner trial expirado ──────────────────────────────
                    if entitlements.trialDaysLeft == 0 {
                        Spacer().frame(height: 12)
                        Text("Tu prueba gratuita ha terminado. Suscríbete para seguir usando WakeyWakey.")
                            .font(.system(size: 13))
                            .foregroundStyle(Color(red: 1, green: 0.42, blue: 0.42))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16).padding(.vertical, 8)
                            .background(Color(red: 1, green: 0.42, blue: 0.42).opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .padding(.horizontal, 28)
                    }

                    Spacer().frame(height: 28)

                    // ── Features ───────────────────────────────────────────
                    VStack(alignment: .leading, spacing: 12) {
                        featureRow("⏱", "Alertas personalizables antes de cada reunión")
                        featureRow("📅", "Todos tus calendarios sin límite")
                        featureRow("🕐", "Filtro por horario laboral y días")
                        featureRow("🔲", "Widget en pantalla de inicio")
                        featureRow("🏝", "Live Activity + Dynamic Island")
                        featureRow("💤", "Posponer reuniones")
                    }
                    .padding(.horizontal, 28)

                    Spacer().frame(height: 28)

                    // ── Plan selector ──────────────────────────────────────
                    VStack(spacing: 10) {
                        if current == nil {
                            ProgressView()
                                .tint(Color.wkYellow)
                                .frame(height: 140)
                        } else {
                            if let pkg = annual {
                                PlanCard(
                                    pkg:        pkg,
                                    label:      "Anual",
                                    badge:      "Mejor valor",
                                    badgeColor: Color.green,
                                    trialText:  trialDaysLeft > 0 ? "\(EntitlementManager.trialDays) días gratis, luego" : nil,
                                    isSelected: selectedPackage == pkg,
                                    onTap:      { selectedPackage = pkg }
                                )
                            }
                            if let pkg = monthly {
                                PlanCard(
                                    pkg:        pkg,
                                    label:      "Mensual",
                                    badge:      nil,
                                    badgeColor: .clear,
                                    trialText:  trialDaysLeft > 0 ? "\(EntitlementManager.trialDays) días gratis, luego" : nil,
                                    isSelected: selectedPackage == pkg,
                                    onTap:      { selectedPackage = pkg }
                                )
                            }
                            if let pkg = lifetime {
                                PlanCard(
                                    pkg:        pkg,
                                    label:      "De por vida",
                                    badge:      "Pago único",
                                    badgeColor: Color(red: 1, green: 0.42, blue: 0.42),
                                    trialText:  nil,
                                    isSelected: selectedPackage == pkg,
                                    onTap:      { selectedPackage = pkg }
                                )
                            }
                        }
                    }
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 24)

                    // ── CTA ────────────────────────────────────────────────
                    let isLifetime = selectedPackage?.packageType == .lifetime
                    let ctaLabel: String = {
                        if isLifetime            { return "Comprar ahora" }
                        if entitlements.trialDaysLeft > 0 { return "Empezar prueba gratuita" }
                        return "Suscribirse"
                    }()

                    Button {
                        guard let pkg = selectedPackage else { return }
                        Task { await entitlements.purchase(pkg) }
                    } label: {
                        ZStack {
                            if entitlements.isLoading {
                                ProgressView().tint(Color.wkNavy)
                            } else {
                                Text(ctaLabel)
                                    .font(.system(size: 17, weight: .heavy))
                                    .foregroundStyle(Color.wkNavy)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(selectedPackage != nil ? Color.wkYellow : Color.wkYellow.opacity(0.4))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(selectedPackage == nil || entitlements.isLoading)
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 12)

                    // ── Restaurar ──────────────────────────────────────────
                    Button {
                        Task { await entitlements.restore() }
                    } label: {
                        Text("Restaurar compra")
                            .font(.system(size: 13))
                            .foregroundStyle(.white.opacity(0.4))
                    }
                    .disabled(entitlements.isLoading)

                    if let err = entitlements.restoreError {
                        Text(err)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(red: 1, green: 0.42, blue: 0.42))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 28)
                            .padding(.top, 6)
                    }

                    Spacer().frame(height: 8)

                    Text("Cancela cuando quieras")
                        .font(.system(size: 11))
                        .foregroundStyle(.white.opacity(0.25))
                        .padding(.bottom, 32)
                }
            }
        }
        .interactiveDismissDisabled(entitlements.trialDaysLeft == 0)
        .onAppear {
            // Limpiar errores previos y preseleccionar anual por defecto
            entitlements.restoreError = nil
            if selectedPackage == nil {
                selectedPackage = annual ?? monthly ?? lifetime
            }
        }
        .onChange(of: entitlements.offerings) { _ in
            if selectedPackage == nil {
                selectedPackage = annual ?? monthly ?? lifetime
            }
        }
        // Auto-cerrar si la compra se completa durante esta sesión
        .onChange(of: entitlements.isPro) { nowPro in
            if nowPro && !didBecomeProDuringSession {
                didBecomeProDuringSession = true
                dismiss()
            }
        }
    }

    // MARK: - Helpers

    private var trialDaysLeft: Int { entitlements.trialDaysLeft }

    private func featureRow(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text(icon).font(.system(size: 15))
            Text(text)
                .font(.system(size: 14))
                .foregroundStyle(.white.opacity(0.85))
        }
    }
}

// MARK: - PlanCard

private struct PlanCard: View {
    let pkg:        Package
    let label:      String
    let badge:      String?
    let badgeColor: Color
    let trialText:  String?
    let isSelected: Bool
    let onTap:      () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // Radio
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(isSelected ? Color.wkYellow : .white.opacity(0.3))
                    .font(.system(size: 20))

                // Labels
                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                    if let trial = trialText {
                        Text(trial)
                            .font(.system(size: 11))
                            .foregroundStyle(Color.wkYellow.opacity(0.7))
                    }
                }

                Spacer()

                // Price + badge
                VStack(alignment: .trailing, spacing: 3) {
                    Text(pkg.storeProduct.localizedPriceString)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(isSelected ? Color.wkYellow : .white)
                    if let badge {
                        Text(badge)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(badgeColor)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(badgeColor.opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 14)
            .background(isSelected ? Color.wkYellow.opacity(0.08) : Color.white.opacity(0.04))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(isSelected ? Color.wkYellow : Color.white.opacity(0.12),
                            lineWidth: isSelected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}
