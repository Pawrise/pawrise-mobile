import AVFoundation
import SwiftUI
import VisionKit

// MARK: - Associer le collier (Figma 17)

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
                option("Scanner le QR code", caption: "Recommandé · rapide", style: .primary, action: onScan)
                option("Saisir le code manuellement", caption: "Code inscrit sous le collier", style: .secondary, action: onManual)
            }
            .padding(.horizontal, Spacing.page)
            .padding(.top, 20)
            .padding(.bottom, 24)
        }
    }

    private enum OptionStyle { case primary, secondary }

    @ViewBuilder
    private func option(_ title: String, caption: String, style: OptionStyle, action: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            switch style {
            case .primary:
                Button(title, action: action).buttonStyle(.pawrisePrimary)
            case .secondary:
                Button(title, action: action).buttonStyle(.pawriseSecondary)
            }
            Text(caption)
                .font(Font.Pawrise.subheadline)
                .foregroundStyle(Color.Pawrise.textMuted)
                .accessibilityHidden(true)
        }
        .accessibilityElement(children: .contain)
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
    @ScaledMetric(relativeTo: .body) private var frameSize: CGFloat = 250

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(spacing: 10) {
                    Text("Scannez le QR code")
                        .font(Font.Pawrise.display(24, relativeTo: .title))
                        .foregroundStyle(Color.Pawrise.textPrimary)
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Text("Placez le QR code du collier dans le cadre.")
                        .font(Font.Pawrise.subheadline)
                        .foregroundStyle(Color.Pawrise.textMuted)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 40)

                viewfinder

                if let invalidPayloadMessage {
                    ErrorBanner(message: invalidPayloadMessage)
                }

                stateMessage
            }
            .padding(.horizontal, Spacing.page)
            .padding(.bottom, 24)
            .frame(maxWidth: .infinity)
        }
        .task { await resolveCameraState() }
    }

    @ViewBuilder
    private var viewfinder: some View {
        let shape = RoundedRectangle(cornerRadius: Radius.card)
        ZStack {
            if cameraState == .ready {
                QRCodeScannerView(onPayload: handle)
                    .accessibilityLabel("Caméra : visez le QR code du collier")
            } else {
                Color.Pawrise.elevated
                Image(systemName: "qrcode")
                    .font(.system(size: frameSize * 0.36, weight: .regular))
                    .foregroundStyle(Color.Pawrise.ink)
                    .accessibilityHidden(true)
                if cameraState == .checking {
                    ProgressView()
                }
            }
        }
        .frame(width: frameSize, height: frameSize)
        .clipShape(shape)
        .overlay { shape.strokeBorder(Color.Pawrise.primary, lineWidth: 1.5) }
    }

    @ViewBuilder
    private var stateMessage: some View {
        switch cameraState {
        case .checking:
            EmptyView()
        case .ready:
            Button("Saisir le code manuellement", action: onManual)
                .buttonStyle(.pawriseLink)
        case .unsupported:
            VStack(spacing: 16) {
                Text("Le scan n'est pas disponible sur cet appareil. Saisissez le code inscrit sous le collier.")
                    .font(Font.Pawrise.subheadline)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Button("Saisir le code manuellement", action: onManual)
                    .buttonStyle(.pawrisePrimary)
            }
        case .denied:
            VStack(spacing: 12) {
                Text("Pawrise n'a pas accès à la caméra. Autorisez-la dans les Réglages pour scanner le QR code, ou saisissez le code inscrit sous le collier.")
                    .font(Font.Pawrise.subheadline)
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
            invalidPayloadMessage = "Ce QR code n'est pas celui d'un collier Pawrise. Visez le QR code imprimé sur le boîtier."
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
                    title: "Saisir le code d'activation",
                    hint: "Format attendu : PW- suivi de 6 lettres ou chiffres. Le code est inscrit sous le collier."
                )
                LabeledField(label: "Code du collier", error: formatError) {
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
            .padding(.horizontal, Spacing.page)
            .padding(.top, 20)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear { isFocused = model.collarError == nil }
    }
}

// MARK: - Activation en cours (Figma 19)

struct CollarActivatingView: View {
    let dogName: String
    @State private var isSpinning = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            ZStack {
                Circle()
                    .stroke(Color.Pawrise.border, lineWidth: 3)
                Circle()
                    .trim(from: 0, to: 0.25)
                    .stroke(Color.Pawrise.primary, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                    .rotationEffect(.degrees(isSpinning ? 360 : 0))
                    .opacity(reduceMotion ? 0 : 1)
                Image(systemName: "cellularbars")
                    .font(.system(size: 26, weight: .medium))
                    .foregroundStyle(Color.Pawrise.primary)
            }
            .frame(width: 80, height: 80)
            .accessibilityHidden(true)
            .padding(.bottom, 8)

            Text("Activation du collier…")
                .font(Font.Pawrise.title2)
                .foregroundStyle(Color.Pawrise.textPrimary)
                .multilineTextAlignment(.center)
            Text("Nous connectons le collier de \(dogName) à Pawrise. Un instant.")
                .font(Font.Pawrise.callout)
                .foregroundStyle(Color.Pawrise.textMuted)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
            Spacer()
        }
        .padding(.horizontal, Spacing.page + 12)
        .frame(maxWidth: .infinity)
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

struct CollarSuccessView: View {
    let dogName: String
    let photo: Dog.Photo?
    let saveError: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                DogAvatar(photo: photo, size: 112)
                    .padding(.top, 48)
                Image(systemName: "checkmark")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(Color.Pawrise.good)
                    .accessibilityHidden(true)
                Text("Le collier de \(dogName) est activé")
                    .font(Font.Pawrise.title)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                Text("\(dogName) est maintenant connecté à Pawrise. Sa localisation et sa santé sont suivies en temps réel.")
                    .font(Font.Pawrise.callout)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 24)
                if let saveError {
                    ErrorBanner(message: saveError)
                }
            }
            .padding(.horizontal, Spacing.page)
            .padding(.bottom, 24)
            .frame(maxWidth: .infinity)
        }
    }
}
