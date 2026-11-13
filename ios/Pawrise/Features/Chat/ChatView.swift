import SwiftUI

/// Onglet Chat IA (Figma US-H 02 à 06 « Chat IA »).
struct ChatView: View {
    @Environment(AppEnvironment.self) private var environment
    @Environment(DogStore.self) private var dogs
    @Environment(ChatConversations.self) private var conversations

    var body: some View {
        Group {
            if let dog = dogs.selectedDog {
                ChatConversationView(dog: dog, model: conversations.conversation(for: dog, environment: environment))
                    .id(dog.id)
            }
        }
        .pawrisePage()
        .onChange(of: dogs.selectedDog?.id) { conversations.cancelAll() }
        .onDisappear { conversations.cancelAll() }
    }
}

/// Mesures de la maquette (US-H).
private enum ChatLayout {
    static let avatarSize: CGFloat = 28
    static let avatarSpacing: CGFloat = 8
    /// Largeur des bulles (279 pt sur un écran de 390 pt).
    static let bubbleWidth: CGFloat = 279
    static let bubbleFontSize: CGFloat = 14.5
    static let bubbleLineHeight: CGFloat = 1.62
}

private struct ChatConversationView: View {
    let dog: Dog
    let model: ChatViewModel

    @Environment(TabRouter.self) private var router
    @State private var draft = ""
    @FocusState private var isComposerFocused: Bool

    private static let bottomID = "chat-bottom"

    var body: some View {
        VStack(spacing: 0) {
            ChatHeader(dog: dog)

            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        AssistantRow {
                            ChatBubbleText(text: welcome)
                        }
                        if model.showsQuickReplies {
                            quickReplies
                        }
                        ForEach(model.messages) { message in
                            switch message.role {
                            case .owner:
                                OwnerBubble(text: message.text)
                            case .assistant:
                                AssistantRow {
                                    AssistantMessageContent(message: message)
                                }
                            }
                        }
                        if model.isResponding {
                            TypingRow(status: model.statusText)
                        }
                        if let failure = model.failure {
                            FailureRow(message: failure) { model.retry() }
                        }
                        Color.clear.frame(height: 1).id(Self.bottomID)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(.rect)
                    // Toucher le fil ferme le clavier (le brouillon est conservé).
                    .onTapGesture { isComposerFocused = false }
                }
                // Fil aligné en haut comme dans la maquette ; il suit le dernier message quand il déborde.
                .modifier(TopAlignedThread())
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: scrollSignature) {
                    withAnimation(.easeOut(duration: 0.25)) {
                        proxy.scrollTo(Self.bottomID, anchor: .bottom)
                    }
                }
                // Clavier ouvert ou fermé : le fil reste calé sur le dernier message.
                .onChange(of: isComposerFocused) {
                    Task {
                        try? await Task.sleep(for: .milliseconds(350))
                        withAnimation(.easeOut(duration: 0.2)) {
                            proxy.scrollTo(Self.bottomID, anchor: .bottom)
                        }
                    }
                }
            }

            Button("Contacter un vétérinaire") { router.open(.appointments) }
                .pawriseFont(.inter, 14, lineHeight: 1.35, relativeTo: .subheadline)
                .foregroundStyle(Color.Pawrise.primary)
                .frame(maxWidth: .infinity, minHeight: 36)

            composer
                .padding(.top, 12)
                .padding(.horizontal, 12)
                .padding(.bottom, 8)
        }
    }

    /// « Bonjour 👋 Je suis Pawrise… » : l'émoji est rendu avec la police système (Inter n'en a pas).
    private var welcome: Text {
        let wave = Text("👋").font(.system(size: ChatLayout.bubbleFontSize))
        return Text("Bonjour \(wave) Je suis Pawrise, l'assistant de \(dog.name). Que souhaitez-vous consulter ?")
    }

    /// Change à chaque nouvel élément du fil pour déclencher le défilement vers le bas.
    private var scrollSignature: [String] {
        [
            String(model.messages.count),
            String(model.isResponding),
            model.statusText ?? "",
            model.failure ?? "",
        ]
    }

    private var quickReplies: some View {
        VStack(alignment: .leading, spacing: 8) {
            QuickReplyButton(title: "Comment va \(dog.name) aujourd'hui ?") {
                model.send("Comment va \(dog.name) aujourd'hui ?")
            }
            QuickReplyButton(title: "Et son sommeil ?") {
                model.send("Et son sommeil ?")
            }
            QuickReplyButton(title: "Voir les indicateurs", minWidth: 160) {
                router.open(.health)
            }
        }
        .padding(.leading, ChatLayout.avatarSize + ChatLayout.avatarSpacing)
    }

    private var canSubmit: Bool {
        model.canSend && !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var composer: some View {
        HStack(spacing: 8) {
            TextField(
                "",
                text: $draft,
                prompt: Text("Écrire un message…").foregroundStyle(Color.Pawrise.textMuted)
            )
            .pawriseFont(.inter, 15, relativeTo: .body)
            .foregroundStyle(Color.Pawrise.textPrimary)
            .focused($isComposerFocused)
            .submitLabel(.send)
            .onSubmit(submit)
            .accessibilityLabel("Écrire un message…")
            Button(action: submit) {
                PawriseIcon(.send, size: 17)
                    .foregroundStyle(Color.Pawrise.onPrimary)
                    .frame(width: 36, height: 36)
                    .background(Color.Pawrise.primary.opacity(canSubmit ? 1 : 0.4), in: .circle)
                    .padding(4)
                    .contentShape(.rect)
                    .padding(-4)
            }
            .buttonStyle(.plain)
            // Pas de `.disabled` : il ajouterait l'atténuation système à l'opacité 0,4 de la maquette.
            .allowsHitTesting(canSubmit)
            .accessibilityLabel("Envoyer")
            .accessibilityHint(canSubmit ? "" : "Écrivez d'abord un message")
        }
        .padding(.horizontal, 16)
        .frame(minHeight: 50)
        .background(Color.Pawrise.card, in: .capsule)
        .overlay { Capsule().strokeBorder(Color.Pawrise.border, lineWidth: 1) }
        .animation(.easeOut(duration: 0.15), value: canSubmit)
    }

    private func submit() {
        guard canSubmit, model.send(draft) else { return }
        draft = ""
        isComposerFocused = true
    }
}

