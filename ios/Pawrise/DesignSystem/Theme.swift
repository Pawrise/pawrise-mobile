import SwiftUI

// Jetons de design repris de pawrise-figma/src/theme/tokens.json (source : Figma « Pawrise App »).

extension Color {
    enum Pawrise {
        static let ink = Color(hex: 0x123044)
        static let primary = Color(hex: 0x0A5C78)
        static let onPrimary = Color.white
        static let teal = Color(hex: 0x14A3A8)

        static let page = Color(hex: 0xF4F8FB)
        static let card = Color.white
        static let elevated = Color(hex: 0xE5F1F7)
        static let secondarySurface = Color(hex: 0xEEF5F8)
        static let mutedSurface = Color(hex: 0xF3F7FA)
        static let avatarBackdrop = Color(hex: 0xE4F0F6)

        static let textPrimary = ink
        static let textMuted = Color(hex: 0x5A7082)

        static let good = Color(hex: 0x0E7C74)
        static let goodSurface = Color(hex: 0xE4F6F4)
        static let watch = Color(hex: 0x8A5A00)
        static let watchSurface = Color(hex: 0xFFF4D8)
        static let alert = Color(hex: 0xB4233A)
        static let alertSurface = Color(hex: 0xFDECEE)

        static let border = Color(hex: 0xD5E3EC)
        static let borderStrong = Color(hex: 0xC1D4E0)

        static func tint(for state: HealthState) -> Color {
            switch state {
            case .good: good
            case .watch: watch
            case .alert: alert
            }
        }

        static func surface(for state: HealthState) -> Color {
            switch state {
            case .good: goodSurface
            case .watch: watchSurface
            case .alert: alertSurface
            }
        }
    }

    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

/// Polices du Figma : Bricolage Grotesque (titres), Inter (texte), DM Mono (valeurs).
/// Tant que les fichiers .ttf ne sont pas ajoutés (Resources/Fonts + clé UIAppFonts),
/// on utilise la police système avec la même taille et la même graisse.
extension Font {
    enum Pawrise {
        static func display(_ size: CGFloat, relativeTo style: Font.TextStyle = .largeTitle) -> Font {
            font("BricolageGrotesque-Bold", size, style, weight: .bold)
        }

        static let largeTitle = display(28)
        static let title = display(24, relativeTo: .title)
        static let title2 = display(20, relativeTo: .title2)
        static let headline = font("Inter-SemiBold", 17, .headline, weight: .semibold)
        static let body = font("Inter-Regular", 16, .body, weight: .regular)
        static let callout = font("Inter-Regular", 15, .callout, weight: .regular)
        static let subheadline = font("Inter-Regular", 14, .subheadline, weight: .regular)
        static let footnote = font("Inter-Medium", 13, .footnote, weight: .medium)
        static let caption = font("Inter-Medium", 12, .caption, weight: .medium)
        static let button = font("Inter-SemiBold", 17, .headline, weight: .semibold)
        static func mono(_ size: CGFloat = 20) -> Font {
            font("DMMono-Medium", size, .title3, weight: .medium, design: .monospaced)
        }

        private static func font(
            _ name: String, _ size: CGFloat, _ style: Font.TextStyle,
            weight: Font.Weight, design: Font.Design = .default
        ) -> Font {
            if UIFont(name: name, size: size) != nil {
                return .custom(name, size: size, relativeTo: style)
            }
            let scaled = UIFontMetrics(forTextStyle: style.uiKit).scaledValue(for: size)
            return .system(size: scaled, weight: weight, design: design)
        }
    }
}

private extension Font.TextStyle {
    var uiKit: UIFont.TextStyle {
        switch self {
        case .largeTitle: .largeTitle
        case .title: .title1
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .callout: .callout
        case .footnote: .footnote
        case .caption: .caption1
        case .caption2: .caption2
        default: .body
        }
    }
}

enum Radius {
    static let card: CGFloat = 28
    static let tile: CGFloat = 20
    static let control: CGFloat = 16
}

enum Spacing {
    static let page: CGFloat = 20
    static let section: CGFloat = 24
}
