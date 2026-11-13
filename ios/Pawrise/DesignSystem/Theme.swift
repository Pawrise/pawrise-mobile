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

/// Polices du Figma « Pawrise-App » : Bricolage Grotesque (titres, opsz 14), Inter (texte), DM Mono (valeurs).
/// Les fichiers sont dans Resources/Fonts (licence OFL) et déclarés dans Info.plist (UIAppFonts).
/// Les tailles sont celles de la maquette ; elles suivent Dynamic Type à partir de la taille par défaut.
extension Font {
    enum Pawrise {
        /// Graisses disponibles en fichier (les autres retombent sur la plus proche).
        enum Weight { case regular, medium, semibold, bold, extraBold }

        /// Bricolage Grotesque Bold (titres d'écran 26, cartes 20/18, marque 32).
        static func display(_ size: CGFloat, _ weight: Weight = .bold, relativeTo style: Font.TextStyle = .title) -> Font {
            let name = switch weight {
            case .extraBold: "BricolageGrotesque-14ptExtraBold"
            case .semibold, .medium, .regular: "BricolageGrotesque-14ptSemiBold"
            case .bold: "BricolageGrotesque-14ptBold"
            }
            return font(name, size, style, weight: weight.system)
        }

        /// Inter (textes, libellés, boutons).
        static func inter(_ size: CGFloat, _ weight: Weight = .regular, relativeTo style: Font.TextStyle = .body) -> Font {
            let name = switch weight {
            case .regular: "Inter-Regular"
            case .medium: "Inter-Medium"
            case .semibold: "Inter-SemiBold"
            case .bold, .extraBold: "Inter-Bold"
            }
            return font(name, size, style, weight: weight.system)
        }

        /// DM Mono Medium (valeurs mesurées : 87, 78 bpm, PW-ABC123…).
        static func mono(_ size: CGFloat = 20, relativeTo style: Font.TextStyle = .title3) -> Font {
            font("DMMono-Medium", size, style, weight: .medium, design: .monospaced)
        }

        // Rôles récurrents de la maquette.
        /// Titre d'écran (« Créer un compte », « Comment s’appelle votre chien ? ») : Bricolage 700 26.
        static let largeTitle = display(26, relativeTo: .largeTitle)
        static let title = display(24, relativeTo: .title)
        static let title2 = display(20, relativeTo: .title2)
        /// Texte d'accompagnement sous un titre : Inter 400 14.
        static let subheadline = inter(14, relativeTo: .subheadline)
        /// Texte courant et saisie : Inter 400 15.
        static let body = inter(15)
        static let callout = inter(15, relativeTo: .callout)
        static let headline = inter(15, .semibold, relativeTo: .headline)
        /// Libellé de champ / petite mention en gras : Inter 600 12.
        static let footnote = inter(12, .semibold, relativeTo: .footnote)
        static let caption = inter(12, .semibold, relativeTo: .caption)
        /// Bouton plein ou blanc : Inter 700 15.
        static let button = inter(15, .bold, relativeTo: .headline)
        /// Lien texte (« Créer un compte », « Retour ») : Inter 600 14.
        static let link = inter(14, .semibold, relativeTo: .subheadline)

        private static func font(
            _ name: String, _ size: CGFloat, _ style: Font.TextStyle,
            weight: Font.Weight, design: Font.Design = .default
        ) -> Font {
            // Bricolage : la taille optique suit la taille du texte, comme dans Figma (police variable).
            if name.hasPrefix("BricolageGrotesque"), let base = PawriseUIFont.make(name, size: size) {
                return Font(UIFontMetrics(forTextStyle: style.uiKit).scaledFont(for: base))
            }
            if UIFont(name: name, size: size) != nil {
                // `.custom(_:size:relativeTo:)` arrondit les tailles fractionnaires (13,5 → 14) : pour celles-là,
                // Dynamic Type est appliqué ici et la taille exacte de la maquette est conservée.
                if size.rounded() != size {
                    return .custom(name, fixedSize: UIFontMetrics(forTextStyle: style.uiKit).scaledValue(for: size))
                }
                return .custom(name, size: size, relativeTo: style)
            }
            let scaled = UIFontMetrics(forTextStyle: style.uiKit).scaledValue(for: size)
            return .system(size: scaled, weight: weight, design: design)
        }
    }
}

