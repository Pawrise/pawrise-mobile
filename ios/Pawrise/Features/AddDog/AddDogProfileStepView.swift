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
            .padding(.horizontal, Spacing.page)
            .padding(.top, 20)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    @ViewBuilder
    private var stepContent: some View {
        let name = model.displayName
        switch model.step {
        case .photo:
            FlowQuestion(title: "Ajoutez une photo de votre chien")
            PhotoStep(model: model)
        case .name:
            FlowQuestion(title: "Comment s'appelle votre chien ?")
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
                title: "Y a-t-il quelque chose d'important à savoir sur la santé de \(name) ?",
                hint: "Optionnel — conditions connues, traitements, allergies, antécédents… Ces informations pourront aider votre vétérinaire plus tard."
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

    var body: some View {
        // Le libellé de PhotosPicker n'est pas isolé au MainActor : on capture des valeurs.
        let photo = model.photo
        let size = avatarSize
        let loading = isLoading
        VStack(spacing: 16) {
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
                    Image(systemName: "plus")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(Color.Pawrise.onPrimary)
                        .frame(width: 44, height: 44)
                        .background(Color.Pawrise.primary, in: .circle)
                        .overlay { Circle().strokeBorder(Color.Pawrise.page, lineWidth: 3) }
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(model.photo == nil ? "Ajouter une photo depuis la photothèque" : "Changer la photo")

            if let errorMessage { ErrorBanner(message: errorMessage) }

            Text("ou choisissez un exemple")
                .font(Font.Pawrise.subheadline)
                .foregroundStyle(Color.Pawrise.textMuted)

            HStack(spacing: 12) {
                ForEach(Array(DogCatalog.samplePhotos.enumerated()), id: \.element) { index, url in
                    let isSelected = model.photo == .remote(url)
                    Button {
                        model.photo = .remote(url)
                    } label: {
                        DogAvatar(photo: .remote(url), size: thumbnailSize, cornerRadius: 14)
                            .padding(3)
                            .overlay {
                                RoundedRectangle(cornerRadius: 17)
                                    .strokeBorder(isSelected ? Color.Pawrise.primary : .clear, lineWidth: 2)
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Photo d'exemple \(index + 1)")
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
                errorMessage = "Cette photo n'a pas pu être importée. Essayez-en une autre."
                return
            }
            let compressed = await Task.detached(priority: .userInitiated) {
                DogPhotoCompressor.jpeg(from: data)
            }.value
            if let compressed {
                model.photo = .local(compressed)
            } else {
                errorMessage = "Cette photo n'a pas pu être importée. Essayez-en une autre."
            }
        } catch {
            errorMessage = "Cette photo n'a pas pu être importée. Essayez-en une autre."
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

private struct BirthDateStep: View {
    @Bindable var model: AddDogFlowModel

    private var dateBinding: Binding<Date> {
        Binding(
            get: { model.birthDate ?? model.birthDateRange.upperBound },
            set: { model.birthDate = $0 }
        )
    }

    var body: some View {
        HStack(spacing: 12) {
            if model.birthDate == nil {
                Button {
                    let range = model.birthDateRange
                    model.birthDate = Calendar.current.date(byAdding: .year, value: -1, to: range.upperBound) ?? range.upperBound
                } label: {
                    HStack {
                        Text("JJ / MM / AAAA")
                            .foregroundStyle(Color.Pawrise.textMuted)
                        Spacer()
                        Image(systemName: "calendar")
                            .foregroundStyle(Color.Pawrise.primary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Indiquer la date de naissance")
            } else {
                DatePicker(
                    "Date de naissance",
                    selection: dateBinding,
                    in: model.birthDateRange,
                    displayedComponents: .date
                )
                .datePickerStyle(.compact)
                .labelsHidden()
                .environment(\.locale, AddDogFlowModel.frenchLocale)
                Spacer()
                Button {
                    model.birthDate = nil
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 20))
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Effacer la date")
            }
        }
        .pawriseFieldBox()
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
            VStack(spacing: 10) {
                Text(symbol)
                    .font(.system(size: 28, weight: .regular))
                    .foregroundStyle(Color.Pawrise.primary)
                    .accessibilityHidden(true)
                Text(sex.label)
                    .font(Font.Pawrise.headline)
                    .foregroundStyle(Color.Pawrise.textPrimary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(isSelected ? Color.Pawrise.elevated : Color.Pawrise.card, in: .rect(cornerRadius: Radius.tile))
            .overlay {
                RoundedRectangle(cornerRadius: Radius.tile)
                    .strokeBorder(isSelected ? Color.Pawrise.primary : Color.Pawrise.border, lineWidth: isSelected ? 1.5 : 1)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Race

private struct BreedStep: View {
    @Bindable var model: AddDogFlowModel

    var body: some View {
        VStack(spacing: 12) {
            TextField("", text: $model.breedQuery, prompt: Text("Rechercher une race"))
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .accessibilityLabel("Race")
                .pawriseFieldBox()
                .onChange(of: model.breedQuery) { _, _ in model.breedQueryDidChange() }

            ForEach(model.breedSuggestions, id: \.self) { breed in
                let isSelected = model.selectedBreed == breed
                Button {
                    model.selectBreed(breed)
                } label: {
                    HStack {
                        Text(breed)
                            .font(Font.Pawrise.body)
                            .foregroundStyle(Color.Pawrise.textPrimary)
                            .multilineTextAlignment(.leading)
                        Spacer()
                        if isSelected {
                            Image(systemName: "checkmark")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Color.Pawrise.primary)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .frame(minHeight: 54)
                    .background(isSelected ? Color.Pawrise.elevated : Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
                    .overlay {
                        RoundedRectangle(cornerRadius: Radius.control)
                            .strokeBorder(isSelected ? Color.Pawrise.primary : Color.Pawrise.border, lineWidth: 1)
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }

            if model.breedSuggestions.isEmpty, let typed = model.breed {
                Text("« \(typed) » sera enregistré tel quel.")
                    .font(Font.Pawrise.subheadline)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
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

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                TextField("", text: $text, prompt: Text("—"))
                    .keyboardType(keyboard)
                    .focused($isFocused)
                    .accessibilityLabel(accessibilityLabel)
                Text(unit)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .accessibilityHidden(true)
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

private struct MedicalStep: View {
    @Bindable var model: AddDogFlowModel
    @ScaledMetric(relativeTo: .body) private var minHeight: CGFloat = 160

    var body: some View {
        ZStack(alignment: .topLeading) {
            TextEditor(text: $model.medicalNotes)
                .font(Font.Pawrise.body)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .scrollContentBackground(.hidden)
                .padding(.horizontal, 11)
                .padding(.vertical, 8)
                .accessibilityLabel("Informations de santé (facultatif)")
            if model.medicalNotes.isEmpty {
                Text("Écrire ici (facultatif)…")
                    .font(Font.Pawrise.body)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
            }
        }
        .frame(minHeight: minHeight)
        .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.control))
        .overlay {
            RoundedRectangle(cornerRadius: Radius.control)
                .strokeBorder(Color.Pawrise.border, lineWidth: 1)
        }
    }
}

// MARK: - Récapitulatif

private struct SummaryStep: View {
    let model: AddDogFlowModel
    @ScaledMetric(relativeTo: .subheadline) private var labelWidth: CGFloat = 128

    var body: some View {
        VStack(spacing: 20) {
            VStack(spacing: 10) {
                DogAvatar(photo: model.photo, size: 96, cornerRadius: 20)
                Text(model.trimmedName)
                    .font(Font.Pawrise.title)
                    .foregroundStyle(Color.Pawrise.textPrimary)
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
            .padding(.vertical, 6)
            .background(Color.Pawrise.card, in: .rect(cornerRadius: Radius.tile))
            .overlay {
                RoundedRectangle(cornerRadius: Radius.tile)
                    .strokeBorder(Color.Pawrise.border, lineWidth: 1)
            }
        }
    }

    private func row(_ label: String, _ value: String, step: AddDogFlowModel.Step) -> some View {
        Button {
            model.edit(step)
        } label: {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(label)
                    .font(Font.Pawrise.subheadline)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .frame(width: labelWidth, alignment: .leading)
                Text(value)
                    .font(Font.Pawrise.callout)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Modifier")
    }
}
