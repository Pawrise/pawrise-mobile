import PhotosUI
import SwiftUI

/// Une étape du profil du chien (Figma 08 à 16).
struct AddDogProfileStepView: View {
    @Bindable var model: AddDogFlowModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                stepContent
            }
            .padding(.horizontal, 20)
            .padding(.top, 26)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.immediately)
    }

    @ViewBuilder
    private var stepContent: some View {
        let name = model.displayName
        switch model.step {
        case .photo:
            FlowQuestion(title: "Ajoutez une photo de votre chien")
            PhotoStep(model: model)
        case .name:
            FlowQuestion(title: "Comment s’appelle votre chien ?")
            NameStep(model: model)
        case .birthDate:
            FlowQuestion(
                title: "Quand est né \(name) ?",
                hint: "Si vous ne connaissez pas la date exacte, vous pouvez indiquer une date approximative."
            )
            BirthDateStep(model: model)
        case .sex:
            FlowQuestion(title: "\(name) est…")
            SexStep(model: model)
        case .breed:
            FlowQuestion(title: "Quelle est la race de \(name) ?")
            BreedStep(model: model)
        case .weight:
            FlowQuestion(title: "Combien pèse \(name) ?", hint: "Un poids approximatif suffit.")
            MeasurementStep(
                text: $model.weightText,
                unit: "kg",
                accessibilityLabel: "Poids en kilogrammes",
                keyboard: .decimalPad,
                isValid: model.isWeightValid,
                error: "Indiquez un poids entre 0 et \(Int(AddDogFlowModel.maxWeightKg)) kg.",
                sanitize: AddDogFlowModel.sanitizeDecimalInput
            )
        case .height:
            FlowQuestion(
                title: "Quelle est la taille de \(name) ?",
                hint: "Au garrot, en centimètres. Une mesure approximative est acceptée."
            )
            MeasurementStep(
                text: $model.heightText,
                unit: "cm",
                accessibilityLabel: "Taille au garrot en centimètres",
                keyboard: .numberPad,
                isValid: model.isHeightValid,
                error: "Indiquez une taille entre 1 et \(AddDogFlowModel.maxHeightCm) cm.",
                sanitize: AddDogFlowModel.sanitizeIntegerInput
            )
        case .medical:
            FlowQuestion(
                title: "Y a-t-il quelque chose d’important à savoir sur la santé de \(name) ?",
                // Retour à la ligne de la maquette après « traitements, » (Figma coupe là, SwiftUI tiendrait « allergies, »).
                hint: "Optionnel — conditions connues, traitements,\nallergies, antécédents… Ces informations pourront aider votre vétérinaire plus tard."
            )
            MedicalStep(model: model)
        case .summary:
            FlowQuestion(title: "Le profil de \(name)", hint: "Vérifiez les informations avant de continuer.")
            SummaryStep(model: model)
        }
    }
}

// MARK: - Photo

private struct PhotoStep: View {
    @Bindable var model: AddDogFlowModel
    @State private var pickerItem: PhotosPickerItem?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @ScaledMetric(relativeTo: .largeTitle) private var avatarSize: CGFloat = 160
    @ScaledMetric(relativeTo: .body) private var thumbnailSize: CGFloat = 64