/// Fil aligné en haut tant qu'il est court (iOS 18+ ; avant, ancré en bas comme un fil de messages).
private struct TopAlignedThread: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 18, *) {
            content
                .defaultScrollAnchor(.top, for: .alignment)
                .defaultScrollAnchor(.bottom, for: .sizeChanges)
        } else {
            content.defaultScrollAnchor(.bottom)
        }
    }
}

// MARK: - En-tête

private struct ChatHeader: View {
    let dog: Dog

    var body: some View {
        HStack(spacing: 12) {
            DogAvatar(photo: dog.photo, size: 44, cornerRadius: 16)
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 6) {
                    Text("Pawrise")
                        .pawriseFont(.inter, 17, .semibold, lineHeight: 1.35, relativeTo: .headline)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    PawriseIcon(.paw, size: 10)
                        .foregroundStyle(Color.Pawrise.onPrimary)
                        .frame(width: 16, height: 16)
                        .background(Color.Pawrise.primary, in: .circle)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel("Assistant officiel")
                }
                Text("● Assistant de \(dog.name) · \(dog.collar?.connected == true ? "collier connecté" : "collier non connecté")")
                    .pawriseFont(.inter, 12, lineHeight: 1.35, relativeTo: .caption)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            Spacer(minLength: 0)
        }
        // Maquette : cadre de 82 pt, contenu centré (20 pt au-dessus, 18 pt au-dessous).
        .padding(.top, 20)
        .padding(.horizontal, 16)
        .padding(.bottom, 18)
        .background(Color.Pawrise.page)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Bulles

/// Ligne de l'assistant : avatar rond de 28 pt aligné en bas, bulle blanche.
private struct AssistantRow<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        HStack(alignment: .bottom, spacing: ChatLayout.avatarSpacing) {
            PawriseIcon(.paw, size: 15)
                .foregroundStyle(Color.Pawrise.onPrimary)
                .frame(width: ChatLayout.avatarSize, height: ChatLayout.avatarSize)
                .background(Color.Pawrise.primary, in: .circle)
            content
            Spacer(minLength: 0)
        }
    }
}

private extension Shape where Self == UnevenRoundedRectangle {
    static var assistantBubble: UnevenRoundedRectangle {
        UnevenRoundedRectangle(topLeadingRadius: 20, bottomLeadingRadius: 4, bottomTrailingRadius: 20, topTrailingRadius: 20)
    }

    static var ownerBubble: UnevenRoundedRectangle {
        UnevenRoundedRectangle(topLeadingRadius: 20, bottomLeadingRadius: 20, bottomTrailingRadius: 4, topTrailingRadius: 20)
    }
}

private extension View {
    /// Texte des bulles : Inter 400 14,5 · 162 %.
    func bubbleFont() -> some View {
        pawriseFont(.inter, ChatLayout.bubbleFontSize, lineHeight: ChatLayout.bubbleLineHeight, relativeTo: .body)
    }
}

private struct ChatBubbleText: View {
    let text: Text

    var body: some View {
        text
            .bubbleFont()
            .foregroundStyle(Color.Pawrise.textPrimary)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 11)
            .frame(maxWidth: ChatLayout.bubbleWidth, alignment: .leading)
            .background(Color.Pawrise.card, in: .assistantBubble)
            .overlay { UnevenRoundedRectangle.assistantBubble.strokeBorder(Color.Pawrise.border, lineWidth: 1) }
    }
}

private struct OwnerBubble: View {
    let text: String

