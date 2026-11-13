import SwiftUI
import UIKit

/// Message d'accueil du Chat IA : « Bonjour <Prénom> 👋 Je suis Pawrise, l'assistant de <chien>. Que souhaitez-vous
/// consulter ? ». Sans prénom connu, « Bonjour 👋 Je suis Pawrise… ».
enum ChatWelcome {
    static let wave = "👋"

    /// Texte avant l'émoji (« Bonjour Alex ») et après (« Je suis Pawrise… »).
    static func parts(ownerFirstName: String?, dogName: String) -> (greeting: String, body: String) {
        let name = ownerFirstName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return (
            name.isEmpty ? "Bonjour" : "Bonjour \(name)",
            "Je suis Pawrise, l'assistant de \(dogName). Que souhaitez-vous consulter ?"
        )
    }

    /// Message complet (lu par VoiceOver).
    static func message(ownerFirstName: String?, dogName: String) -> String {
        let parts = parts(ownerFirstName: ownerFirstName, dogName: dogName)
        return "\(parts.greeting) \(wave) \(parts.body)"
    }

    /// Message à afficher dans la bulle. L'émoji est rendu par la police système (Inter n'en a pas) ; si l'appareil
    /// ne sait pas dessiner les émojis en couleur (simulateur iOS 26 sur macOS 15 : carré « ? »), il est remplacé
    /// par une image (Noto Emoji U+1F44B, Google, licence Apache 2.0).
    @MainActor
    static func text(ownerFirstName: String?, dogName: String, fontSize: CGFloat) -> Text {
        let parts = parts(ownerFirstName: ownerFirstName, dogName: dogName)
        let emoji: Text
        if EmojiSupport.rendersColorEmoji {
            emoji = Text(verbatim: wave).font(.system(size: fontSize))
        } else {
            // Image tenue dans la hauteur des lettres d'Inter (ascendante + descendante) : l'interligne ne bouge pas.
            let side = (UIFontMetrics(forTextStyle: .body).scaledValue(for: fontSize) * 0.95).rounded()
            emoji = Text(EmojiSupport.waveImage(side: side)).baselineOffset(-side * 0.12)
        }
        return Text("\(parts.greeting) \(emoji) \(parts.body)")
            .accessibilityLabel(Text(verbatim: message(ownerFirstName: ownerFirstName, dogName: dogName)))
    }
}

/// Détecte si le système dessine réellement les émojis (Apple Color Emoji) : on rend 👋 dans une petite image et on
/// cherche des pixels colorés. Le glyphe de remplacement (carré « ? ») est noir sur blanc.
@MainActor
enum EmojiSupport {
    static let rendersColorEmoji: Bool = {
        let width = 32, height = 32
        var pixels = [UInt8](repeating: 255, count: width * height * 4) // RGBA, fond blanc
        let drawn: Bool = pixels.withUnsafeMutableBytes { buffer in
            guard let context = CGContext(
                data: buffer.baseAddress, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            ) else { return false }
            // Repère UIKit (origine en haut à gauche) pour dessiner la chaîne.
            context.translateBy(x: 0, y: CGFloat(height))
            context.scaleBy(x: 1, y: -1)
            UIGraphicsPushContext(context)
            NSAttributedString(string: ChatWelcome.wave, attributes: [.font: UIFont.systemFont(ofSize: 24)])
                .draw(at: CGPoint(x: 2, y: 2))
            UIGraphicsPopContext()
            return true
        }
        guard drawn else { return true }
        for index in stride(from: 0, to: pixels.count, by: 4) {
            let rgb = [pixels[index], pixels[index + 1], pixels[index + 2]].map(Int.init)
            if rgb.max()! - rgb.min()! > 60 { return true }
        }
        return false
    }()

    private static var waveImages: [CGFloat: Image] = [:]

    /// Image de 👋 à la taille voulue (en points), gardée en cache.
    static func waveImage(side: CGFloat) -> Image {
        if let cached = waveImages[side] { return cached }
        let size = CGSize(width: side, height: side)
        let source = UIImage(named: "EmojiWave")
        let rendered = UIGraphicsImageRenderer(size: size).image { _ in
            source?.draw(in: CGRect(origin: .zero, size: size))
        }
        let image = Image(uiImage: rendered).renderingMode(.original)
        waveImages[side] = image
        return image
    }
}
