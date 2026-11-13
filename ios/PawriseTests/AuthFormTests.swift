import Testing
@testable import Pawrise

@Suite("Formulaire d'inscription : « Continuer » actif seulement si tout est valide")
struct SignupFormTests {
    private let complete = SignupForm(
        firstName: "Alex", lastName: "Martin", email: "alex@example.com", phone: "",
        password: "chien-26", acceptsTerms: true
    )

    @Test func emptyFormIsInvalid() {
        let form = SignupForm()
        #expect(!form.isValid)
        for field in SignupForm.Field.allCases where field != .phone {
            #expect(form.error(for: field) != nil, "\(field)")
        }
        #expect(form.error(for: .phone) == nil) // facultatif
    }

    @Test func completeFormIsValid() {
        #expect(complete.isValid)
        #expect(complete.e164Phone == nil)
    }

    @Test(arguments: [
        (SignupForm.Field.firstName, "  "), (.lastName, ""), (.email, "alex@example"),
        (.password, "1234567"), (.phone, "06 12"),
    ])
    func oneInvalidFieldDisablesSubmit(_ field: SignupForm.Field, _ value: String) {
        var form = complete
        switch field {
        case .firstName: form.firstName = value
        case .lastName: form.lastName = value
        case .email: form.email = value
        case .phone: form.phone = value
        case .password: form.password = value
        case .terms: Issue.record("cas non prévu")
        }
        #expect(!form.isValid)
        #expect(form.error(for: field) != nil)
    }

    @Test func termsAreRequired() {
        var form = complete
        form.acceptsTerms = false
        #expect(!form.isValid)
        #expect(form.error(for: .terms) == "Acceptez les conditions d’utilisation pour continuer.")
    }

    @Test func validPhoneIsSentAsE164() {
        var form = complete
        form.phone = "06 12 34 56 78"
        #expect(form.isValid)
        #expect(form.e164Phone == "+33612345678")
        form.phone = "+32 470 12 34 56"
        #expect(form.isValid)
        #expect(form.e164Phone == "+32470123456")
    }

    @Test func invalidPhoneShowsExistingMessage() {
        var form = complete
        form.phone = "06 12 34"
        #expect(form.error(for: .phone) == "Saisissez un numéro valide, par exemple 06 12 34 56 78.")
    }
}

@Suite("Formulaire de connexion")
struct LoginFormTests {
    @Test func requiresValidEmailAndPassword() {
        #expect(!LoginForm().isValid)
        #expect(!LoginForm(email: "alex@example.com", password: "").isValid)
        #expect(!LoginForm(email: "alex", password: "secret").isValid)
        #expect(LoginForm(email: " Alex@Example.com ", password: "x").isValid)
    }
}

@Suite("Téléphone (PhoneNumberKit, région FR, E.164)")
struct PhoneInputTests {
    @Test(arguments: [
        ("06 12 34 56 78", "+33612345678"),
        ("0612345678", "+33612345678"),
        ("01 42 68 53 00", "+33142685300"),
        ("+33 6 12 34 56 78", "+33612345678"),
        ("0033 6 12 34 56 78", "+33612345678"),
        ("+32 470 12 34 56", "+32470123456"),
        ("+44 7400 123456", "+447400123456"),
    ])
    func validNumbersBecomeE164(_ raw: String, _ expected: String) {
        #expect(PhoneInput.e164(raw) == expected)
    }

    @Test(arguments: ["", "0612", "06 12 34 56 7", "+0612345678", "06 12 34 56 78 90", "abc", "+33 1"])
    func invalidNumbersAreRejected(_ raw: String) {
        #expect(PhoneInput.e164(raw) == nil)
    }

    @Test func formatsWhileTyping() {
        #expect(PhoneInput.formatWhileTyping("06") == "06")
        #expect(PhoneInput.formatWhileTyping("0612") == "06 12")
        #expect(PhoneInput.formatWhileTyping("0612345678") == "06 12 34 56 78")
        #expect(PhoneInput.formatWhileTyping("+32470123456") == "+32 470 12 34 56")
    }
}

@Suite("Chat IA : message d'accueil")
struct ChatWelcomeTests {
    @Test func greetsOwnerByFirstName() {
        #expect(
            ChatWelcome.message(ownerFirstName: "Alex", dogName: "Nala")
                == "Bonjour Alex 👋 Je suis Pawrise, l'assistant de Nala. Que souhaitez-vous consulter ?"
        )
    }

    @Test(arguments: [nil, "", "  "] as [String?])
    func keepsGenericGreetingWithoutFirstName(_ firstName: String?) {
        #expect(
            ChatWelcome.message(ownerFirstName: firstName, dogName: "Nala")
                == "Bonjour 👋 Je suis Pawrise, l'assistant de Nala. Que souhaitez-vous consulter ?"
        )
    }
}
