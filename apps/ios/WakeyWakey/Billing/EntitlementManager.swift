import Foundation
import RevenueCat
import UserNotifications

/// Gestiona el acceso Pro combinando dos fuentes:
///  1. Trial de 14 días desde la primera instalación (sin pedir nada)
///  2. Suscripción o compra única via RevenueCat / App Store
///
/// `isPro` = trial activo OR suscripción/compra activa
@MainActor
class EntitlementManager: ObservableObject {

    static let shared = EntitlementManager()

    static let entitlementID = "WakeyWakey Pro"
    static let trialDays     = 14

    private let installDateKey = "ww_install_date"

    // MARK: - Estado

    @Published var isPro:         Bool = true   // optimista hasta que RevenueCat responda
    @Published var trialDaysLeft: Int
    @Published var offerings:     Offerings?
    @Published var isLoading:     Bool = false
    @Published var restoreError:  String?

    private var isRevenueCatPro = false

    // MARK: - Init

    init() {
        let installDate = Self.getOrSetInstallDate()
        let elapsed     = Calendar.current.dateComponents([.day], from: installDate, to: Date()).day ?? 0
        trialDaysLeft   = max(0, Self.trialDays - elapsed)
        isPro           = trialDaysLeft > 0   // actualizado cuando RevenueCat responda
    }

    // MARK: - Configurar RevenueCat (llamar en App.init)

    func configure() {
        Purchases.logLevel = .error
        Purchases.configure(withAPIKey: "appl_taYwunKuycUTotfBfjloXeSwBAT")
        refresh()
    }

    // MARK: - Refresh desde RevenueCat

    func refresh() {
        Task {
            do {
                let info        = try await Purchases.shared.customerInfo()
                isRevenueCatPro = info.entitlements.active[Self.entitlementID] != nil
                updateIsPro()

                let off = try await Purchases.shared.offerings()
                offerings = off
            } catch {
                // Sin red: mantenemos el estado del trial
            }
        }
    }

    // MARK: - Compra

    func purchase(_ package: Package) async {
        isLoading = true
        defer { isLoading = false }
        do {
            let result      = try await Purchases.shared.purchase(package: package)
            isRevenueCatPro = result.customerInfo.entitlements.active[Self.entitlementID] != nil
            updateIsPro()
        } catch {
            // El usuario canceló o hubo error — no hacer nada
        }
    }

    // MARK: - Restaurar

    func restore() async {
        isLoading    = true
        restoreError = nil
        defer { isLoading = false }
        do {
            let info        = try await Purchases.shared.restorePurchases()
            isRevenueCatPro = info.entitlements.active[Self.entitlementID] != nil
            updateIsPro()
            if !isRevenueCatPro {
                restoreError = "No se encontró ninguna compra anterior asociada a este Apple ID."
            }
        } catch {
            restoreError = "No se pudo conectar con App Store. Inténtalo de nuevo."
        }
    }

    // MARK: - Helpers

    private func updateIsPro() {
        let wasProBefore = isPro
        #if DEBUG
        isPro = true
        return
        #endif
        isPro = trialDaysLeft > 0 || isRevenueCatPro

        if wasProBefore && !isPro {
            // Trial expirado o suscripción cancelada → cancelar notificaciones y limpiar widget
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
            WidgetDataWriter.clear()
        } else if !wasProBefore && isPro {
            // Acaba de suscribirse o restaurar → reprogramar notificaciones
            let settings = SettingsStore.shared
            let cal      = CalendarService.shared
            Task {
                await AlertScheduler.shared.rescheduleAll(
                    events:        cal.todayEvents,
                    minutesBefore: settings.alertMinutesBefore,
                    soundName:     settings.alertSoundName
                )
            }
        }
    }

    private static func getOrSetInstallDate() -> Date {
        let key = "ww_install_date"
        if let stored = UserDefaults.standard.object(forKey: key) as? Date {
            return stored
        }
        let now = Date()
        UserDefaults.standard.set(now, forKey: key)
        return now
    }

    // MARK: - Debug

    #if DEBUG
    func debugSetTrialDays(_ days: Int) {
        trialDaysLeft = days
        updateIsPro()
    }
    #endif
}
