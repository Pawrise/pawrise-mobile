import SwiftUI

// Icônes et réglages typographiques des écrans Santé, Chat IA et Paramètres (équipe B).
// Fichier séparé de Theme.swift / Components.swift : il n'y ajoute que des éléments nouveaux.

// MARK: - Icônes du Figma

/// Icônes de la maquette « Pawrise-App » (grille 24 × 24, trait 1,9 mis à l'échelle), dessinées avec Lucide :
/// les tracés exportés de Figma sont déformés (traits vectorisés). Même source que l'app Android.
enum PawriseGlyph: Sendable {
    case map, chat, settings, doc, heart, pulse, activity, moon, thermometer, battery
    case alert, alertCircle, swap, close, check, signal, shield, send, paw, sparkles, brand
    case user, lock, chevronRight, trash, logout, eye, eyeOff

    /// Tracés Lucide (LucidePaths.swift, généré) ; `brand` (logo) et `sparkles` (✦ plein) restent dessinés ici.
    fileprivate var paths: [String] {
        switch self {
        case .map: LucidePaths.map
        case .chat: LucidePaths.chat
        case .settings: LucidePaths.settings
        case .doc: LucidePaths.doc
        case .heart: LucidePaths.heart
        case .pulse: LucidePaths.pulse
        case .activity: LucidePaths.activity
        case .moon: LucidePaths.moon
        case .thermometer: LucidePaths.thermometer
        case .battery: LucidePaths.battery
        case .alert: LucidePaths.alert
        case .alertCircle: LucidePaths.alertCircle
        case .swap: LucidePaths.swap
        case .close: LucidePaths.close
        case .check: LucidePaths.check
        case .signal: LucidePaths.signal
        case .shield: LucidePaths.shield
        case .send: LucidePaths.send
        case .paw: LucidePaths.paw
        case .sparkles: ["M12 3l1.8 4.7L18.5 9l-4.7 1.8L12 15l-1.8-4.2L5.5 9l4.7-1.3L12 3ZM19 15l.9 2.3 2.1.7-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.7L19 15Z"]
        case .user: LucidePaths.user
        case .lock: LucidePaths.lock
        case .chevronRight: LucidePaths.chevronRight
        case .trash: LucidePaths.trash
        case .logout: LucidePaths.logout
        case .eye: LucidePaths.eye
        case .eyeOff: LucidePaths.eyeOff
        case .brand: []
        }
    }

    fileprivate var isFilled: Bool { self == .brand || self == .sparkles }

}

/// Forme d'une icône, dessinée dans le carré `rect`.
struct PawriseGlyphShape: Shape {
    let glyph: PawriseGlyph

    func path(in rect: CGRect) -> Path {
        let side = min(rect.width, rect.height)
        if glyph == .brand {
            // Fleur du logo : huit pétales elliptiques (viewBox 64, cx 32, cy 13, rx 6,5, ry 11).
            let scale = side / 64
            var path = Path()
            for index in 0..<8 {
                let petal = Path(ellipseIn: CGRect(x: 25.5, y: 2, width: 13, height: 22))
                let rotation = CGAffineTransform(translationX: 32, y: 32)
                    .rotated(by: Double(index) * .pi / 4)
                    .translatedBy(x: -32, y: -32)
                path.addPath(petal, transform: rotation)
            }
            return path.applying(CGAffineTransform(translationX: rect.minX, y: rect.minY).scaledBy(x: scale, y: scale))
        }
        let scale = side / 24
        let transform = CGAffineTransform(translationX: rect.minX, y: rect.minY).scaledBy(x: scale, y: scale)
        var path = Path()
        for data in glyph.paths {
            path.addPath(SVGPathParser.path(data), transform: transform)
        }
        return path
    }
}

/// Icône Pawrise à la taille de la maquette (le trait suit la taille : 1,9 pour 24 pt).
struct PawriseIcon: View {
    let glyph: PawriseGlyph
    var size: CGFloat = 24
    var lineWidth: CGFloat = 1.9

    init(_ glyph: PawriseGlyph, size: CGFloat = 24, lineWidth: CGFloat = 1.9) {
        self.glyph = glyph
        self.size = size
        self.lineWidth = lineWidth
    }

