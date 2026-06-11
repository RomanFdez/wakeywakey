import AVFoundation
import AudioToolbox

struct AlertSound: Identifiable, Equatable {
    let id: String       // filename without extension, or "default"
    let displayName: String

    var filename: String? { id == "default" ? nil : "\(id).mp3" }

    static let defaultName = "clock-alarm"

    static let all: [AlertSound] = [
        AlertSound(id: "default",          displayName: "Sistema"),
        AlertSound(id: "clock-alarm",      displayName: "Despertador"),
        AlertSound(id: "service-bell",     displayName: "Campana"),
        AlertSound(id: "call-to-attention",displayName: "Atención"),
        AlertSound(id: "boxing-ring",      displayName: "Boxeo"),
        AlertSound(id: "coin",             displayName: "Moneda"),
        AlertSound(id: "level-up",         displayName: "Level up"),
        AlertSound(id: "metal-spring",     displayName: "Resorte"),
        AlertSound(id: "notification-1",   displayName: "Notificación 1"),
        AlertSound(id: "notification-2",   displayName: "Notificación 2"),
        AlertSound(id: "notification-3",   displayName: "Notificación 3"),
        AlertSound(id: "notification-4",   displayName: "Notificación 4"),
        AlertSound(id: "notification-5",   displayName: "Notificación 5"),
        AlertSound(id: "punch",            displayName: "Golpe"),
        AlertSound(id: "referee-whistle",  displayName: "Pito árbitro"),
        AlertSound(id: "whistle",          displayName: "Silbato"),
    ]
}

// MARK: - Preview player

@MainActor
class SoundPreviewPlayer: ObservableObject {
    private var player: AVAudioPlayer?

    func play(_ sound: AlertSound) {
        player?.stop()
        guard let filename = sound.filename,
              let url = Bundle.main.url(forResource: sound.id, withExtension: "mp3")
        else {
            // "Sistema" — play a short system sound via AudioServicesPlaySystemSound
            AudioServicesPlaySystemSound(1057)
            return
        }
        _ = filename  // suppress warning
        player = try? AVAudioPlayer(contentsOf: url)
        player?.play()
    }

    func stop() { player?.stop() }
}
