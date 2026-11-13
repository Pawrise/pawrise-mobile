import SwiftUI

// Icônes et réglages typographiques des écrans Santé, Chat IA et Paramètres (équipe B).
// Fichier séparé de Theme.swift / Components.swift : il n'y ajoute que des éléments nouveaux.

// MARK: - Icônes du Figma

/// Icônes du Figma « Pawrise-App » (grille 24 × 24, trait 1,9 mis à l'échelle), reprises du composant
/// `Icon` du prototype (pawrise-figma/src/components/Icon.tsx) pour un rendu identique à la maquette.
enum PawriseGlyph: Sendable {
    case map, chat, settings, doc, heart, pulse, activity, moon, thermometer, battery
    case alert, swap, close, check, signal, shield, send, paw, sparkles, brand
    case user, lock, chevronRight, trash, logout

    fileprivate var paths: [String] {
        switch self {
        case .map: ["M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z", "M9 4v14M15 6v14"]
        case .chat: ["M4 5h16v11H9l-5 4V5Z"]
        case .settings: [
            "M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 0 1 0 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 0 1 0-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.281Z",
            "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
        ]
        case .doc: ["M6 3h8l4 4v14H6V3Z", "M14 3v4h4M9 13h6M9 17h6"]
        case .heart: ["M12 20s-7-4.5-9.5-9C1 8 2.5 4.5 6 4.5c2 0 3.2 1.2 4 2.5.8-1.3 2-2.5 4-2.5 3.5 0 5 3.5 3.5 6.5C19 15.5 12 20 12 20Z"]
        case .pulse: ["M2 12h4l2-5 4 12 2.5-9 1.5 2H22"]
        case .activity: ["M2 12h4l3 8 4-16 3 8h6"]
        case .moon: ["M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z"]
        case .thermometer: ["M14 14.7V5a2 2 0 1 0-4 0v9.7a4 4 0 1 0 4 0Z", "M12 9v6"]
        case .battery: ["M4 8h12a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2Z", "M22 11v2"]
        case .alert: ["M12 3 2 20h20L12 3Z", "M12 10v4M12 17h.01"]
        case .swap: ["M7.5 21 3 16.5m0 0L7.5 12M3 16.5h13.5m0-13.5L21 7.5m0 0L16.5 12M21 7.5H7.5"]
        case .close: ["M6 6l12 12M18 6 6 18"]
        case .check: ["m5 12 5 5L20 6"]
        case .signal: ["M4 18v-3M9 18v-6M14 18v-9M19 18V6"]
        case .shield: ["M12 3 5 6v6c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V6l-7-3Z"]
        case .send: ["M4 12 20 4l-6 16-3-6-7-2Z"]
        case .paw: [
            Self.circle(7, 9, 1.7), Self.circle(12, 7, 1.7), Self.circle(17, 9, 1.7),
            "M12 12c-2.5 0-4.5 2-4.5 4S9 21 12 21s4.5-1 4.5-3-2-4-4.5-4Z",
        ]
        case .sparkles: ["M12 3l1.8 4.7L18.5 9l-4.7 1.8L12 15l-1.8-4.2L5.5 9l4.7-1.3L12 3ZM19 15l.9 2.3 2.1.7-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.7L19 15Z"]
        case .user: [Self.circle(12, 8, 4), "M4 21c0-4 3.6-6 8-6s8 2 8 6"]
        case .lock: ["M6 10h12a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2Z", "M8 10V7a4 4 0 0 1 8 0v3"]
        case .chevronRight: ["m9 6 6 6-6 6"]
        case .trash: ["M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13"]
        case .logout: ["M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3", "M10 17l5-5-5-5M15 12H4"]
        case .brand: []
        }
    }

    fileprivate var isFilled: Bool { self == .brand || self == .sparkles }

    private static func circle(_ cx: Double, _ cy: Double, _ r: Double) -> String {
        "M\(cx - r) \(cy)a\(r) \(r) 0 1 0 \(2 * r) 0a\(r) \(r) 0 1 0 \(-2 * r) 0Z"
    }
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
