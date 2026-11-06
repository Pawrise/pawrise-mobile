import SwiftUI

/// Onglet Chat IA (Figma 02 à 06 « Chat IA »).
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
                .padding(.horizontal, Spacing.page)
                .padding(.top, 8)
                .padding(.bottom, 12)

            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        AssistantRow {
                            ChatBubbleText(
                                text: "Bonjour 👋 Je suis Pawrise, l'assistant de \(dog.name). Que souhaitez-vous consulter ?"
                            )
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
                                    AssistantMessageContent(message: message) { router.open(.appointments) }
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
                    .padding(.horizontal, Spacing.page)
                    .padding(.vertical, 12)
                }
                .defaultScrollAnchor(.bottom)
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: scrollSignature) {
                    withAnimation(.easeOut(duration: 0.25)) {
                        proxy.scrollTo(Self.bottomID, anchor: .bottom)
                    }
                }
            }

            Button("Contacter un vétérinaire") { router.open(.appointments) }
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.primary)
                .frame(minHeight: 44)
                .padding(.top, 4)

            composer
                .padding(.horizontal, 14)
                .padding(.bottom, 10)
        }
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
        VStack(alignment: .leading, spacing: 10) {
            QuickReplyButton(title: "Comment va \(dog.name) aujourd'hui ?") {
                model.send("Comment va \(dog.name) aujourd'hui ?")
            }
            QuickReplyButton(title: "Et son sommeil ?") {
                model.send("Et son sommeil ?")
            }
            QuickReplyButton(title: "Voir les indicateurs") {
                router.open(.health)
            }
        }
        .padding(.leading, ChatLayout.avatarSize + ChatLayout.avatarSpacing)
        .padding(.trailing, 40)
    }

    private var canSubmit: Bool {
        model.canSend && !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var composer: some View {
        HStack(spacing: 8) {
            TextField("Écrire un message…", text: $draft)
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .focused($isComposerFocused)
                .submitLabel(.send)
                .onSubmit(submit)
                .padding(.leading, 18)
                .frame(minHeight: 48)
            Button(action: submit) {
                Image(systemName: "paperplane")
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(Color.Pawrise.onPrimary)
                    .frame(width: 40, height: 40)
                    .background(Color.Pawrise.primary.opacity(canSubmit ? 1 : 0.35), in: .circle)
            }
            .disabled(!canSubmit)
            .accessibilityLabel("Envoyer")
            .padding(.trailing, 6)
        }
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

// MARK: - En-tête

private struct ChatHeader: View {
    let dog: Dog

    var body: some View {
        HStack(spacing: 14) {
            DogAvatar(photo: dog.photo, size: 46, cornerRadius: 14)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text("Pawrise")
                        .font(Font.Pawrise.title2)
                        .foregroundStyle(Color.Pawrise.textPrimary)
                    Image(systemName: "pawprint.fill")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(Color.Pawrise.onPrimary)
                        .frame(width: 17, height: 17)
                        .background(Color.Pawrise.primary, in: .circle)
                        .accessibilityLabel("Assistant officiel")
                }
                HStack(spacing: 6) {
                    Circle()
                        .fill(Color.Pawrise.textMuted)
                        .frame(width: 8, height: 8)
                    Text("Assistant de \(dog.name) · \(dog.collar?.connected == true ? "collier connecté" : "collier non connecté")")
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.85)
                }
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Bulles

/// Ligne de l'assistant : petit avatar rond à gauche, bulle blanche.
private enum ChatLayout {
    static let avatarSize: CGFloat = 28
    static let avatarSpacing: CGFloat = 10
}

private struct AssistantRow<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        HStack(alignment: .bottom, spacing: ChatLayout.avatarSpacing) {
            Image(systemName: "pawprint.fill")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.Pawrise.onPrimary)
                .frame(width: ChatLayout.avatarSize, height: ChatLayout.avatarSize)
                .background(Color.Pawrise.primary, in: .circle)
                .accessibilityHidden(true)
            content
            Spacer(minLength: 40)
        }
    }
}

private extension Shape where Self == UnevenRoundedRectangle {
    static var assistantBubble: UnevenRoundedRectangle {
        UnevenRoundedRectangle(topLeadingRadius: 22, bottomLeadingRadius: 6, bottomTrailingRadius: 22, topTrailingRadius: 22)
    }

    static var ownerBubble: UnevenRoundedRectangle {
        UnevenRoundedRectangle(topLeadingRadius: 22, bottomLeadingRadius: 22, bottomTrailingRadius: 6, topTrailingRadius: 22)
    }
}

private struct ChatBubbleText: View {
    let text: String

    var body: some View {
        Text(text)
            .font(Font.Pawrise.body)
            .foregroundStyle(Color.Pawrise.textPrimary)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.Pawrise.card, in: .assistantBubble)
            .overlay { UnevenRoundedRectangle.assistantBubble.strokeBorder(Color.Pawrise.border, lineWidth: 1) }
    }
}

