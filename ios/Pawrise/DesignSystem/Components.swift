import SwiftUI

// MARK: - Boutons

/// Bouton principal plein (« Se connecter », « Suivant »).
struct PrimaryButtonStyle: ButtonStyle {
    var isLoading = false
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        ZStack {
            configuration.label.opacity(isLoading ? 0 : 1)
            if isLoading { ProgressView().tint(Color.Pawrise.onPrimary) }
        }
        .font(Font.Pawrise.button)
        .foregroundStyle(Color.Pawrise.onPrimary)
        .frame(maxWidth: .infinity, minHeight: 56)
        .background(Color.Pawrise.primary.opacity(isEnabled ? 1 : 0.4), in: .rect(cornerRadius: Radius.control))
        .scaleEffect(configuration.isPressed ? 0.98 : 1)
        .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// Bouton secondaire blanc (« Créer un compte », « Saisir le code manuellement »).
struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Font.Pawrise.button)
            .foregroundStyle(Color.Pawrise.primary)
            .frame(maxWidth: .infinity, minHeight: 56)
            .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
            .opacity(configuration.isPressed ? 0.7 : 1)
    }
}

/// Lien texte (« J’ai déjà un compte »).
/// `compact` : la hauteur suit le texte (comme la maquette) et la zone tactile est agrandie à 44 pt sans
/// décaler la mise en page.
struct LinkButtonStyle: ButtonStyle {
    var compact = false

    func makeBody(configuration: Configuration) -> some View {
        if compact {
            configuration.label
                .font(Font.Pawrise.link)
                .foregroundStyle(Color.Pawrise.primary)
                .figmaLineHeight(1.35, size: 14, relativeTo: .subheadline)
                .contentShape(Rectangle().inset(by: -12))
                .opacity(configuration.isPressed ? 0.6 : 1)
        } else {
            configuration.label
                .font(Font.Pawrise.link)
                .foregroundStyle(Color.Pawrise.primary)
                .frame(minHeight: 44)
                .opacity(configuration.isPressed ? 0.6 : 1)
        }
    }
}

extension ButtonStyle where Self == PrimaryButtonStyle {
    static var pawrisePrimary: PrimaryButtonStyle { PrimaryButtonStyle() }
    static func pawrisePrimary(loading: Bool) -> PrimaryButtonStyle { PrimaryButtonStyle(isLoading: loading) }
}

extension ButtonStyle where Self == SecondaryButtonStyle {
    static var pawriseSecondary: SecondaryButtonStyle { SecondaryButtonStyle() }
}

extension ButtonStyle where Self == LinkButtonStyle {
    static var pawriseLink: LinkButtonStyle { LinkButtonStyle() }
    /// Lien à la hauteur du texte (maquette), zone tactile agrandie.
    static var pawriseCompactLink: LinkButtonStyle { LinkButtonStyle(compact: true) }
}

// MARK: - Champs

/// Champ libellé + saisie arrondie, comme dans les écrans Créer un compte / Connexion.
/// Maquette : libellé Inter 600 12 (135 %), 6 pt d’écart, champ de 54 pt, rayon 16.
/// Créer un compte (A02) utilise une saisie Inter 17 et une bordure #C1D4E0 ; Connexion (J02) Inter 15 et #D5E3EC.
struct LabeledField<Field: View>: View {
    let label: String
    var error: String?
    var fieldFont: Font = Font.Pawrise.body
    var borderColor: Color = Color.Pawrise.border
    @ViewBuilder var field: Field

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(Font.Pawrise.footnote)
                .foregroundStyle(Color.Pawrise.textMuted)
                .figmaLineHeight(1.35, size: 12, relativeTo: .footnote)
            field
                .font(fieldFont)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .padding(.horizontal, 16)
                .frame(minHeight: 54)
                .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
                .overlay {
                    RoundedRectangle(cornerRadius: Radius.control)
                        .strokeBorder(error == nil ? borderColor : Color.Pawrise.alert, lineWidth: 1)
                }
            if let error {
                Text(error)
                    .font(Font.Pawrise.caption)
                    .foregroundStyle(Color.Pawrise.alert)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// Bandeau d'erreur sous un formulaire.
struct ErrorBanner: View {
    let message: String

    var body: some View {
        Label(message, systemImage: "exclamationmark.circle")
            .font(Font.Pawrise.subheadline)
            .foregroundStyle(Color.Pawrise.alert)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.Pawrise.alertSurface, in: .rect(cornerRadius: Radius.control))
            .accessibilityElement(children: .combine)
    }
}

// MARK: - Parcours en étapes

/// Barre segmentée du parcours « profil du chien » (9 segments de 26 × 6 pt, rayon 3, écart 5).
struct StepProgressBar: View {
    let total: Int
    let current: Int // 1-based

