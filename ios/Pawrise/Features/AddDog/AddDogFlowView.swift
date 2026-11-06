import SwiftUI

/// Profil du chien (Figma 08 à 16) puis association du collier (Figma 17 à 20).
/// Le chien n'est ajouté au compte qu'à la toute fin, ce qui bascule `RootView` sur les onglets.
struct AddDogFlowView: View {
    let onFinish: () -> Void
    let onClose: () -> Void

    @Environment(AppEnvironment.self) private var environment
    @State private var model = AddDogFlowModel()

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, 8)
                .padding(.top, 4)

            content
                .id(contentID)
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .offset(y: 12)),
                    removal: .opacity
                ))
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            footer
        }
        .animation(.easeOut(duration: 0.25), value: contentID)
        .pawrisePage()
    }

    private var contentID: String {
        model.phase == .profile ? "profile-\(model.step.rawValue)" : "collar-\(model.phase)"
    }

    // MARK: En-tête

    private var header: some View {
        FlowHeader(
            onBack: model.canGoBack ? { model.goBack() } : nil,
            onClose: onClose
        ) {
            if model.phase == .profile {
                StepProgressBar(total: AddDogFlowModel.Step.allCases.count, current: model.step.number)
            } else {
                Text("Collier Pawrise")
                    .font(Font.Pawrise.footnote)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .accessibilityAddTraits(.isHeader)
            }
        }
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
                EmptyView()
            }
        }
        .padding(.horizontal, Spacing.page)
        .padding(.top, 12)
        .padding(.bottom, 16)
    }

    // MARK: Actions

    private func activate(_ code: String) {
        let existing = Set(environment.dogs.dogs.compactMap { $0.collar.map { Collar.normalize($0.code) } })
        Task { await model.activate(code: code, using: environment.collars, existingCodes: existing) }
    }

    private func finish() {
        Task {
            if await model.save(to: environment.dogs) {
                onFinish()
            }
        }
    }
}

/// Titre de question + aide, alignés à gauche comme dans le Figma.
struct FlowQuestion: View {
    let title: String
    var hint: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(Font.Pawrise.display(26, relativeTo: .title))
                .foregroundStyle(Color.Pawrise.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if let hint {
                Text(hint)
                    .font(Font.Pawrise.subheadline)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension View {
    /// Cadre de saisie blanc arrondi (même rendu que `LabeledField`, sans libellé).
    func pawriseFieldBox(isInvalid: Bool = false) -> some View {
        font(Font.Pawrise.body)
            .foregroundStyle(Color.Pawrise.textPrimary)
            .padding(.horizontal, 16)
            .frame(minHeight: 54)
            .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
            .overlay {
                RoundedRectangle(cornerRadius: Radius.control)
                    .strokeBorder(isInvalid ? Color.Pawrise.alert : Color.Pawrise.border, lineWidth: 1)
            }
    }
}
