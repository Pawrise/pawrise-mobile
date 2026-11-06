# pawrise-mobile

Application mobile propriétaire de Pawrise Care. Android natif (Kotlin, Jetpack Compose, Material 3) dans `android/`. L'app iOS (SwiftUI) viendra dans `ios/`.

Maquettes : [Figma Pawrise-App](https://www.figma.com/design/33w6Fklug8ywPYh6aDNGCj/Pawrise-App). Le Figma fait foi en cas d'écart avec les stories.

## Mise en route

### 1. Poste de dev (une fois)

```bash
sudo ./scripts/setup-host.sh   # JDK 21, Android Studio (/opt/android-studio), groupe kvm, ANDROID_HOME
```

Puis lance Android Studio, laisse l'assistant installer le SDK, ouvre le dossier `android/` et crée un émulateur (ou branche un téléphone en débogage USB).

### 2. Backend local

| Mode | Commande | URL vue par l'app |
|---|---|---|
| Mock (défaut) | `docker compose up -d` puis `./scripts/dev-reverse.sh` | `http://localhost:4010/` |
| Vrai service d'auth | `docker compose up -d` dans `pawrise-auth`, puis `adb reverse tcp:3000 tcp:3000` | `-Ppawrise.apiBaseUrl=http://localhost:3000/` |

Le mock (Prism) sert les `example` de `api/openapi.yaml`, traduction OpenAPI de `pawrise-auth/docs/api-contract.md` (le `.md` fait foi). Pour tester un état d'erreur d'écran, ajoute l'en-tête `Prefer` à la requête (intercepteur OkHttp de debug, ou curl) :

```bash
curl -H 'Content-Type: application/json' -H 'Prefer: code=423' \
     -d '{"email":"alex@example.com","password":"x"}' localhost:4010/v1/auth/login   # account_locked
# Prefer: code=409                               → email_taken (register)
# Prefer: code=422, example=password_breached    → exemple nommé
```

Une requête hors schéma (mot de passe < 8 caractères, champ manquant…) reçoit le premier exemple 422 de la route. Les routes Bearer répondent 401 sans en-tête `Authorization`.
 `adb reverse` fait pointer `localhost` du téléphone ou de l'émulateur vers ta machine. Pour changer d'URL de façon durable, ajoute `pawrise.apiBaseUrl=…` dans `~/.gradle/gradle.properties`.

### 3. Build

```bash
cd android
./gradlew assembleDebug          # build
./gradlew installDebug           # installe sur l'émulateur / le téléphone
./gradlew lint testDebugUnitTest # ce que lance la CI
```

Sans JDK ni SDK sur la machine, le même build tourne dans Docker :

```bash
docker compose run --rm android ./gradlew assembleDebug
```

## Structure

```
android/
  app/                  Activity, navigation (Navigation 3), injection (Hilt)
  core/designsystem/    Thème Pawrise : jetons --pw-*, polices, composants partagés
  core/network/         Retrofit + OkHttp + kotlinx.serialization
  feature/auth/         Bienvenue, connexion, inscription
  feature/onboarding/   Découverte en 5 étapes (LearnFlow)
  feature/dogprofile/   Profil du chien et association du collier (AddDogFlow)
api/openapi.yaml        Contrat servi par le mock
docker/                 Image de build Android (JDK 21 + SDK 37)
```

## Contribuer

Conventions de l'organisation : [Pawrise/.github](https://github.com/Pawrise/.github/blob/main/CONTRIBUTING.md). Branches `feature/…`, commits `feat: …`, PR relue avant fusion. La CI (`.github/workflows/ci.yml`) lance lint, tests unitaires et build debug.