    var body: some View {
        HStack(spacing: 5) {
            ForEach(1...total, id: \.self) { index in
                RoundedRectangle(cornerRadius: 3)
                    .fill(index <= current ? Color.Pawrise.primary : Color.Pawrise.border)
                    .frame(width: 26, height: 6)
            }
        }
        .accessibilityElement()
        .accessibilityLabel("Étape \(current) sur \(total)")
    }
}

/// En-tête de parcours (maquette « En-tête ») : rangée non étirée calée à gauche — marge 16, chevron 20,
/// écart 12, zone centrale de 282 pt (progression ou titre centré), écart 12, croix 20 ; 28 pt libres à droite.
/// Rangée de 44 pt placée sous la zone sûre.
struct FlowHeader<Center: View>: View {
    var onBack: (() -> Void)?
    var onClose: (() -> Void)?
    @ViewBuilder var center: Center

    var body: some View {
        HStack(spacing: 12) {
            FlowIconButton(asset: "Onboarding/back", label: "Retour", action: onBack)
            Color.clear
                .frame(width: 282)
                .overlay { center }
            FlowIconButton(asset: "Onboarding/close", label: "Fermer", action: onClose)
        }
        .padding(.leading, 16)
        .frame(maxWidth: .infinity, minHeight: 44, maxHeight: 44, alignment: .leading)
    }
}

extension FlowHeader where Center == EmptyView {
    init(onBack: (() -> Void)? = nil, onClose: (() -> Void)? = nil) {
        self.init(onBack: onBack, onClose: onClose) { EmptyView() }
    }
}

/// Icône de 20 pt de la maquette, zone tactile de 44 pt qui ne décale pas la mise en page.
struct FlowIconButton: View {
    let asset: String
    let label: String
    var size: CGFloat = 20
    let action: (() -> Void)?

    var body: some View {
        if let action {
            Button(action: action) {
                Image(asset)
                    .resizable()
                    .frame(width: size, height: size)
                    .contentShape(Rectangle().inset(by: -12))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
        } else {
            Color.clear.frame(width: size, height: size)
        }
    }
}

// MARK: - Éléments visuels

/// Pastille ronde avec icône (écrans Découverte, Activation du collier).
struct IconBadge: View {
    let systemImage: String
    var size: CGFloat = 96

    var body: some View {
        Image(systemName: systemImage)
            .font(.system(size: size * 0.34, weight: .regular))
            .foregroundStyle(Color.Pawrise.primary)
            .frame(width: size, height: size)
            .background(Color.Pawrise.elevated, in: .circle)
            .accessibilityHidden(true)
    }
}

/// Pastille ronde 96 pt avec une icône vectorielle de la maquette (40 pt), écrans Découverte.
struct AssetIconBadge: View {
    let asset: String
    var size: CGFloat = 96
    var iconSize: CGFloat = 40

    var body: some View {
        Image(asset)
            .resizable()
            .frame(width: iconSize, height: iconSize)
            .frame(width: size, height: size)
            .background(Color.Pawrise.elevated, in: .circle)
            .accessibilityHidden(true)
    }
}

/// « ✓ Tout va bien », « À surveiller », « Alerte ».
struct StatePill: View {
    let state: HealthState

    var body: some View {
        Label(state.label, systemImage: state == .good ? "checkmark" : "exclamationmark.triangle")
            .font(Font.Pawrise.caption)
            .foregroundStyle(Color.Pawrise.tint(for: state))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.Pawrise.surface(for: state), in: .capsule)
    }
}

/// Photo du chien, ronde ou carrée arrondie.
struct DogAvatar: View {
    let photo: Dog.Photo?
    var size: CGFloat = 56
    var cornerRadius: CGFloat? // nil = cercle

    var body: some View {
        content
            .frame(width: size, height: size)
            .background(Color.Pawrise.avatarBackdrop)
            .clipShape(.rect(cornerRadius: cornerRadius ?? size / 2))
            .accessibilityHidden(true)
    }

