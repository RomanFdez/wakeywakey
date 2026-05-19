import SwiftUI
import EventKit

struct SettingsView: View {

    @EnvironmentObject var settings: SettingsStore
    @EnvironmentObject var calendarService: CalendarService
    @Environment(\.dismiss) private var dismiss

    private let minuteOptions = [0, 1, 2, 5, 10]
    private let rowBg = Color.white.opacity(0.07)

    var body: some View {
        NavigationStack {
            ZStack {
                Color.wkNavy.ignoresSafeArea()

                List {
                    generalSection
                    eventosSection
                    calendariosSection
                    otrosSection
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Ajustes")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Listo") { dismiss() }
                        .foregroundStyle(Color.wkYellow)
                        .fontWeight(.semibold)
                }
            }
        }
    }

    // MARK: - General

    private var generalSection: some View {
        Section {
            // Tiempo de alerta
            Picker(selection: $settings.alertMinutesBefore) {
                ForEach(minuteOptions, id: \.self) { min in
                    Text(minuteLabel(min)).tag(min)
                }
            } label: {
                Text("Avisar antes").foregroundStyle(.white)
            }
            .pickerStyle(.menu)
            .tint(Color.wkYellow)
            .listRowBackground(rowBg)

            wkToggle("Sonido",                   isOn: $settings.soundEnabled)
            wkToggle("Repetir hasta descartar",  isOn: $settings.repeatSoundUntilDismiss)
                .disabled(!settings.soundEnabled)
                .opacity(settings.soundEnabled ? 1 : 0.4)
            wkToggle("Vibración",                isOn: $settings.vibrationEnabled)
        } header: {
            sectionHeader("General")
        }
    }

    // MARK: - Eventos

    private var eventosSection: some View {
        Section {
            wkToggle("Solo videollamadas",          isOn: $settings.videoConferenceOnly)
            wkToggle("Solo eventos aceptados",      isOn: $settings.acceptedEventsOnly)
            wkToggle("Mostrar eventos de todo el día", isOn: $settings.showAllDayEvents)
            wkToggle("Solo horario laboral",        isOn: $settings.workingHoursOnly)

            if settings.workingHoursOnly {
                workingHoursPicker
                workingDaysPicker
            }
        } header: {
            sectionHeader("Eventos")
        }
    }

    private var workingHoursPicker: some View {
        Group {
            Picker(selection: $settings.workingHoursStart) {
                ForEach(0..<24, id: \.self) { h in Text(hourLabel(h)).tag(h) }
            } label: { Text("Desde").foregroundStyle(.white) }
            .pickerStyle(.menu).tint(Color.wkYellow).listRowBackground(rowBg)

            Picker(selection: $settings.workingHoursEnd) {
                ForEach(0..<24, id: \.self) { h in Text(hourLabel(h)).tag(h) }
            } label: { Text("Hasta").foregroundStyle(.white) }
            .pickerStyle(.menu).tint(Color.wkYellow).listRowBackground(rowBg)
        }
    }

    // Días: 2=L 3=M 4=X 5=J 6=V 7=S 1=D
    private let weekDays: [(Int, String)] = [(2,"L"),(3,"M"),(4,"X"),(5,"J"),(6,"V"),(7,"S"),(1,"D")]

    private var workingDaysPicker: some View {
        HStack(spacing: 6) {
            ForEach(weekDays, id: \.0) { (day, label) in
                let active = settings.workingDays.contains(day)
                Button {
                    if active { settings.workingDays.remove(day) }
                    else      { settings.workingDays.insert(day) }
                } label: {
                    Text(label)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(active ? Color.wkNavy : .white.opacity(0.4))
                        .frame(width: 36, height: 36)
                        .background(active ? Color.wkYellow : Color.white.opacity(0.08))
                        .clipShape(Circle())
                }
            }
        }
        .frame(maxWidth: .infinity)
        .listRowBackground(rowBg)
    }

    // MARK: - Calendarios

    private var calendarsByAccount: [(String, [EKCalendar])] {
        var dict: [(String, [EKCalendar])] = []
        var seen: [String: Int] = [:]
        for cal in calendarService.availableCalendars {
            let account = cal.source?.title ?? "Otro"
            if let idx = seen[account] {
                dict[idx].1.append(cal)
            } else {
                seen[account] = dict.count
                dict.append((account, [cal]))
            }
        }
        return dict
    }

    private var calendariosSection: some View {
        Section {
            if calendarService.availableCalendars.isEmpty {
                Text("Sin acceso al calendario")
                    .foregroundStyle(.white.opacity(0.4))
                    .font(.system(size: 14))
                    .listRowBackground(rowBg)
            } else {
                // Aviso si ninguno seleccionado
                if settings.enabledCalendarIds.isEmpty {
                    Label("Todos los calendarios activos", systemImage: "info.circle")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.wkYellow.opacity(0.8))
                        .listRowBackground(rowBg)
                }

                ForEach(calendarsByAccount, id: \.0) { (account, cals) in
                    // Cabecera de cuenta
                    Text(account.uppercased())
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(Color.wkYellow.opacity(0.7))
                        .kerning(1.2)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 4, trailing: 16))

                    ForEach(cals, id: \.calendarIdentifier) { cal in
                        Button { toggleCalendar(cal) } label: {
                            HStack(spacing: 12) {
                                Circle()
                                    .fill(Color(cgColor: cal.cgColor))
                                    .frame(width: 12, height: 12)
                                Text(cal.title)
                                    .foregroundStyle(.white)
                                Spacer()
                                if settings.enabledCalendarIds.contains(cal.calendarIdentifier) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.wkYellow)
                                        .fontWeight(.semibold)
                                }
                            }
                        }
                        .listRowBackground(rowBg)
                    }
                }
            }
        } header: {
            sectionHeader("Calendarios")
        }
    }

    // MARK: - Otros

    private var otrosSection: some View {
        Section {
            Button("Repetir tutorial") {
                settings.onboardingCompleted = false
                dismiss()
            }
            .foregroundStyle(Color.wkYellow)
            .listRowBackground(rowBg)
        } header: {
            sectionHeader("Otros")
        }
    }

    // MARK: - Helpers

    private func wkToggle(_ title: String, isOn: Binding<Bool>) -> some View {
        Toggle(title, isOn: isOn)
            .toggleStyle(WKToggleStyle())
            .foregroundStyle(.white)
            .listRowBackground(rowBg)
    }

    private func toggleCalendar(_ cal: EKCalendar) {
        let id = cal.calendarIdentifier
        if settings.enabledCalendarIds.contains(id) {
            settings.enabledCalendarIds.remove(id)
        } else {
            settings.enabledCalendarIds.insert(id)
        }
        calendarService.loadTodayEvents(enabledIds: settings.enabledCalendarIds, settings: settings)
    }

    private func minuteLabel(_ min: Int) -> String {
        switch min {
        case 0:  return "Al empezar"
        case 1:  return "1 minuto antes"
        default: return "\(min) minutos antes"
        }
    }

    private func hourLabel(_ h: Int) -> String {
        String(format: "%02d:00", h)
    }

    private func sectionHeader(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(.white.opacity(0.5))
            .kerning(1.1)
    }
}