    // Maquette A08 : photo ronde 160, bouton « + » 44 en bas à droite, « ou choisissez un exemple »
    // (Inter 12,5), 4 vignettes 64 × 64 rayon 16 écartées de 12, sélection bordée de #0A5C78 (2 pt) ; écarts 20.
    var body: some View {
        // Le libellé de PhotosPicker n’est pas isolé au MainActor : on capture des valeurs.
        let photo = model.photo
        let size = avatarSize
        let loading = isLoading
        VStack(spacing: 20) {
            PhotosPicker(selection: $pickerItem, matching: .images, photoLibrary: .shared()) {
                ZStack(alignment: .bottomTrailing) {
                    DogAvatar(photo: photo, size: size)
                        .overlay {
                            if loading {
                                ProgressView()
                                    .frame(width: size, height: size)
                                    .background(.ultraThinMaterial, in: .circle)
                            }
                        }
                    Image("Onboarding/photoplus")
                        .resizable()
                        .frame(width: 44, height: 44)
                        .offset(x: -1, y: -1)
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(model.photo == nil ? "Ajouter une photo depuis la photothèque" : "Changer la photo")

            if let errorMessage { ErrorBanner(message: errorMessage) }

            Text("ou choisissez un exemple")
                .font(Font.Pawrise.inter(12.5, relativeTo: .footnote))
                .foregroundStyle(Color.Pawrise.textMuted)
                .figmaLineHeight(1.35, size: 12.5, relativeTo: .footnote)

            HStack(spacing: 12) {
                ForEach(Array(SampleDogPhotos.all.enumerated()), id: \.offset) { index, data in
                    let isSelected = model.photo == .local(data)
                    Button {
                        model.photo = .local(data)
                    } label: {
                        DogAvatar(photo: .local(data), size: thumbnailSize, cornerRadius: 16)
                            .overlay {
                                RoundedRectangle(cornerRadius: 16)
                                    .strokeBorder(isSelected ? Color.Pawrise.primary : .clear, lineWidth: 2)
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Photo d’exemple \(index + 1)")
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
        }
        .frame(maxWidth: .infinity)
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task { await load(item) }
        }
    }

    private func load(_ item: PhotosPickerItem) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                errorMessage = "Cette photo n’a pas pu être importée. Essayez-en une autre."
                return
            }
            let compressed = await Task.detached(priority: .userInitiated) {
                DogPhotoCompressor.jpeg(from: data)
            }.value
            if let compressed {
                model.photo = .local(compressed)
            } else {
                errorMessage = "Cette photo n’a pas pu être importée. Essayez-en une autre."
            }
        } catch {
            errorMessage = "Cette photo n’a pas pu être importée. Essayez-en une autre."
        }
    }
}

// MARK: - Nom

private struct NameStep: View {
    @Bindable var model: AddDogFlowModel
    @FocusState private var isFocused: Bool

    var body: some View {
        TextField("", text: $model.name, prompt: Text("Ex. Nala"))
            .textContentType(.name)
            .textInputAutocapitalization(.words)
            .autocorrectionDisabled()
            .submitLabel(.next)
            .focused($isFocused)
            .onSubmit { model.advance() }
            .accessibilityLabel("Nom du chien")
            .pawriseFieldBox()
            .onChange(of: model.name) { _, value in
                if value.count > AddDogFlowModel.nameMaxLength {
                    model.name = String(value.prefix(AddDogFlowModel.nameMaxLength))
                }
            }
            .onAppear { isFocused = true }
    }
}

// MARK: - Naissance

/// Maquette A10 : champ « 12 / 04 / 2023 » (Inter 17). Saisie au pavé numérique, mise en forme automatique.
private struct BirthDateStep: View {
    @Bindable var model: AddDogFlowModel
    @FocusState private var isFocused: Bool

    /// Met en forme à chaque frappe ; la date complète (8 chiffres) ferme le pavé numérique.
    private var text: Binding<String> {
        Binding(
            get: { model.birthDateText },
            set: { value in
                let formatted = AddDogFlowModel.formatBirthDateInput(value)
                model.birthDateText = formatted
                if formatted.count == 14 { isFocused = false }
            }
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            TextField("", text: text, prompt: Text("JJ / MM / AAAA"))
                .keyboardType(.numberPad)
                .textContentType(.none)
                .focused($isFocused)
                .accessibilityLabel("Date de naissance")
                .accessibilityHint("Facultatif. Format jour, mois, année.")
                .pawriseFieldBox(isInvalid: model.showsBirthDateError)
            if model.showsBirthDateError {
                Text("Indiquez une date réelle, au format JJ / MM / AAAA, dans les \(AddDogFlowModel.maxAgeYears) dernières années.")
                    .font(Font.Pawrise.caption)
                    .foregroundStyle(Color.Pawrise.alert)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

// MARK: - Sexe

private struct SexStep: View {
    @Bindable var model: AddDogFlowModel

    var body: some View {
        HStack(spacing: 12) {
            card(.female, symbol: "♀")
            card(.male, symbol: "♂")
        }
    }

    private func card(_ sex: Dog.Sex, symbol: String) -> some View {
        let isSelected = model.sex == sex
        return Button {
            model.sex = sex
        } label: {
            // Maquette A11 : carte 132 pt rayon 24, symbole Inter 36, libellé Inter 600 15, écart 12.
            VStack(spacing: 12) {
                // Inter n’a pas ♀ / ♂ : Figma les dessine avec sa police de secours, Noto Sans Symbols
                // (sous-ensemble embarqué), à 36 pt dans une boîte de 49 pt (interligne 135 %).
                Text(symbol)
                    .font(Font(UIFontMetrics(forTextStyle: .largeTitle).scaledFont(for: PawriseUIFont.symbols(size: 36))))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .frame(height: 49)
                    .offset(y: -3) // Figma place l’encre 3 pt plus haut dans sa boîte de 49 pt
                    .accessibilityHidden(true)
                Text(sex.label)
                    .font(Font.Pawrise.inter(15, .semibold, relativeTo: .headline))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .figmaLineHeight(1.35, size: 15, relativeTo: .headline)
            }
            .frame(maxWidth: .infinity, minHeight: 132)
            .background(isSelected ? Color.Pawrise.elevated : Color.Pawrise.card, in: .rect(cornerRadius: 24))
            .overlay {
                RoundedRectangle(cornerRadius: 24)
                    .strokeBorder(isSelected ? Color.Pawrise.primary : Color.Pawrise.borderStrong, lineWidth: 1)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Race

/// Maquette A12 : champ de recherche, puis suggestions (même cadre ; choisie : fond #E5F1F7, bordure #0A5C78).
private struct BreedStep: View {
    @Bindable var model: AddDogFlowModel

    var body: some View {
        VStack(spacing: 24) {
            TextField("", text: $model.breedQuery, prompt: Text("Rechercher une race"))
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .accessibilityLabel("Race")
                .pawriseFieldBox()
                .onChange(of: model.breedQuery) { _, _ in model.breedQueryDidChange() }

            VStack(spacing: 12) {
                ForEach(model.breedSuggestions, id: \.self) { breed in
                    let isSelected = model.selectedBreed == breed
                    Button {
                        model.selectBreed(breed)
                    } label: {
                        Text(breed)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .pawriseFieldBox(isSelected: isSelected)
                            .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }

                if model.breedSuggestions.isEmpty, let typed = model.breed {
                    Text("« \(typed) » sera enregistré tel quel.")
                        .font(Font.Pawrise.inter(13.5, relativeTo: .subheadline))
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }
}

// MARK: - Poids / Taille

private struct MeasurementStep: View {
    @Binding var text: String
    let unit: String
    let accessibilityLabel: String
    let keyboard: UIKeyboardType
    let isValid: Bool
    let error: String
    let sanitize: (String) -> String
    @FocusState private var isFocused: Bool
    @ScaledMetric(relativeTo: .body) private var unitOffset: CGFloat = 203.3

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Maquette A13 / A14 : valeur à gauche, unité à 221,5 pt du bord du champ (x = 241,5), Inter 17.
            HStack(spacing: 0) {
                TextField("", text: $text, prompt: Text("—"))
                    .keyboardType(keyboard)
                    .focused($isFocused)
                    .accessibilityLabel(accessibilityLabel)
                    .frame(width: unitOffset)
                Text(unit)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .accessibilityHidden(true)
                Spacer(minLength: 0)
            }
            .pawriseFieldBox(isInvalid: !isValid)
            .onChange(of: text) { _, value in
                let cleaned = sanitize(value)
                if cleaned != value { text = cleaned }
            }

            if !isValid {
                Text(error)
                    .font(Font.Pawrise.caption)
                    .foregroundStyle(Color.Pawrise.alert)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .onAppear { isFocused = true }
    }
}

// MARK: - Santé

/// Maquette A15 : zone de 174 pt, texte à 14 pt du haut et 16 pt des bords, Inter 17.
private struct MedicalStep: View {
    @Bindable var model: AddDogFlowModel
    @ScaledMetric(relativeTo: .body) private var minHeight: CGFloat = 174

    var body: some View {
        ZStack(alignment: .topLeading) {
            // TextEditor ajoute 8 pt en haut et 5 pt à gauche autour du texte.
            TextEditor(text: $model.medicalNotes)
                .font(Font.Pawrise.flowField)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .scrollContentBackground(.hidden)
                .padding(.horizontal, 11)
                .padding(.top, 7)
                .accessibilityLabel("Informations de santé (facultatif)")
            if model.medicalNotes.isEmpty {
                Text("Écrire ici (facultatif)…")
                    .font(Font.Pawrise.flowField)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .padding(.horizontal, 16)
                    .padding(.top, 15)
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
            }
        }
        .frame(minHeight: minHeight)
        .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
        .overlay {
            RoundedRectangle(cornerRadius: Radius.control)
                .strokeBorder(Color.Pawrise.borderStrong, lineWidth: 1)
        }
    }
}

// MARK: - Récapitulatif

/// Maquette A16 : photo 96 rayon 16, nom Bricolage 24 (écart 8) ; carte rayon 24 bordure #C1D4E0,
/// 6 lignes de 51 pt (libellé Inter 13 sur 124 pt, valeur Inter 14, chevron 16), marges 16, écart 12.
private struct SummaryStep: View {
    let model: AddDogFlowModel
    @ScaledMetric(relativeTo: .subheadline) private var labelWidth: CGFloat = 124
    @ScaledMetric(relativeTo: .subheadline) private var rowHeight: CGFloat = 51

    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 8) {
                DogAvatar(photo: model.photo, size: 96, cornerRadius: 16)
                Text(model.trimmedName)
                    .font(Font.Pawrise.display(24, relativeTo: .title))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .figmaLineHeight(1.35, size: 24, family: .display, relativeTo: .title)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)

            VStack(spacing: 0) {
                row("Date de naissance", model.formattedBirthDate() ?? "Non renseignée", step: .birthDate)
                row("Sexe", model.sex?.label ?? "Non renseigné", step: .sex)
                row("Race", model.breed ?? "Non renseignée", step: .breed)
                row("Poids", model.formattedWeight() ?? "Non renseigné", step: .weight)
                row("Taille", model.formattedHeight() ?? "Non renseignée", step: .height)
                row("Infos santé", model.trimmedMedicalNotes.isEmpty ? "Aucune" : model.trimmedMedicalNotes, step: .medical)
            }
            .padding(.vertical, 1)
            .background(Color.Pawrise.card, in: .rect(cornerRadius: 24))
            .overlay {
                RoundedRectangle(cornerRadius: 24)
                    .strokeBorder(Color.Pawrise.borderStrong, lineWidth: 1)
            }
        }
    }

    private func row(_ label: String, _ value: String, step: AddDogFlowModel.Step) -> some View {
        Button {
            model.edit(step)
        } label: {
            HStack(spacing: 12) {
                Text(label)
                    .font(Font.Pawrise.inter(13, relativeTo: .subheadline))
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .frame(width: labelWidth, alignment: .leading)
                Text(value)
                    .font(Font.Pawrise.inter(14, relativeTo: .subheadline))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image("Onboarding/rowchevron")
                    .resizable()
                    .frame(width: 16, height: 16)
                    .accessibilityHidden(true)
            }
            // Maquette : chevron (cadre 16 pt) à 5 pt du bord intérieur droit de la carte.
            .padding(.leading, 16)
            .padding(.trailing, 5)
            .frame(minHeight: rowHeight)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Modifier")
    }
}
