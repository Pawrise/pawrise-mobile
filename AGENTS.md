# pawrise-mobile

App Android propriétaire de Pawrise (Kotlin, Compose, Material 3). Le projet Gradle est dans `android/`.

## Stack

- AGP 9 avec Kotlin intégré : pas de plugin `org.jetbrains.kotlin.android`, ne pas l'ajouter.
- Versions dans `android/gradle/libs.versions.toml` uniquement, jamais en dur dans un `build.gradle.kts`.
- Navigation 3 (`androidx.navigation3`) : destinations `NavKey` sérialisables dans `app/.../navigation/Routes.kt`, branchées dans `PawriseNavDisplay.kt`.
- Hilt (KSP) pour l'injection, ViewModel par écran via `hiltViewModel()`.
- Réseau : Retrofit + kotlinx.serialization (`snake_case` côté API via `@SerialName`). Erreurs au format RFC 9457 : se baser sur le champ `code`, jamais sur `title`/`detail`.

## Règles

- **Le Figma fait foi.** Fichier `33w6Fklug8ywPYh6aDNGCj` : US-J connexion = nœud `2025:5772`, US-A inscription + onboarding = nœud `2025:1819`. Les noms de calques (`AuthScreen`, `LearnFlow`, `AddDogFlow`) donnent le découpage.
- **Pas de nouveau hex** dans les écrans : passer par `MaterialTheme.colorScheme` ou `PwColors`, `PwRadius`, `PwSpacing` (`core/designsystem`).
- Un module `feature:*` ne dépend jamais d'un autre `feature:*`. Il expose une `XxxRoute(...)` avec des callbacks de navigation ; c'est `app` qui relie les écrans.
- Chaque écran : une `Route` (branche le ViewModel) et un `Screen` sans état, avec une `@Preview` 390×844.
- Contrat de l'API d'auth : `pawrise-auth/docs/api-contract.md` (fait foi), traduit dans `api/openapi.yaml` pour le mock. Mot de passe : 8 caractères minimum (le Figma dit 6, le texte d'aide suit le contrat).

## Commandes

```bash
cd android && ./gradlew assembleDebug lint testDebugUnitTest
docker compose run --rm android ./gradlew assembleDebug   # sans SDK local
docker compose up -d && ./scripts/dev-reverse.sh           # mock d'API sur localhost:4010
```