private struct OwnerBubble: View {
    let text: String

    var body: some View {
        HStack {
            Spacer(minLength: 56)
            Text(text)
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.onPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.Pawrise.primary, in: .ownerBubble)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Vous : \(text)")
    }
}

/// Réponse de l'assistant : bandeau « Urgence », texte, sources dépliables, action « vétérinaire ».
private struct AssistantMessageContent: View {
    let message: ChatMessage
    let onOpenVet: () -> Void

    @State private var showsSources = false

    private var response: AssistantResponse? { message.response }
    private var isUrgent: Bool { response?.isUrgent == true }
    private var vetAction: AssistantResponse.SuggestedAction? {
        response?.suggestedActions.first { $0.actionId == "open_vet_handoff" }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            VStack(alignment: .leading, spacing: 10) {
                if isUrgent {
                    Label("Urgence", systemImage: "exclamationmark.triangle.fill")
                        .font(.custom("Inter-SemiBold", size: 14, relativeTo: .subheadline))
                        .foregroundStyle(Color.Pawrise.alert)
                }
                Text(message.text)
                    .font(Font.Pawrise.body)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                if let citations = response?.citations, !citations.isEmpty {
                    sources(citations)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(isUrgent ? Color.Pawrise.alertSurface : Color.Pawrise.card, in: .assistantBubble)
            .overlay {
                UnevenRoundedRectangle.assistantBubble
                    .strokeBorder(isUrgent ? Color.Pawrise.alert.opacity(0.4) : Color.Pawrise.border, lineWidth: 1)
            }

            if let vetAction {
                Button(action: onOpenVet) {
                    Label(vetAction.label, systemImage: "stethoscope")
                        .font(.custom("Inter-SemiBold", size: 15, relativeTo: .subheadline))
                        .foregroundStyle(Color.Pawrise.onPrimary)
                        .padding(.horizontal, 16)
                        .frame(minHeight: 40)
                        .background(isUrgent ? Color.Pawrise.alert : Color.Pawrise.primary, in: .capsule)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func sources(_ citations: [AssistantResponse.Citation]) -> some View {
        DisclosureGroup(isExpanded: $showsSources) {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(citations.enumerated()), id: \.offset) { _, citation in
                    Text(Self.prefix(for: citation.sourceId) + citation.snippet)
                        .font(Font.Pawrise.footnote)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.top, 8)
        } label: {
            Text(citations.count > 1 ? "\(citations.count) sources" : "1 source")
                .font(Font.Pawrise.caption)
                .foregroundStyle(Color.Pawrise.primary)
        }
        .tint(Color.Pawrise.primary)
    }

    static func prefix(for sourceID: String) -> String {
        switch sourceID {
        case "telemetry": "Collier · "
        case "message": "Votre message · "
        default: "Fiche santé · "
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
                    .padding(.horizontal, 16)
                    .frame(height: 40)
                    .background(Color.Pawrise.card, in: .assistantBubble)
                    .overlay { UnevenRoundedRectangle.assistantBubble.strokeBorder(Color.Pawrise.border, lineWidth: 1) }
                if let status {
                    Text(status)
                        .font(Font.Pawrise.caption)
                        .italic()
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .transition(.opacity)
                }
            }
            .animation(.easeOut(duration: 0.2), value: status)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(status ?? "Pawrise prépare sa réponse")
    }
}

private struct TypingDots: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let time = context.date.timeIntervalSinceReferenceDate
            HStack(spacing: 5) {
                ForEach(0..<3, id: \.self) { index in
                    let phase = reduceMotion ? 0 : sin(time * 6 - Double(index) * 0.8)
                    Circle()
                        .fill(Color.Pawrise.textMuted)
                        .frame(width: 8, height: 8)
                        .offset(y: -2.5 * max(phase, 0))
                        .opacity(0.55 + 0.45 * max(phase, 0))
                }
            }
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
                .font(Font.Pawrise.subheadline)
                .foregroundStyle(Color.Pawrise.alert)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("Réessayer", action: onRetry)
                .font(.custom("Inter-SemiBold", size: 15, relativeTo: .subheadline))
                .foregroundStyle(Color.Pawrise.primary)
                .frame(minHeight: 44)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 4)
        .background(Color.Pawrise.alertSurface, in: .rect(cornerRadius: Radius.control))
    }
}

private struct QuickReplyButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.custom("Inter-SemiBold", size: 16, relativeTo: .callout))
                .foregroundStyle(Color.Pawrise.primary)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(minHeight: 44)
                .background(Color.Pawrise.elevated, in: .capsule)
                .overlay { Capsule().strokeBorder(Color.Pawrise.primary, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }
}
