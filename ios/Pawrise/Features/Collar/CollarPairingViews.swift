import AVFoundation
import SwiftUI
import VisionKit

// MARK: - Associer le collier (Figma 17)

/// Maquette A17 : question, puis bouton principal, mention, bouton secondaire, mention, tous écartés de 24.
struct CollarIntroView: View {
    let dogName: String
    let onScan: () -> Void
    let onManual: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                FlowQuestion(
                    title: "Connectons le collier de \(dogName)",
                    hint: "Chaque chien Pawrise a son propre collier. Le QR code se trouve directement sur le boîtier du collier."
                )
                Button("Scanner le QR code", action: onScan)
                    .buttonStyle(.pawrisePrimary)
                    .accessibilityHint("Recommandé · rapide")
                caption("Recommandé · rapide")
                Button("Saisir le code manuellement", action: onManual)
                    .buttonStyle(.pawriseSecondary)
                    .accessibilityHint("Code inscrit sous le collier")
                caption("Code inscrit sous le collier")
            }
            .padding(.horizontal, 20)
            .padding(.top, 26)
            .padding(.bottom, 24)
        }
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .font(Font.Pawrise.inter(12.5, relativeTo: .footnote))
            .foregroundStyle(Color.Pawrise.textMuted)
            .figmaLineHeight(1.35, size: 12.5, relativeTo: .footnote)
            .accessibilityHidden(true)
    }
}

/// Bloc centré des écrans 18 à 20 : la maquette le centre dans la zone 80–764 ; sous la barre d’état réelle
/// la zone commence 27 pt plus bas, d’où 27 pt de marge basse pour garder le même centre.
private struct CollarCenteredContent<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                content
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 27)
            .frame(maxWidth: .infinity)
            .containerRelativeFrame(.vertical, alignment: .center) { length, _ in length }
        }
        .scrollBounceBehavior(.basedOnSize)
    }
}

// MARK: - Scanner le QR (Figma 18)

struct CollarScanView: View {
    let onCode: (String) -> Void
    let onManual: () -> Void

    private enum CameraState { case checking, ready, unsupported, denied }

    @State private var cameraState: CameraState = .checking
    @State private var invalidPayloadMessage: String?
    @State private var hasDetectedCode = false
    @Environment(\.openURL) private var openURL
    @ScaledMetric(relativeTo: .body) private var frameSize: CGFloat = 256

    // Maquette A18 : titre Bricolage 700 22, consigne Inter 13,5, cadre 256 rayon 24 (fond #E5F1F7, bordure
    // #0A5C78 1 pt, QR 120), action ; écarts 24, bloc centré.
    var body: some View {
        CollarCenteredContent {
            Text("Scannez le QR code")
                .font(Font.Pawrise.display(22, relativeTo: .title))
                .foregroundStyle(Color.Pawrise.textPrimary)
                .multilineTextAlignment(.center)
                .figmaLineHeight(1.35, size: 22, family: .display, relativeTo: .title)
                .accessibilityAddTraits(.isHeader)
            Text("Placez le QR code du collier dans le cadre.")
                .font(Font.Pawrise.inter(13.5, relativeTo: .subheadline))
                .foregroundStyle(Color.Pawrise.textMuted)
                .multilineTextAlignment(.center)
                .figmaLineHeight(1.35, size: 13.5, relativeTo: .subheadline)

            viewfinder

            if let invalidPayloadMessage {
                ErrorBanner(message: invalidPayloadMessage)
            }

            stateMessage
        }
        .task { await resolveCameraState() }
    }

    @ViewBuilder
    private var viewfinder: some View {
        let shape = RoundedRectangle(cornerRadius: 24, style: .circular)
        ZStack {
            if cameraState == .ready {
                QRCodeScannerView(onPayload: handle)
                    .accessibilityLabel("Caméra : visez le QR code du collier")
            } else {
                Color.Pawrise.elevated
                Image("Onboarding/qr")
                    .resizable()
                    .frame(width: 120, height: 120)
                    .accessibilityHidden(true)
                if cameraState == .checking {
                    ProgressView()
                }
            }
        }
        .frame(width: frameSize, height: frameSize)
        .clipShape(shape)
        .overlay { shape.strokeBorder(Color.Pawrise.primary, lineWidth: 1) }
    }

    @ViewBuilder
    private var stateMessage: some View {
        switch cameraState {
        case .checking:
            EmptyView()
        case .ready:
            Button("Saisir le code manuellement", action: onManual)
                .buttonStyle(.pawriseCompactLink)
        case .unsupported:
            VStack(spacing: 16) {
                Text("Le scan n’est pas disponible sur cet appareil. Saisissez le code inscrit sous le collier.")
                    .font(Font.Pawrise.inter(13.5, relativeTo: .subheadline))
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .figmaLineHeight(1.35, size: 13.5, relativeTo: .subheadline)
                    .fixedSize(horizontal: false, vertical: true)
                // Même bouton que « QR code détecté » de la maquette : 230 pt de large.
                Button("Saisir le code manuellement", action: onManual)
                    .buttonStyle(.pawrisePrimary)
                    .frame(width: 230)
            }
        case .denied:
            VStack(spacing: 12) {
                Text("Pawrise n’a pas accès à la caméra. Autorisez-la dans les Réglages pour scanner le QR code, ou saisissez le code inscrit sous le collier.")
                    .font(Font.Pawrise.inter(13.5, relativeTo: .subheadline))
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Button("Ouvrir les Réglages") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                }
                .buttonStyle(.pawrisePrimary)
                Button("Saisir le code manuellement", action: onManual)
                    .buttonStyle(.pawriseSecondary)
            }
        }
    }

    private func handle(_ payload: String) {
        guard !hasDetectedCode else { return }
        if let code = CollarQRCode.code(from: payload) {
            hasDetectedCode = true
            invalidPayloadMessage = nil
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            onCode(code)
        } else {
            invalidPayloadMessage = "Ce QR code n’est pas celui d’un collier Pawrise. Visez le QR code imprimé sur le boîtier."
        }
    }

    private func resolveCameraState() async {
        guard DataScannerViewController.isSupported else {
            cameraState = .unsupported
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            break
        case .notDetermined:
            guard await AVCaptureDevice.requestAccess(for: .video) else {
                cameraState = .denied
                return
            }
        case .denied, .restricted:
            cameraState = .denied
            return
        @unknown default:
            cameraState = .denied
            return
        }
        cameraState = QRCodeScannerView.isAvailable ? .ready : .unsupported
    }
}