    @ViewBuilder
    private var content: some View {
        switch photo {
        case let .remote(url):
            // `photoUrl` de l'API-CORE (ou photo de démo) : chargée en arrière-plan, gardée en cache.
            CachedRemoteImage(url: url) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                placeholder
            }
        case let .local(data):
            if let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                placeholder
            }
        case nil:
            placeholder
        }
    }

    private var placeholder: some View {
        PawriseIcon(.paw, size: size * 0.35)
            .foregroundStyle(Color.Pawrise.primary.opacity(0.5))
    }
}

/// Logo Pawrise (maquette « Marque ») : icône vectorielle Icon/brand 48 pt sur carré 80 pt, rayon 24, fond #E5F1F7.
struct PawriseMark: View {
    var size: CGFloat = 80

    var body: some View {
        Image("Onboarding/brand")
            .resizable()
            .frame(width: size * 0.6, height: size * 0.6)
            .frame(width: size, height: size)
            .background(Color.Pawrise.elevated, in: .rect(cornerRadius: size * 0.3))
            .accessibilityHidden(true)
    }
}

extension View {
    /// Fond de page standard des écrans Pawrise.
    func pawrisePage() -> some View {
        frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(Color.Pawrise.page.ignoresSafeArea())
    }
}

// MARK: - Texte à coupure Figma

/// Texte multiligne coupé comme dans Figma : SwiftUI évite les mots seuls en dernière ligne (« push out »)
/// et ne coupe donc pas les titres au même endroit que la maquette. Ce libellé UIKit désactive cette règle,
/// applique l’interligne de la maquette (hauteur de ligne fixe, glyphes centrés) et suit Dynamic Type.
struct FigmaText: UIViewRepresentable {
    let text: String
    var fontName: String
    var size: CGFloat
    var lineHeight: CGFloat = 1.35
    var textStyle: UIFont.TextStyle = .title1
    var color: Color = Color.Pawrise.textPrimary
    var alignment: NSTextAlignment = .natural
    var isHeader = true
    /// Approche (pt) : Figma rend Inter 13,5 environ 2 % plus serré que iOS.
    var kern: CGFloat = 0

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// Titre Bricolage Grotesque Bold (titres d’écran 26, 24…).
    static func display(_ text: String, size: CGFloat, alignment: NSTextAlignment = .natural) -> FigmaText {
        FigmaText(text: text, fontName: "BricolageGrotesque-14ptBold", size: size, alignment: alignment)
    }

    /// Texte Inter Regular secondaire (#5A7082), ex. Inter 13,5 centré.
    static func body(_ text: String, size: CGFloat, alignment: NSTextAlignment = .natural) -> FigmaText {
        FigmaText(
            text: text, fontName: "Inter-Regular", size: size, textStyle: .subheadline,
            color: Color.Pawrise.textMuted, alignment: alignment, isHeader: false, kern: size == 13.5 ? -0.12 : 0
        )
    }

    func makeUIView(context: Context) -> UILabel {
        let label = UILabel()
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.lineBreakStrategy = []
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        label.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return label
    }

    func updateUIView(_ label: UILabel, context: Context) {
        let base = PawriseUIFont.make(fontName, size: size) ?? .systemFont(ofSize: size, weight: .bold)
        let metrics = UIFontMetrics(forTextStyle: textStyle)
        let font = metrics.scaledFont(for: base)
        let lineBox = metrics.scaledValue(for: size * lineHeight)
        let paragraph = NSMutableParagraphStyle()
        paragraph.minimumLineHeight = lineBox
        paragraph.maximumLineHeight = lineBox
        paragraph.alignment = alignment
        paragraph.lineBreakStrategy = []
        label.attributedText = NSAttributedString(string: text, attributes: [
            .font: font,
            .foregroundColor: UIColor(color),
            .paragraphStyle: paragraph,
            .kern: kern,
            // UIKit place l’espace d’interligne au-dessus des glyphes : on les recentre comme Figma.
            .baselineOffset: (lineBox - font.lineHeight) / 4,
        ])
        label.accessibilityTraits = isHeader ? [.staticText, .header] : .staticText
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UILabel, context: Context) -> CGSize? {
        let width = proposal.width ?? UIView.layoutFittingExpandedSize.width
        let fitted = uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
        return CGSize(width: proposal.width ?? fitted.width, height: ceil(fitted.height))
    }
}