private extension Font.Pawrise.Weight {
    var system: Font.Weight {
        switch self {
        case .regular: .regular
        case .medium: .medium
        case .semibold: .semibold
        case .bold: .bold
        case .extraBold: .heavy
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

// MARK: - Interligne Figma

/// Familles de police et leur interligne naturel (ascendante + descendante / em, mesuré dans les fichiers).
enum FigmaFontFamily {
    case display, inter, mono

    var naturalLineHeight: CGFloat {
        switch self {
        case .display: 1.2
        case .inter: 1.21
        case .mono: 1.302
        }
    }
}

/// Reproduit un interligne Figma (« lh=135 % ») : espace entre les lignes et demi-interligne
/// au-dessus et au-dessous de la boîte de texte, comme Figma. Suit Dynamic Type.
private struct FigmaLineHeight: ViewModifier {
    @ScaledMetric private var extra: CGFloat

    init(size: CGFloat, multiple: CGFloat, family: FigmaFontFamily, relativeTo style: Font.TextStyle) {
        _extra = ScaledMetric(wrappedValue: max(0, (multiple - family.naturalLineHeight) * size), relativeTo: style)
    }

    func body(content: Content) -> some View {
        content
            .lineSpacing(extra)
            .padding(.vertical, extra / 2)
    }
}

extension View {
    /// Interligne Figma : `figmaLineHeight(1.35, size: 15)` pour Inter 15 à 135 %.
    func figmaLineHeight(
        _ multiple: CGFloat, size: CGFloat, family: FigmaFontFamily = .inter, relativeTo style: Font.TextStyle = .body
    ) -> some View {
        modifier(FigmaLineHeight(size: size, multiple: multiple, family: family, relativeTo: style))
    }
}


/// Fabrique UIKit des polices de la maquette.
/// Bricolage Grotesque est une police variable (opsz 12–96, wght 200–800) : Figma règle la taille optique
/// automatiquement sur la taille du texte, ce qui rend les titres plus étroits et plus fins qu'avec une seule
/// instance « 14 pt ». On reproduit ce réglage ; les noms « BricolageGrotesque-14ptBold » etc. restent les clés.
enum PawriseUIFont {
    private static let variableName = "BricolageGrotesque-96ptExtraBold"
    private static let opsz = 0x6F70_737A, wght = 0x7767_6874, wdth = 0x7764_7468

    static func make(_ name: String, size: CGFloat) -> UIFont? {
        guard name.hasPrefix("BricolageGrotesque") else { return UIFont(name: name, size: size) }
        let weight: CGFloat = name.hasSuffix("ExtraBold") ? 800 : name.hasSuffix("SemiBold") ? 600 : 700
        guard let base = UIFont(name: variableName, size: size) else { return UIFont(name: name, size: size) }
        let axes: [Int: CGFloat] = [opsz: min(max(size, 12), 96), wght: weight, wdth: 100]
        let descriptor = base.fontDescriptor.addingAttributes([
            UIFontDescriptor.AttributeName(rawValue: kCTFontVariationAttribute as String): axes,
        ])
        return UIFont(descriptor: descriptor, size: size)
    }

    /// ♀ et ♂ : Inter ne les contient pas ; Figma les dessine avec Noto Sans Symbols (sous-ensemble embarqué).
    static func symbols(size: CGFloat) -> UIFont {
        UIFont(name: "NotoSansSymbols-Regular", size: size) ?? .systemFont(ofSize: size)
    }
}
