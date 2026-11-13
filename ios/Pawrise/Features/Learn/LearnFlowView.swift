import SwiftUI

/// Découverte en 5 écrans (Figma 03 à 07). La dernière étape lance le profil du chien.
struct LearnFlowView: View {
    let onFinish: () -> Void

    @State private var index = 0

    private var page: LearnPage { LearnPage.all[index] }
    private var isLast: Bool { index == LearnPage.all.count - 1 }

    var body: some View {
        // Maquette A03–A07 : la ligne « Étape n sur 5 » + retour est centrée sur une rangée de 44 pt sous la zone
        // sûre ; pastilles 4 pt plus bas ; bouton à 16 pt du bas de l’écran.
        VStack(spacing: 0) {
            header
            chips
                .padding(.top, 4)

            TabView(selection: $index) {
                ForEach(LearnPage.all.indices, id: \.self) { pageIndex in
                    LearnPageView(page: LearnPage.all[pageIndex])
                        .tag(pageIndex)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            Button(isLast ? "Connecter le collier" : "Suivant") {
                if isLast { onFinish() } else { go(to: index + 1) }
            }
            .buttonStyle(.pawrisePrimary)
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
        .ignoresSafeArea(.container, edges: .bottom)
        .pawrisePage()
    }

    private var header: some View {
        ZStack {
            Text("Étape \(index + 1) sur \(LearnPage.all.count)")
                .font(Font.Pawrise.inter(13, .semibold, relativeTo: .footnote))
                .foregroundStyle(Color.Pawrise.textMuted)
                .figmaLineHeight(1.35, size: 13, relativeTo: .footnote)
                .accessibilityAddTraits(.isHeader)
            HStack {
                FlowIconButton(asset: "Onboarding/learnback", label: "Retour", action: index > 0 ? { go(to: index - 1) } : nil)
                Spacer()
            }
            .padding(.horizontal, 20)
        }
        .frame(height: 44)
    }

    private var chips: some View {
        HStack(spacing: 5) {
            ForEach(LearnPage.all.indices, id: \.self) { chipIndex in
                chip(chipIndex)
            }
        }
        .frame(maxWidth: .infinity)
    }

    /// Pastille 64 × 28 : étape en cours #0A5C78 / texte blanc, étapes vues #E5F1F7, à venir blanches.
    private func chip(_ chipIndex: Int) -> some View {
        let isCurrent = chipIndex == index
        let background: Color = isCurrent
            ? Color.Pawrise.primary
            : (chipIndex < index ? Color.Pawrise.elevated : Color.Pawrise.card)
        return Button {
            go(to: chipIndex)
        } label: {
            Text(LearnPage.all[chipIndex].chip)
                .font(Font.Pawrise.inter(11, .semibold, relativeTo: .caption2))
                .foregroundStyle(isCurrent ? Color.Pawrise.onPrimary : Color.Pawrise.textMuted)
                .lineLimit(1)
                .frame(minWidth: 64, minHeight: 28)
                .background(background, in: .capsule)
                .contentShape(Rectangle().inset(by: -8))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(LearnPage.all[chipIndex].chip), étape \(chipIndex + 1)")
        .accessibilityAddTraits(isCurrent ? .isSelected : [])
    }

    private func go(to newIndex: Int) {
        withAnimation(.easeInOut(duration: 0.25)) { index = newIndex }
    }
}

/// Icône 96 pt, titre Bricolage 700 26 (largeur 342), texte Inter 15 (largeur 320), écarts 16, centrés.
private struct LearnPageView: View {
    let page: LearnPage

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                AssetIconBadge(asset: page.icon)
                FigmaText.display(page.title, size: 26, alignment: .center)
                Text(page.text)
                    .font(Font.Pawrise.body)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .figmaLineHeight(1.35, size: 15)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: 320)
            }
            .padding(.horizontal, 24)
            .frame(maxWidth: .infinity)
            .containerRelativeFrame(.vertical, alignment: .center) { length, _ in length }
            // La maquette centre ce bloc dans la zone 104–764 ; sous la barre d’état réelle la zone disponible
            // est plus basse : on remonte le bloc de 8 pt pour retrouver la position de la maquette.
            .offset(y: -8)
        }
        .scrollBounceBehavior(.basedOnSize)
    }
}

struct LearnPage: Identifiable, Sendable {
    let chip: String
    /// Icône vectorielle de la maquette (Resources/Onboarding.xcassets).
    let icon: String
    let title: String
    let text: String

    var id: String { chip }

    // Textes au caractère près de la maquette (apostrophes droites dans A03–A07).
    static let all: [LearnPage] = [
        LearnPage(
            chip: "App",
            icon: "Onboarding/paw",
            title: "Pawrise veille sur votre chien",
            text: "Le collier mesure sa santé et indique où il se trouve. Vous consultez tout dans l'application."
        ),
        LearnPage(
            chip: "Collier",
            icon: "Onboarding/signal",
            title: "Allumez le collier",
            text: "Maintenez le bouton du boîtier jusqu'au voyant. Le collier se réveille et se prépare à transmettre."
        ),
        LearnPage(
            chip: "Mesures",
            icon: "Onboarding/heart",
            title: "Les mesures arrivent",
            text: "Rythme cardiaque, activité, sommeil et température s'affichent dans Santé dès que le collier envoie ses données."
        ),
        LearnPage(
            chip: "Alerte",
            icon: "Onboarding/alert",
            title: "Un rendez-vous en cas de danger",
            text: "Si une alerte s'affiche, ouvrez Rendez-vous et réservez un vétérinaire pour votre chien."
        ),
        LearnPage(
            chip: "Position",
            icon: "Onboarding/map",
            title: "Suivez sa position",
            text: "L'onglet Localisation montre où se trouve votre chien, pour le retrouver rapidement."
        ),
    ]
}