// MARK: - Saisie manuelle

struct CollarManualEntryView: View {
    @Bindable var model: AddDogFlowModel
    let onSubmit: () -> Void
    @FocusState private var isFocused: Bool

    private var formatError: String? {
        let normalized = Collar.normalize(model.collarCode)
        guard normalized.count >= 9, !model.isCollarCodeValid else { return nil }
        return "Le code commence par PW- suivi de 6 lettres ou chiffres."
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                FlowQuestion(
                    title: "Saisir le code d’activation",
                    hint: "Format attendu : PW- suivi de 6 lettres ou chiffres. Le code est inscrit sous le collier."
                )
                LabeledField(label: "Code du collier", error: formatError, borderColor: Color.Pawrise.borderStrong) {
                    TextField("", text: $model.collarCode, prompt: Text("PW-ABC123"))
                        .font(Font.Pawrise.mono(20))
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .keyboardType(.asciiCapable)
                        .submitLabel(.go)
                        .focused($isFocused)
                        .onSubmit { if model.isCollarCodeValid { onSubmit() } }
                        .onChange(of: model.collarCode) { _, value in
                            let upper = String(value.uppercased().prefix(12))
                            if upper != value { model.collarCode = upper }
                            model.collarCodeDidChange()
                        }
                }
                if let error = model.collarError {
                    ErrorBanner(message: error)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 26)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.immediately)
        .onAppear { isFocused = model.collarError == nil }
    }
}

// MARK: - Activation en cours (Figma 19)

/// Maquette A19 : anneau 80 pt (trait 4 pt #C1D4E0) avec l’icône signal 28, titre Bricolage 700 20,
/// texte Inter 13,5 ; écarts 24. Un arc #0A5C78 tourne sur l’anneau tant que l’activation dure.
struct CollarActivatingView: View {
    let dogName: String
    @State private var isSpinning = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        CollarCenteredContent {
            ZStack {
                Circle()
                    .strokeBorder(Color.Pawrise.borderStrong, lineWidth: 4)
                Circle()
                    .inset(by: 2)
                    .trim(from: 0, to: 0.25)
                    .stroke(Color.Pawrise.primary, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(isSpinning ? 360 : 0))
                    .opacity(reduceMotion ? 0 : 1)
                Image("Onboarding/signal28")
                    .resizable()
                    .frame(width: 28, height: 28)
            }
            .frame(width: 80, height: 80)
            .accessibilityHidden(true)

            Text("Activation du collier…")
                .font(Font.Pawrise.display(20, relativeTo: .title2))
                .foregroundStyle(Color.Pawrise.textPrimary)
                .multilineTextAlignment(.center)
                .figmaLineHeight(1.35, size: 20, family: .display, relativeTo: .title2)
            FigmaText.body("Nous connectons le collier de \(dogName) à Pawrise. Un instant.", size: 13.5, alignment: .center)
        }
        .accessibilityElement(children: .combine)
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.linear(duration: 0.9).repeatForever(autoreverses: false)) {
                isSpinning = true
            }
        }
    }
}

// MARK: - Collier activé (Figma 20)

/// Maquette A20 : photo ronde 112, coche 28 (#0E7C74), titre Bricolage 700 24, texte Inter 14 sur 256 pt ;
/// écarts 24, bloc centré.
struct CollarSuccessView: View {
    let dogName: String
    let photo: Dog.Photo?
    let saveError: String?

    var body: some View {
        CollarCenteredContent {
            DogAvatar(photo: photo, size: 112)
            Image("Onboarding/check")
                .resizable()
                .frame(width: 28, height: 28)
                .accessibilityHidden(true)
            Text("Le collier de \(dogName) est activé")
                .font(Font.Pawrise.display(24, relativeTo: .title))
                .foregroundStyle(Color.Pawrise.textPrimary)
                .multilineTextAlignment(.center)
                .figmaLineHeight(1.35, size: 24, family: .display, relativeTo: .title)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("\(dogName) est maintenant connecté à Pawrise. Sa localisation et sa santé sont suivies en temps réel.")
                .font(Font.Pawrise.inter(14, relativeTo: .subheadline))
                .foregroundStyle(Color.Pawrise.textMuted)
                .multilineTextAlignment(.center)
                .figmaLineHeight(1.35, size: 14, relativeTo: .subheadline)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: 256)
            if let saveError {
                ErrorBanner(message: saveError)
            }
        }
    }
}
