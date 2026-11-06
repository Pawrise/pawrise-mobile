import Observation

/// Les cinq onglets de la barre du bas (Figma « Homepage · Santé »).
enum AppTab: String, CaseIterable, Identifiable, Sendable {
    case location, chat, health, appointments, settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .location: "Localisation"
        case .chat: "Chat IA"
        case .health: "Pawrise"
        case .appointments: "Rendez-vous"
        case .settings: "Paramètres"
        }
    }

    /// Symbole SF de l'onglet ; `nil` pour l'onglet central, dessiné avec la fleur Pawrise.
    var systemImage: String? {
        switch self {
        case .location: "map"
        case .chat: "bubble.left"
        case .health: nil
        case .appointments: "doc.text"
        case .settings: "gearshape"
        }
    }
}

/// Onglet sélectionné, partagé dans l'environnement pour que les écrans enfants puissent changer d'onglet
/// (« Contacter un vétérinaire » → Rendez-vous, « Voir les indicateurs » → Pawrise).
@MainActor
@Observable
final class TabRouter {
    var selection: AppTab

    init(selection: AppTab = .health) {
        self.selection = selection
    }

    func open(_ tab: AppTab) {
        selection = tab
    }
}