    var body: some View {
        HStack {
            Spacer(minLength: 0)
            Text(text)
                .bubbleFont()
                .foregroundStyle(Color.Pawrise.onPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(maxWidth: ChatLayout.bubbleWidth, alignment: .leading)
                .background(Color.Pawrise.primary, in: .ownerBubble)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Vous : \(text)")
    }
}

/// Réponse de l'assistant : bandeau « Urgence » si besoin, puis le texte (maquette : bulle de texte seule ;
/// le lien fixe « Contacter un vétérinaire » sous le fil reste l'accès au vétérinaire).
private struct AssistantMessageContent: View {
    let message: ChatMessage

    private var isUrgent: Bool { message.response?.isUrgent == true }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if isUrgent {
                Label("Urgence", systemImage: "exclamationmark.triangle.fill")
                    .pawriseFont(.inter, 13, .semibold, relativeTo: .subheadline)
                    .foregroundStyle(Color.Pawrise.alert)
            }
            Text(message.text)
                .bubbleFont()
                .foregroundStyle(Color.Pawrise.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 11)
        .frame(maxWidth: ChatLayout.bubbleWidth, alignment: .leading)
        .background(isUrgent ? Color.Pawrise.alertSurface : Color.Pawrise.card, in: .assistantBubble)
        .overlay {
            UnevenRoundedRectangle.assistantBubble
                .strokeBorder(isUrgent ? Color.Pawrise.alert.opacity(0.4) : Color.Pawrise.border, lineWidth: 1)
        }
    }
}

// MARK: - Saisie en cours, erreurs, suggestions

private struct TypingRow: View {
    let status: String?

    var body: some View {
        AssistantRow {
            HStack(spacing: 10) {
                TypingDots()
                    .padding(.vertical, 12)
                    .padding(.leading, 14)
                    .padding(.trailing, 26)
                    .background(Color.Pawrise.card, in: .assistantBubble)
                    .overlay { UnevenRoundedRectangle.assistantBubble.strokeBorder(Color.Pawrise.border, lineWidth: 1) }

            }
            .animation(.easeOut(duration: 0.2), value: status)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(status ?? "Pawrise prépare sa réponse")
    }
}

/// Trois points de 6 pt espacés de 4 pt (maquette « Trois points animés »).
private struct TypingDots: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let time = context.date.timeIntervalSinceReferenceDate
            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { index in
                    let phase = reduceMotion ? 0 : sin(time * 6 - Double(index) * 0.8)
                    Circle()
                        .fill(Color.Pawrise.textMuted)
                        .frame(width: 6, height: 6)
                        .offset(y: -2 * max(phase, 0))
                        .opacity(0.55 + 0.45 * max(phase, 0))
                }
            }
            .frame(height: 8)
        }
    }
}

private struct FailureRow: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 10) {
            Image(systemName: "exclamationmark.circle")
                .foregroundStyle(Color.Pawrise.alert)
                .accessibilityHidden(true)
            Text(message)
                .pawriseFont(.inter, 13, lineHeight: 1.35, relativeTo: .subheadline)
                .foregroundStyle(Color.Pawrise.alert)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("Réessayer", action: onRetry)
                .pawriseFont(.inter, 13, .semibold, relativeTo: .subheadline)
                .foregroundStyle(Color.Pawrise.primary)
                .frame(minHeight: 44)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 4)
        .background(Color.Pawrise.alertSurface, in: .rect(cornerRadius: 16))
    }
}

/// Suggestion en capsule (Inter 600 13, fond #E5F1F7, filet primaire, 36 pt pour une ligne) : ajustée au texte,
/// au plus 219 pt de large ; au-delà le texte passe à la ligne comme dans Figma (« Comment va Nala aujourd'hui » / « ? »).
private struct QuickReplyButton: View {
    let title: String
    var minWidth: CGFloat = 0
    let action: () -> Void

    private static let maxWidth: CGFloat = 219

    /// Le titre tient-il sur une ligne dans 219 pt (padding 14 + 14) ?
    private var fitsOnOneLine: Bool {
        guard let font = PawriseUIFont.make(PawriseFontFamily.inter.postScriptName(.semibold), size: 13) else { return true }
        let width = (title as NSString).size(withAttributes: [.font: font]).width
        return width + 28 <= Self.maxWidth
    }

    var body: some View {
        Button(action: action) {
            if fitsOnOneLine {
                capsule(
                    Text(title)
                        .pawriseFont(.inter, 13, .semibold, lineHeight: 1.35, relativeTo: .subheadline)
                        .foregroundStyle(Color.Pawrise.primary)
                        .fixedSize()
                        .frame(minWidth: max(minWidth - 28, 0), alignment: .leading)
                )
            } else {
                capsule(
                    MaquetteText(text: title, size: 13, weight: .semibold, lineHeight: 1.35, color: Color.Pawrise.primary, relativeTo: .subheadline)
                        .frame(width: Self.maxWidth - 26, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true),
                    horizontalPadding: 13
                )
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
    }

    private func capsule(_ content: some View, horizontalPadding: CGFloat = 14) -> some View {
        content
            .padding(.horizontal, horizontalPadding)
            .padding(.vertical, 9)
            .background(Color.Pawrise.elevated, in: .capsule)
            .overlay { Capsule().strokeBorder(Color.Pawrise.primary, lineWidth: 1) }
            .contentShape(.capsule)
    }
}
