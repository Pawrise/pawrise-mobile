import SwiftUI

/// Profil du chien (Figma 08 à 16) puis association du collier (Figma 17 à 20).
/// Le chien n’est ajouté au compte qu’à la toute fin, ce qui bascule `RootView` sur les onglets.
struct AddDogFlowView: View {
    let onFinish: () -> Void
    let onClose: () -> Void

    @Environment(AppEnvironment.self) private var environment
    @State private var model = AddDogFlowModel()
    /// Activation en cours, annulée si l’utilisateur revient en arrière (A19).
    @State private var activation: Task<Void, Never>?

    var body: some View {
        // Maquette A08–A20 : en-tête (rangée de 44 pt sous la zone sûre), contenu, zone d’action de 80 pt
        // en bas de l’écran (bouton à 16 pt du bord, sous la zone sûre comme dans la maquette).
        VStack(spacing: 0) {
            header

            content
                .id(contentID)
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .offset(y: 12)),
                    removal: .opacity
                ))
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            footer
        }
        .ignoresSafeArea(.container, edges: .bottom)
        .animation(.easeOut(duration: 0.25), value: contentID)
        .pawrisePage()
    }

    private var contentID: String {
        model.phase == .profile ? "profile-\(model.step.rawValue)" : "collar-\(model.phase)"
    }

    // MARK: En-tête

    private var header: some View {
        FlowHeader(
            onBack: backAction,
            onClose: onClose
        ) {
            if model.phase == .profile {
                StepProgressBar(total: AddDogFlowModel.Step.allCases.count, current: model.step.number)
            } else {
                Text("Collier Pawrise")
                    .font(Font.Pawrise.inter(13, .semibold, relativeTo: .footnote))
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .figmaLineHeight(1.35, size: 13, relativeTo: .footnote)
                    .accessibilityAddTraits(.isHeader)
            }
        }
    }

    /// Retour : étape précédente ; depuis la photo (première étape), retour à la découverte comme la croix.
    private var backAction: (() -> Void)? {
        if model.canGoBack {
            return {
                if model.phase == .activating { activation?.cancel() }
                model.goBack()
            }
        }
        return model.phase == .profile ? onClose : nil
    }

    // MARK: Contenu

    @ViewBuilder
    private var content: some View {
        switch model.phase {
        case .profile:
            AddDogProfileStepView(model: model)
        case .collarIntro:
            CollarIntroView(
                dogName: model.displayName,
                onScan: { model.startScan() },
                onManual: { model.startManualEntry() }
            )
        case .scan:
            CollarScanView(
                onCode: { code in activate(code) },
                onManual: { model.startManualEntry() }
            )
        case .manual:
            CollarManualEntryView(model: model, onSubmit: { activate(model.collarCode) })
        case .activating:
            CollarActivatingView(dogName: model.displayName)
        case .success:
            CollarSuccessView(dogName: model.displayName, photo: model.photo, saveError: model.saveError)
        }
    }

    // MARK: Pied de page

    @ViewBuilder
    private var footer: some View {
        Group {
            switch model.phase {
            case .profile:
                Button(model.step == .summary ? "Continuer" : "Suivant") { model.advance() }
                    .buttonStyle(.pawrisePrimary)
                    .disabled(!model.canAdvance)
            case .manual:
                Button("Activer le collier") { activate(model.collarCode) }
                    .buttonStyle(.pawrisePrimary)
                    .disabled(!model.isCollarCodeValid)
            case .success:
                Button("Voir le tableau de bord de \(model.displayName)", action: finish)
                    .buttonStyle(.pawrisePrimary(loading: model.isSaving))
                    .disabled(model.isSaving)
            case .collarIntro, .scan, .activating:
                // Zone d’action vide mais réservée (80 pt), comme le cadre « Action principale » de la maquette.
                Color.clear.frame(height: 56)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 16)
        .frame(minHeight: 80, alignment: .top)
    }

    // MARK: Actions

    private func activate(_ code: String) {
        let existing = Set(environment.dogs.dogs.compactMap { $0.collar.map { Collar.normalize($0.code) } })
        activation = Task { await model.activate(code: code, using: environment.collars, existingCodes: existing) }
    }

    private func finish() {
        Task {
            if await model.save(to: environment.dogs) {
                onFinish()
            }
        }
    }
}

/// Titre de question + aide (maquette « Question ») : Bricolage 700 26 et Inter 400 13,5, interligne 135 %, écart 8.
struct FlowQuestion: View {
    let title: String
    var hint: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            FigmaText.display(title, size: 26)
            if let hint {
                Text(hint)
                    .font(Font.Pawrise.inter(13.5, relativeTo: .subheadline))
                    // Figma rend Inter 13,5 environ 2 % plus serré que iOS (mesuré) : mêmes coupures de ligne.
                    .tracking(-0.12)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .figmaLineHeight(1.35, size: 13.5, relativeTo: .subheadline)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension Font.Pawrise {
    /// Saisie des champs du parcours : Inter 400 17.
    static let flowField = inter(17)
}

extension View {
    /// Cadre de saisie du parcours (maquette « Champ ») : 56 pt, rayon 16, bordure #C1D4E0, Inter 17, marges 16.
    func pawriseFieldBox(isInvalid: Bool = false, isSelected: Bool = false) -> some View {
        font(Font.Pawrise.flowField)
            .foregroundStyle(Color.Pawrise.textPrimary)
            .padding(.horizontal, 16)
            .frame(minHeight: 56)
            .background(isSelected ? Color.Pawrise.elevated : Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
            .overlay {
                RoundedRectangle(cornerRadius: Radius.control)
                    .strokeBorder(
                        isInvalid ? Color.Pawrise.alert : (isSelected ? Color.Pawrise.primary : Color.Pawrise.borderStrong),
                        lineWidth: 1
                    )
            }
    }
}
