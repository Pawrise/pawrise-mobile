# Pawrise iOS

App propriétaire Pawrise en SwiftUI : connexion, découverte, profil du chien, association du collier, tableau de bord Santé et chat avec l'assistant Pawrise.

## Lancer l'app

1. Ouvrir `ios/Pawrise.xcodeproj` avec **Xcode 26** (iOS 17 minimum).
2. Choisir un simulateur iPhone, puis ⌘R.

Par défaut (Debug) l'app fonctionne **sans serveur** :

| Brique | Mode Debug | Remplacé plus tard par |
|---|---|---|
| Comptes | `MockAuthService`, mêmes règles et codes d'erreur que [pawrise-auth](https://github.com/Pawrise/pawrise-auth/blob/main/docs/api-contract.md) | `RemoteAuthService` (`PAWRISE_AUTH_MODE = remote`) |
| Chiens | Fichier JSON local par compte (`LocalDogRepository`) | Core API |
| Collier | `MockCollarService` (délai simulé) | Core API |
| Mesures | `SeedHealthRepository` : données du Figma, scénario Normal ou Anomalie | Core API (séries TimescaleDB) |
| Chat | **Vrai** `pawrise-assistant` sur `http://127.0.0.1:8100` | Même route derrière KrakenD |

**Compte de démo** : `demo@pawrise.fr` / `pawrise2026` (arrive directement sur le tableau de bord de Nala).
Créer un compte lance le parcours complet : découverte → profil du chien (9 étapes) → collier → Santé.

Codes collier de test : n'importe quel `PW-XXXXXX` valide fonctionne ; `PW-000000` simule « déjà associé », `PW-999999` simule « collier injoignable ».

Le scénario des mesures (Normal / Anomalie) se change dans **Paramètres → Données simulées**.

## Brancher le chat

```sh
cd ../pawrise-assistant   # git clone git@github.com:Pawrise/pawrise-assistant.git
uv sync && uv run dev     # http://127.0.0.1:8100, sans clé API
```

En Debug, l'app envoie `pet_ref = pet_demo_nala` (chien de démo de l'assistant) quel que soit le chien affiché, tant que la Core API ne fournit pas les profils.

Sur un **iPhone physique**, `uv run dev` n'écoute que sur 127.0.0.1. Lancer plutôt :

```sh
uv run uvicorn pawrise_assistant.api.app:create_app --factory --host 0.0.0.0 --port 8100
```

puis indiquer l'IP du Mac dans `Config/Local.xcconfig` :

```
PAWRISE_ASSISTANT_BASE_URL = http:/$()/192.168.1.20:8100
```

## Configuration

Les réglages vivent dans `Config/*.xcconfig` et sont lus via `Info.plist` (`AppConfig`).
Pour des valeurs personnelles (client Google, équipe de signature, IP), copier `Config/Local.xcconfig.example` en `Config/Local.xcconfig` (ignoré par git).

### Connexion Apple

Capability « Sign in with Apple » déjà déclarée (`Config/Pawrise.entitlements`). Il faut une équipe Apple Developer (`DEVELOPMENT_TEAM`) et l'identifiant `fr.pawrise.app` pour l'essayer sur appareil. Le nonce est généré dans l'app (`SocialSignIn`) et vérifié par pawrise-auth.

### Connexion Google

1. Créer un client OAuth **iOS** dans Google Cloud (bundle id `fr.pawrise.app`).
2. Renseigner `GOOGLE_CLIENT_ID` et `GOOGLE_REVERSED_CLIENT_ID` dans `Config/Local.xcconfig`.
3. Donner ce client ID à l'équipe back : pawrise-auth accepte la liste des clients iOS, Android et web (`GOOGLE_CLIENT_IDS`).

Sans client configuré, le bouton Google affiche un message explicite au lieu de planter.

## Architecture

```
Pawrise/
  App/            Point d'entrée, assemblage des services (AppEnvironment), aiguillage (RootView)
  Core/
    Auth/         Contrat pawrise-auth, TokenStore (Keychain + refresh unique), SessionStore, Apple/Google
    Network/      APIClient (JSON, RFC 9457, retry après refresh), AssistantClient (SSE)
    Storage/      Keychain
  Data/           Dépôts : chiens, mesures simulées, collier
  DesignSystem/   Jetons du Figma (couleurs, typo, rayons) et composants
  Features/       Auth, Learn (découverte), AddDog + Collar, Home (Santé), Chat, Shell (onglets)
PawriseTests/     Swift Testing
```

- SwiftUI + Observation, MVVM léger (`@Observable` pour l'état d'écran).
- Chaque dépendance externe est derrière un protocole (`AuthService`, `DogRepository`, `HealthRepository`, `CollarService`, `AssistantService`) : passer du simulé au réel ne touche pas les écrans.
- Le projet utilise des dossiers synchronisés : un fichier ajouté dans `Pawrise/` est compilé sans modifier le `.pbxproj`.
- Dépendance externe unique : GoogleSignIn (SPM).

### Polices

Le Figma utilise Bricolage Grotesque, Inter et DM Mono. Tant que les `.ttf` ne sont pas ajoutés (`Resources/Fonts` + clé `UIAppFonts`), iOS affiche la police système à la même taille.

## Tests

```sh
xcodebuild test -project Pawrise.xcodeproj -scheme Pawrise -destination 'platform=iOS Simulator,name=iPhone 17'
```

- `PawriseTests` (Swift Testing) : règles de saisie, analyseur SSE et décodage des réponses de l'assistant, auth simulée (codes d'erreur, verrouillage, rotation du refresh token), refresh unique en concurrence, données simulées, conversation du chat.
- `PawriseUITests` (XCUITest) : deux parcours complets dans le simulateur, avec captures d'écran jointes au rapport :
  1. inscription → découverte → profil du chien → collier → Santé → détail d'un indicateur ;
  2. compte de démo → Chat IA → question au vrai `pawrise-assistant`. Ce test est ignoré si l'assistant ne tourne pas sur `127.0.0.1:8100`.

L'argument de lancement `-PawriseUITestReset` (Debug uniquement) remet l'app à zéro avant chaque parcours.

La CI (`.github/workflows/ios-ci.yml`) lance la même commande sur chaque PR qui touche `ios/`.
