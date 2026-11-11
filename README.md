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
| API réelle | selon l'équipe backend, puis `adb reverse` sur son port | `-Ppawrise.apiBaseUrl=http://localhost:<port>/v1/` |

Le mock (Prism) sert `api/openapi.yaml`, copie du contrat qui fait foi : `pawrise-doc/contrat-interface/openapi.yaml` (`./scripts/sync-contract.sh` la met à jour). Il **valide les requêtes** : un champ mal nommé ou manquant est refusé. Pour tester un état d'erreur d'écran, ajoute l'en-tête `Prefer` :

```bash
curl -H 'Content-Type: application/json' -H 'Prefer: code=409' \
     -d '{"firstName":"C","lastName":"M","email":"c@example.com","password":"chien-oslo-2026","acceptedTermsVersion":"2026-10"}' \
     localhost:4010/auth/register    # EMAIL_ALREADY_USED
```

Les routes Bearer répondent 401 sans en-tête `Authorization`. Les chemins sont ceux du contrat (`/auth/login`) ; en production l'URL de base se termine par `/v1/` (convention 7).

### 3. Build

```bash
cd android
./gradlew assembleDebug          # build
./gradlew installDebug           # installe sur l'émulateur / le téléphone
./gradlew lint testDebugUnitTest # tests JVM
./gradlew testDebugUnitTest -Ppawrise.contractUrl=http://localhost:4010/  # + tests de contrat (mock lancé)
```

Sans JDK ni SDK sur la machine, le même build tourne dans Docker :

```bash
docker compose run --rm android assembleDebug
```

## Structure

```
android/
  app/                  Activity, navigation (Navigation 3), injection (Hilt)
  core/designsystem/    Thème Pawrise : jetons --pw-*, polices, composants partagés
  core/network/         Retrofit + OkHttp + kotlinx.serialization, erreurs Problem Details
  core/auth/            Session : AuthRepository, jetons chiffrés (Keystore), refresh unique sur 401
  feature/auth/         Bienvenue, connexion, inscription
  feature/onboarding/   Découverte en 5 étapes (LearnFlow)
  feature/dogprofile/   Profil du chien et association du collier (AddDogFlow)
api/openapi.yaml        Copie du contrat pawrise-doc, servie par le mock
docker/                 Image de build Android (JDK 21 + SDK 37)
```

## Contribuer

Conventions de l'organisation : [Pawrise/.github](https://github.com/Pawrise/.github/blob/main/CONTRIBUTING.md). Branches `feature/…`, commits `feat: …`, PR relue avant fusion. La CI (`.github/workflows/ci.yml`) lance lint, tests unitaires et build debug.
