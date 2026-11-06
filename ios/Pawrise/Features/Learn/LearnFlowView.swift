import SwiftUI

/// Découverte en 5 écrans (Figma 03 à 07). La dernière étape lance le profil du chien.
struct LearnFlowView: View {
    let onFinish: () -> Void

    @State private var index = 0

    private var page: LearnPage { LearnPage.all[index] }
    private var isLast: Bool { index == LearnPage.all.count - 1 }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(onBack: index > 0 ? { go(to: index - 1) } : nil) {
                Text("Étape \(index + 1) sur \(LearnPage.all.count)")
                    .font(Font.Pawrise.footnote)
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .accessibilityAddTraits(.isHeader)
            }
            .padding(.horizontal, 8)
            .padding(.top, 4)

            chips
                .padding(.top, 12)

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
            .padding(.horizontal, Spacing.page)
            .padding(.top, 12)
            .padding(.bottom, 16)
        }
        .pawrisePage()
    }

    private var chips: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(LearnPage.all.indices, id: \.self) { chipIndex in
                        chip(chipIndex).id(chipIndex)
                    }
                }
                .padding(.horizontal, 16)
            }
            .onChange(of: index) { _, newValue in
                withAnimation { proxy.scrollTo(newValue, anchor: .center) }
            }
        }
    }

    private func chip(_ chipIndex: Int) -> some View {
        let isCurrent = chipIndex == index
        let background: Color = isCurrent
            ? Color.Pawrise.primary
            : (chipIndex < index ? Color.Pawrise.elevated : Color.Pawrise.card)
        return Button {
            go(to: chipIndex)
        } label: {
            Text(LearnPage.all[chipIndex].chip)
                .font(Font.Pawrise.footnote)
                .foregroundStyle(isCurrent ? Color.Pawrise.onPrimary : Color.Pawrise.textPrimary)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .frame(minHeight: 36)
                .background(background, in: .capsule)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(LearnPage.all[chipIndex].chip), étape \(chipIndex + 1)")
        .accessibilityAddTraits(isCurrent ? .isSelected : [])
    }

    private func go(to newIndex: Int) {
        withAnimation(.easeInOut(duration: 0.25)) { index = newIndex }
    }
}

private struct LearnPageView: View {
    let page: LearnPage

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                IconBadge(systemImage: page.symbol)
                Text(page.title)
                    .font(Font.Pawrise.display(26, relativeTo: .title))
                    .foregroundStyle(Color.Pawrise.textPrimary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                Text(page.text)
                    .font(Font.Pawrise.callout)
                    .foregroundStyle(Color.Pawrise.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, Spacing.page + 8)
            .padding(.vertical, 24)
            .frame(maxWidth: .infinity)
            .containerRelativeFrame(.vertical, alignment: .center) { length, _ in length }
        }
        .scrollBounceBehavior(.basedOnSize)
    }
}

struct LearnPage: Identifiable, Sendable {
    let chip: String
    let symbol: String
    let title: String
    let text: String

    var id: String { chip }

    static let all: [LearnPage] = [
        LearnPage(
            chip: "App",
            symbol: "pawprint",
            title: "Pawrise veille sur votre chien",
            text: "Le collier mesure sa santé et indique où il se trouve. Vous consultez tout dans l'application."
        ),
        LearnPage(
            chip: "Collier",
            symbol: "cellularbars",
            title: "Allumez le collier",
            text: "Maintenez le bouton du boîtier jusqu'au voyant. Le collier se réveille et se prépare à transmettre."
        ),
        LearnPage(
            chip: "Mesures",
            symbol: "heart",
            title: "Les mesures arrivent",
            text: "Rythme cardiaque, activité, sommeil et température s'affichent dans Santé dès que le collier envoie ses données."
        ),
        LearnPage(
            chip: "Alerte",
            symbol: "exclamationmark.triangle",
            title: "Un rendez-vous en cas de danger",
            text: "Si une alerte s'affiche, ouvrez Rendez-vous et réservez un vétérinaire pour votre chien."
        ),
        LearnPage(
            chip: "Position",
            symbol: "map",
            title: "Suivez sa position",
            text: "L'onglet Localisation montre où se trouve votre chien, pour le retrouver rapidement."
        ),
    ]
}
