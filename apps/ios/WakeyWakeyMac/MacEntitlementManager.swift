import Foundation
import RevenueCat

/// Acceso Pro = trial de 14 días desde la instalación OR suscripción/compra activa
/// (RevenueCat, entitlement "WakeyWakey Pro"). En el Mac de desarrollo siempre Pro.
@MainActor
final class MacEntitlementManager: ObservableObject {

    static let shared = MacEntitlementManager()

    static let entitlementID = "WakeyWakey Pro"
    static let trialDays     = 14
    // NOTA: clave pública de RevenueCat. Para producción, crear una app macOS en
    // RevenueCat y usar su clave (puede diferir de la de iOS).
    private static let apiKey = "appl_taYwunKuycUTotfBfjloXeSwBAT"

    @Published var isPro: Bool = true          // optimista hasta que responda RevenueCat
    @Published var trialDaysLeft: Int
    @Published var offerings: Offerings?
    @Published var isLoading = false
    @Published var restoreError: String?

    private var isRevenueCatPro = false
    private let installKey = "ww_install_date"

    private init() {
        let installed = Self.getOrSetInstallDate()
        let elapsed = Calendar.current.dateComponents([.day], from: installed, to: Date()).day ?? 0
        trialDaysLeft = max(0, Self.trialDays - elapsed)
    }

    func configure() {
        Purchases.logLevel = .error
        Purchases.configure(withAPIKey: Self.apiKey)
        refresh()
    }

    func refresh() {
        Task {
            do {
                let info = try await Purchases.shared.customerInfo()
                isRevenueCatPro = info.entitlements.active[Self.entitlementID] != nil
                updateIsPro()
                offerings = try await Purchases.shared.offerings()
            } catch { /* sin red: mantener estado del trial */ }
        }
    }

    func purchase(_ package: Package) async {
        isLoading = true; defer { isLoading = false }
        do {
            let result = try await Purchases.shared.purchase(package: package)
            isRevenueCatPro = result.customerInfo.entitlements.active[Self.entitlementID] != nil
            updateIsPro()
        } catch { /* cancelado o error */ }
    }

    func restore() async {
        isLoading = true; restoreError = nil; defer { isLoading = false }
        do {
            let info = try await Purchases.shared.restorePurchases()
            isRevenueCatPro = info.entitlements.active[Self.entitlementID] != nil
            updateIsPro()
            if !isRevenueCatPro { restoreError = "No previous purchase found for this Apple ID." }
        } catch { restoreError = "Couldn't reach the App Store. Try again." }
    }

    // MARK: - Debug (barra de dev): forzar estados de pago

    enum DebugTier { case trial, free, pro }
    @Published var debugTier: DebugTier?
    /// Día de trial simulado (1 = primer día). Cuando != nil, el estado Pro se calcula
    /// realmente a partir de los días (ignorando DevGate y debugTier), para probar el corte.
    @Published var debugTrialDay: Int?

    func debugSet(_ tier: DebugTier) {
        debugTrialDay = nil
        switch tier {
        case .trial:
            UserDefaults.standard.set(Date(), forKey: installKey)
            isRevenueCatPro = false
        case .free:
            UserDefaults.standard.set(Date().addingTimeInterval(-31 * 86_400), forKey: installKey)
            isRevenueCatPro = false
        case .pro:
            isRevenueCatPro = true
        }
        recomputeTrialDays()
        debugTier = tier
        updateIsPro()
    }

    /// Simula estar en el día `day` del trial. trialDaysLeft = trialDays - (day-1).
    /// day 14 → queda 1 día (Pro); day 15 → quedan 0 (Free). Rango 1…trialDays+2.
    func debugSetTrialDay(_ day: Int) {
        let d = max(1, min(day, Self.trialDays + 2))
        debugTrialDay = d
        debugTier = nil
        isRevenueCatPro = false
        trialDaysLeft = max(0, Self.trialDays - (d - 1))
        updateIsPro()
    }

    private func recomputeTrialDays() {
        let installed = (UserDefaults.standard.object(forKey: installKey) as? Date) ?? Date()
        let elapsed = Calendar.current.dateComponents([.day], from: installed, to: Date()).day ?? 0
        trialDaysLeft = max(0, Self.trialDays - elapsed)
    }

    private func updateIsPro() {
        // Modo "día de trial" (barra dev): calcula el estado real por días, ignorando todo lo demás.
        if debugTrialDay != nil { isPro = trialDaysLeft > 0; return }
        // El estado de debug (barra dev) tiene prioridad, para poder probar en el Mac de dev.
        if let t = debugTier { isPro = (t != .free); return }
        if DevGate.isDevMachine { isPro = true; return }   // tu Mac: siempre Pro
        isPro = trialDaysLeft > 0 || isRevenueCatPro
    }

    private static func getOrSetInstallDate() -> Date {
        let key = "ww_install_date"
        if let d = UserDefaults.standard.object(forKey: key) as? Date { return d }
        let now = Date(); UserDefaults.standard.set(now, forKey: key); return now
    }
}
