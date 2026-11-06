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

/// Lien texte (« J'ai déjà un compte »).
struct LinkButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Font.Pawrise.headline)
            .foregroundStyle(Color.Pawrise.primary)
            .frame(minHeight: 44)
            .opacity(configuration.isPressed ? 0.6 : 1)
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
}

// MARK: - Champs

/// Champ libellé + saisie arrondie, comme dans les écrans Créer un compte / Connexion.
struct LabeledField<Field: View>: View {
    let label: String
    var error: String?
    @ViewBuilder var field: Field

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(Font.Pawrise.footnote)
                .foregroundStyle(Color.Pawrise.textPrimary)
            field
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .padding(.horizontal, 16)
                .frame(minHeight: 54)
                .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
                .overlay {
                    RoundedRectangle(cornerRadius: Radius.control)
                        .strokeBorder(error == nil ? Color.Pawrise.border : Color.Pawrise.alert, lineWidth: 1)
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

/// Barre segmentée du parcours « profil du chien » (9 segments).
struct StepProgressBar: View {
    let total: Int
    let current: Int // 1-based

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...total, id: \.self) { index in
                Capsule()
                    .fill(index <= current ? Color.Pawrise.primary : Color.Pawrise.border)
                    .frame(height: 4)
            }
        }
        .accessibilityElement()
        .accessibilityLabel("Étape \(current) sur \(total)")
    }
}

/// En-tête de parcours : retour à gauche, contenu au centre, fermeture à droite.
struct FlowHeader<Center: View>: View {
    var onBack: (() -> Void)?
    var onClose: (() -> Void)?
    @ViewBuilder var center: Center

    var body: some View {
        HStack(spacing: 16) {
            iconButton("chevron.left", label: "Retour", action: onBack)
            center.frame(maxWidth: .infinity)
            iconButton("xmark", label: "Fermer", action: onClose)
        }
        .frame(minHeight: 44)
    }

    @ViewBuilder
    private func iconButton(_ symbol: String, label: String, action: (() -> Void)?) -> some View {
        if let action {
            Button(action: action) {
                Image(systemName: symbol)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(Color.Pawrise.primary)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel(label)
        } else {
            Color.clear.frame(width: 44, height: 44)
        }
    }
}

extension FlowHeader where Center == EmptyView {
    init(onBack: (() -> Void)? = nil, onClose: (() -> Void)? = nil) {
        self.init(onBack: onBack, onClose: onClose) { EmptyView() }
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
            AsyncImage(url: url) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else {
                    placeholder
                }
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
        Image(systemName: "pawprint.fill")
            .font(.system(size: size * 0.35))
            .foregroundStyle(Color.Pawrise.primary.opacity(0.5))
    }
}

/// Logo Pawrise (fleur de pattes) dessiné en SF Symbols en attendant l'asset vectoriel.
struct PawriseMark: View {
    var size: CGFloat = 80

    var body: some View {
        Image(systemName: "pawprint.fill")
            .font(.system(size: size * 0.4, weight: .semibold))
            .foregroundStyle(Color.Pawrise.primary)
            .frame(width: size, height: size)
            .background(Color.Pawrise.elevated, in: .rect(cornerRadius: size * 0.28))
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
