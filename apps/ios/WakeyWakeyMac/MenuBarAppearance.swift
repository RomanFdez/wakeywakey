import AppKit
import SwiftUI

/// Colores disponibles para el texto del icono de la barra de menú.
enum MenuBarColor {
    static let all: [(id: String, ns: NSColor, color: Color)] = [
        ("auto",   .labelColor, .primary),   // adaptativo (visible en barra clara u oscura)
        ("white",  .white,                                          .white),
        ("yellow", NSColor(red: 1, green: 0.878, blue: 0.227, alpha: 1), Color(red: 1, green: 0.878, blue: 0.227)),
        ("red",    .systemRed,    .red),
        ("blue",   .systemBlue,   .blue),
        ("green",  .systemGreen,  .green),
        ("orange", .systemOrange, .orange),
        ("purple", .systemPurple, .purple),
    ]
    static func ns(_ id: String) -> NSColor { all.first { $0.id == id }?.ns ?? .white }
    static func color(_ id: String) -> Color { all.first { $0.id == id }?.color ?? .white }
}
