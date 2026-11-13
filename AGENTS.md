# pawrise-mobile

App Android propriétaire de Pawrise (Kotlin, Compose, Material 3). Le projet Gradle est dans `android/`.

## Stack

- AGP 9 avec Kotlin intégré : pas de plugin `org.jetbrains.kotlin.android`, ne pas l'ajouter.
- Versions dans `android/gradle/libs.versions.toml` uniquement, jamais en dur dans un `build.gradle.kts`.
- Navigation 3 (`androidx.navigation3`) : destinations `NavKey` sérialisables dans `app/.../navigation/Routes.kt`, branchées dans `PawriseNavDisplay.kt`.
- Hilt (KSP) pour l'injection, ViewModel par écran via `hiltViewModel()`.
- Réseau : Retrofit + kotlinx.serialization, appels enveloppés par `apiCall {}` → `ApiResult`. Erreurs RFC 9457 : se baser sur `code`, jamais sur `title`/`detail`.

## Règles

- **Le Figma fait foi.** Fichier `33w6Fklug8ywPYh6aDNGCj` : US-J connexion = nœud `2025:5772`, US-A inscription + onboarding = nœud `2025:1819`. Les noms de calques (`AuthScreen`, `LearnFlow`, `AddDogFlow`) donnent le découpage.
- **Pas de nouveau hex** dans les écrans : passer par `MaterialTheme.colorScheme` ou `PwColors`, `PwRadius`, `PwSpacing` (`core/designsystem`).
- Un module `feature:*` ne dépend jamais d'un autre `feature:*`. Il expose une `XxxRoute(...)` avec des callbacks de navigation ; c'est `app` qui relie les écrans.
- Chaque écran : une `Route` (branche le ViewModel) et un `Screen` sans état, avec une `@Preview` 390×844.
- Écrans faits : US-J connexion ; US-A Bienvenue, inscription, découverte, profil du chien, collier. La homepage (US-B) est encore un `PlaceholderScreen`.
- Tests d'écran : Robolectric + `createComposeRule` v2, taille Figma (`robolectric.properties`), champs retrouvés par leur libellé (`contentDescription`). Faux dépôts dans `core:testing` ; tests de parcours de l'app avec Hilt (`@UninstallModules` + `@BindValue`).
- **Contrat d'API** : `pawrise-doc/contrat-interface/openapi.yaml` fait foi (copié dans `api/openapi.yaml` par `scripts/sync-contract.sh`, jamais modifié à la main). camelCase, codes d'erreur `MAJUSCULES_SNAKE` (`ErrorCodes`), erreurs de validation en liste `errors[{field, code}]`. Le `pawrise-auth/docs/api-contract.md` (snake_case) est obsolète.
- Inscription : modifs de maquette M1–M3 de pawrise-doc appliquées car l'API les exige (CGU obligatoires, mot de passe ≥ 8 caractères, prénom + nom séparés).
- Tests : comportement sur la JVM (ViewModel, Compose via Robolectric, MockWebServer) et `*ContractTest` contre le mock Prism. Pas d'émulateur nécessaire.

## Commandes

```bash
cd android && ./gradlew assembleDebug lint testDebugUnitTest
docker compose run --rm android assembleDebug   # sans SDK local
docker compose up -d && ./scripts/dev-reverse.sh           # mock d'API sur localhost:4010
```