    var body: some View {
        Group {
            if glyph.isFilled {
                PawriseGlyphShape(glyph: glyph).fill()
            } else {
                PawriseGlyphShape(glyph: glyph)
                    .stroke(style: StrokeStyle(lineWidth: lineWidth * size / 24, lineCap: .round, lineJoin: .round))
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

// MARK: - Lecture des tracés SVG

/// Lit l'attribut `d` d'un tracé SVG (M L H V C S Q A Z, absolus et relatifs).
enum SVGPathParser {
    static func path(_ data: String) -> Path {
        var scanner = Scanner(data)
        var path = Path()
        var command: Character = "M"
        var current = CGPoint.zero
        var start = CGPoint.zero
        var lastControl: CGPoint?

        while !scanner.isAtEnd {
            if let next = scanner.command() {
                command = next
            }
            let relative = command.isLowercase
            let origin = relative ? current : .zero
            func point(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: origin.x + x, y: origin.y + y) }
            let before = scanner.position

            switch command.uppercased() {
            case "Z":
                path.closeSubpath()
                current = start
                lastControl = nil
                continue
            case "M":
                guard let x = scanner.number(), let y = scanner.number() else { break }
                current = point(x, y)
                start = current
                path.move(to: current)
                command = relative ? "l" : "L"
                lastControl = nil
            case "L":
                guard let x = scanner.number(), let y = scanner.number() else { break }
                current = point(x, y)
                path.addLine(to: current)
                lastControl = nil
            case "H":
                guard let x = scanner.number() else { break }
                current = CGPoint(x: relative ? current.x + x : x, y: current.y)
                path.addLine(to: current)
                lastControl = nil
            case "V":
                guard let y = scanner.number() else { break }
                current = CGPoint(x: current.x, y: relative ? current.y + y : y)
                path.addLine(to: current)
                lastControl = nil
            case "C":
                guard let x1 = scanner.number(), let y1 = scanner.number(),
                      let x2 = scanner.number(), let y2 = scanner.number(),
                      let x = scanner.number(), let y = scanner.number() else { break }
                let control2 = point(x2, y2)
                current = point(x, y)
                path.addCurve(to: current, control1: point(x1, y1), control2: control2)
                lastControl = control2
            case "S":
                guard let x2 = scanner.number(), let y2 = scanner.number(),
                      let x = scanner.number(), let y = scanner.number() else { break }
                let control1 = lastControl.map { CGPoint(x: 2 * current.x - $0.x, y: 2 * current.y - $0.y) } ?? current
                let control2 = point(x2, y2)
                current = point(x, y)
                path.addCurve(to: current, control1: control1, control2: control2)
                lastControl = control2
            case "Q":
                guard let x1 = scanner.number(), let y1 = scanner.number(),
                      let x = scanner.number(), let y = scanner.number() else { break }
                current = point(x, y)
                path.addQuadCurve(to: current, control: point(x1, y1))
                lastControl = nil
            case "A":
                guard let rx = scanner.number(), let ry = scanner.number(), let angle = scanner.number(),
                      let large = scanner.flag(), let sweep = scanner.flag(),
                      let x = scanner.number(), let y = scanner.number() else { break }
                let end = point(x, y)
                addArc(to: &path, from: current, to: end, rx: rx, ry: ry, angle: angle, large: large, sweep: sweep)
                current = end
                lastControl = nil
            default:
                break
            }
            // Données illisibles : on s'arrête plutôt que de boucler.
            if scanner.position == before { break }
        }
        return path
    }

    /// Arc elliptique SVG converti en courbes de Bézier (au plus un quart de tour par courbe).
    private static func addArc(
        to path: inout Path, from p0: CGPoint, to p1: CGPoint,
        rx rxIn: CGFloat, ry ryIn: CGFloat, angle: CGFloat, large: Bool, sweep: Bool
    ) {
        guard p0 != p1 else { return }
        var rx = abs(rxIn), ry = abs(ryIn)
        guard rx > 0, ry > 0 else {
            path.addLine(to: p1)
            return
        }
        let phi = angle * .pi / 180
        let cosPhi = cos(phi), sinPhi = sin(phi)
        let dx = (p0.x - p1.x) / 2, dy = (p0.y - p1.y) / 2
        let x1 = cosPhi * dx + sinPhi * dy
        let y1 = -sinPhi * dx + cosPhi * dy
        let lambda = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry)
        if lambda > 1 {
            rx *= sqrt(lambda)
            ry *= sqrt(lambda)
        }
        let numerator = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1
        let denominator = rx * rx * y1 * y1 + ry * ry * x1 * x1
        var coefficient = denominator == 0 ? 0 : sqrt(max(0, numerator / denominator))
        if large == sweep { coefficient = -coefficient }
        let cxp = coefficient * rx * y1 / ry
        let cyp = -coefficient * ry * x1 / rx
        let cx = cosPhi * cxp - sinPhi * cyp + (p0.x + p1.x) / 2
        let cy = sinPhi * cxp + cosPhi * cyp + (p0.y + p1.y) / 2

        func angleBetween(_ ux: CGFloat, _ uy: CGFloat, _ vx: CGFloat, _ vy: CGFloat) -> CGFloat {
            atan2(ux * vy - uy * vx, ux * vx + uy * vy)
        }
        let ux = (x1 - cxp) / rx, uy = (y1 - cyp) / ry
        let vx = (-x1 - cxp) / rx, vy = (-y1 - cyp) / ry
        let theta = angleBetween(1, 0, ux, uy)
        var delta = angleBetween(ux, uy, vx, vy)
        if !sweep, delta > 0 { delta -= 2 * .pi } else if sweep, delta < 0 { delta += 2 * .pi }

        let segments = max(1, Int(ceil(abs(delta) / (.pi / 2) - 0.001)))
        let step = delta / CGFloat(segments)
        let t = 4 / 3 * tan(step / 4)
        func map(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
            CGPoint(x: cx + rx * x * cosPhi - ry * y * sinPhi, y: cy + rx * x * sinPhi + ry * y * cosPhi)
        }
        var a = theta
        for _ in 0..<segments {
            let b = a + step
            path.addCurve(
                to: map(cos(b), sin(b)),
                control1: map(cos(a) - t * sin(a), sin(a) + t * cos(a)),
                control2: map(cos(b) + t * sin(b), sin(b) - t * cos(b))
            )
            a = b
        }
    }

    private struct Scanner {
        private let characters: [Character]
        private(set) var position = 0

        init(_ text: String) { characters = Array(text) }

        var isAtEnd: Bool {
            mutating get {
                skipSeparators()
                return position >= characters.count
            }
        }

        private mutating func skipSeparators() {
            while position < characters.count, characters[position] == " " || characters[position] == ","
                || characters[position] == "\n" || characters[position] == "\t" {
                position += 1
            }
        }

        mutating func command() -> Character? {
            skipSeparators()
            guard position < characters.count, characters[position].isLetter else { return nil }
            defer { position += 1 }
            return characters[position]
        }

        mutating func flag() -> Bool? {
            skipSeparators()
            guard position < characters.count, characters[position] == "0" || characters[position] == "1" else { return nil }
            defer { position += 1 }
            return characters[position] == "1"
        }

        mutating func number() -> CGFloat? {
            skipSeparators()
            let begin = position
            if position < characters.count, characters[position] == "-" || characters[position] == "+" { position += 1 }
            var hasDigits = false
            var hasDot = false
            while position < characters.count {
                let character = characters[position]
                if character.isASCII, character.isNumber {
                    hasDigits = true
                } else if character == ".", !hasDot {
                    hasDot = true
                } else {
                    break
                }
                position += 1
            }
            if hasDigits, position < characters.count, characters[position] == "e" || characters[position] == "E" {
                position += 1
                if position < characters.count, characters[position] == "-" || characters[position] == "+" { position += 1 }
                while position < characters.count, characters[position].isASCII, characters[position].isNumber { position += 1 }
            }
            guard hasDigits, let value = Double(String(characters[begin..<position])) else {
                position = begin
                return nil
            }
            return CGFloat(value)
        }
    }
}

// MARK: - Texte à la hauteur de ligne de la maquette

/// Familles de polices de la maquette (mêmes fichiers que `Font.Pawrise`).
enum PawriseFontFamily {
    case inter, display, mono

    func postScriptName(_ weight: Font.Pawrise.Weight) -> String {
        switch self {
        case .inter:
            switch weight {
            case .regular: "Inter-Regular"
            case .medium: "Inter-Medium"
            case .semibold: "Inter-SemiBold"
            case .bold, .extraBold: "Inter-Bold"
            }
        case .display:
            switch weight {
            case .extraBold: "BricolageGrotesque-14ptExtraBold"
            case .bold: "BricolageGrotesque-14ptBold"
            default: "BricolageGrotesque-14ptSemiBold"
            }
        case .mono:
            "DMMono-Medium"
        }
    }
}

extension View {
    /// Police de la maquette avec sa hauteur de ligne (ex. Inter 400 11 · 140 %) : l'interligne et la
    /// marge verticale reproduisent la boîte de texte Figma, pour que les espacements coïncident.
    func pawriseFont(
        _ family: PawriseFontFamily, _ size: CGFloat, _ weight: Font.Pawrise.Weight = .regular,
        lineHeight multiple: CGFloat? = nil, relativeTo style: Font.TextStyle = .body
    ) -> some View {
        let name = family.postScriptName(weight)
        let font: Font = switch family {
        case .inter: Font.Pawrise.inter(size, weight, relativeTo: style)
        case .display: Font.Pawrise.display(size, weight, relativeTo: style)
        case .mono: Font.Pawrise.mono(size, relativeTo: style)
        }
        let natural = PawriseUIFont.make(name, size: size)?.lineHeight ?? size * 1.2
        let extra = multiple.map { max(0, size * $0 - natural) } ?? 0
        return self
            .font(font)
            .lineSpacing(extra)
            .padding(.vertical, extra / 2)
    }
}

// MARK: - Texte à coupure « gloutonne »

/// Texte multiligne coupé comme dans Figma : chaque ligne prend autant de mots que possible.
/// `Text` applique la stratégie de coupure d'iOS qui évite un mot seul en dernière ligne (« Peu / de réveils. »
/// au lieu de « Peu de / réveils. ») ; ce `UILabel` la désactive. Suit Dynamic Type.
struct MaquetteText: UIViewRepresentable {
    let text: String
    var family: PawriseFontFamily = .inter
    var size: CGFloat
    var weight: Font.Pawrise.Weight = .regular
    var lineHeight: CGFloat?
    var color: Color
    var relativeTo: UIFont.TextStyle = .body

    func makeUIView(context: Context) -> UILabel {
        let label = UILabel()
        label.numberOfLines = 0
        label.lineBreakStrategy = []
        label.adjustsFontForContentSizeCategory = false
        label.setContentHuggingPriority(.required, for: .vertical)
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return label
    }

    func updateUIView(_ label: UILabel, context: Context) {
        let metrics = UIFontMetrics(forTextStyle: relativeTo)
        let traits = UITraitCollection(preferredContentSizeCategory: UIContentSizeCategory(context.environment.dynamicTypeSize))
        let base = PawriseUIFont.make(family.postScriptName(weight), size: size) ?? .systemFont(ofSize: size)
        let font = metrics.scaledFont(for: base, compatibleWith: traits)
        let paragraph = NSMutableParagraphStyle()
        paragraph.lineBreakStrategy = []
        paragraph.lineBreakMode = .byWordWrapping
        var baselineOffset: CGFloat = 0
        if let lineHeight {
            let height = metrics.scaledValue(for: size * lineHeight, compatibleWith: traits)
            paragraph.minimumLineHeight = height
            paragraph.maximumLineHeight = height
            baselineOffset = (height - font.lineHeight) / 4
        }
        label.attributedText = NSAttributedString(string: text, attributes: [
            .font: font,
            .foregroundColor: UIColor(color),
            .paragraphStyle: paragraph,
            .baselineOffset: baselineOffset,
        ])
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UILabel, context: Context) -> CGSize? {
        let width = proposal.width ?? .greatestFiniteMagnitude
        let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: proposal.width ?? ceil(fitted.width), height: ceil(fitted.height))
    }
}
