import Foundation

/// Formulaire « Créer un compte » : règles de validation, testées sans interface.
/// « Continuer » reste désactivé tant que `isValid` est faux ; l'erreur d'un champ n'est affichée qu'une fois
/// le champ quitté (voir `SignupView`).
struct SignupForm: Equatable {
    enum Field: Hashable, CaseIterable { case firstName, lastName, email, phone, password, terms }

    var firstName = ""
    var lastName = ""
    var email = ""
    /// Facultatif ; s'il est rempli, il doit être un numéro valide (envoyé en E.164).
    var phone = ""
    var password = ""
    var acceptsTerms = false

    func error(for field: Field) -> String? {
        switch field {
        case .firstName:
            firstName.trimmingCharacters(in: .whitespaces).isEmpty ? "Indiquez votre prénom." : nil
        case .lastName:
            lastName.trimmingCharacters(in: .whitespaces).isEmpty ? "Indiquez votre nom." : nil
        case .email:
            CredentialRules.isValidEmail(email) ? nil : "Saisissez une adresse e-mail valide, par exemple alex@exemple.fr."
        case .phone:
            isPhoneEmpty || e164Phone != nil ? nil : "Saisissez un numéro valide, par exemple 06 12 34 56 78."
        case .password:
            CredentialRules.isValidPassword(password)
                ? nil : "Le mot de passe doit faire au moins \(CredentialRules.passwordMinLength) caractères."
        case .terms:
            acceptsTerms ? nil : "Acceptez les conditions d’utilisation pour continuer."
        }
    }

    var isValid: Bool { Field.allCases.allSatisfy { error(for: $0) == nil } }

    private var isPhoneEmpty: Bool { phone.trimmingCharacters(in: .whitespaces).isEmpty }

    /// Numéro à envoyer au serveur (E.164), nil si le champ est vide ou invalide.
    var e164Phone: String? { isPhoneEmpty ? nil : CredentialRules.e164Phone(phone) }
}

/// Formulaire « Connexion » : e-mail valide et mot de passe non vide.
struct LoginForm: Equatable {
    var email = ""
    var password = ""

    var isValid: Bool { CredentialRules.isValidEmail(email) && !password.isEmpty }
}
