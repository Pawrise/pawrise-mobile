package com.pawrise.feature.settings

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pawrise.core.auth.CURRENT_TERMS_VERSION
import com.pawrise.core.auth.User
import com.pawrise.core.designsystem.R
import com.pawrise.core.designsystem.theme.BricolageGrotesque
import com.pawrise.core.designsystem.theme.DmMono
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.pets.Pet
import com.pawrise.core.pets.SamplePhoto
import com.pawrise.core.pets.scenario.HealthScenario
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Onglet Paramètres (langage visuel de la maquette I02 « Paramètres · partage ») : carte profil, Confidentialité,
 * Légal, Mes chiens, Données simulées, Se déconnecter, Supprimer mon compte (écran de confirmation).
 */
@Composable
fun SettingsRoute(onSignedOut: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var deleting by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }

    if (deleting) {
        val close = {
            viewModel.clearDeleteError()
            deleting = false
        }
        BackHandler(enabled = !state.deleting, onBack = close)
        DeleteAccountScreen(
            email = state.user?.email,
            dogNames = state.pets.map { it.name },
            deleting = state.deleting,
            error = state.deleteError,
            onBack = close,
            onConfirm = viewModel::deleteAccount,
        )
    } else {
        SettingsScreen(
            state = state,
            onToggle = viewModel::setPrivacy,
            onRetryPrivacy = viewModel::loadPrivacy,
            onScenario = viewModel::selectScenario,
            onResendVerification = viewModel::resendVerification,
            onLogout = viewModel::logout,
            onDeleteAccount = { deleting = true },
        )
    }
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onToggle: (PrivacyToggle, Boolean) -> Unit,
    onRetryPrivacy: () -> Unit,
    onScenario: (HealthScenario) -> Unit,
    onResendVerification: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    termsUrl: String = BuildConfig.TERMS_URL,
    privacyUrl: String = BuildConfig.PRIVACY_URL,
) {
    val context = LocalContext.current
    val version = remember { versionLabel(context) }
    Column(
        Modifier
            .fillMaxSize()
            .background(PwColors.SurfacePage)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text(
            "Paramètres",
            style = pwText(26f, FontWeight.Bold, lineHeight = 1.2f, family = BricolageGrotesque),
            modifier = Modifier.padding(top = 16.dp, bottom = 21.5.dp).semantics { heading() },
        )

        state.user?.let { user ->
            // Carte de compte sans titre de section, comme la carte de profil de la maquette.
            AccountCard(user, state.sendingVerification, state.verificationMessage, onResendVerification)
            Spacer(Modifier.height(20.dp))
        }

        SettingsSectionTitle("Confidentialité")
        PrivacySection(state, onToggle, onRetryPrivacy)
        Spacer(Modifier.height(19.dp))

        SettingsSectionTitle("Légal")
        LegalSection(termsUrl, privacyUrl)
        Spacer(Modifier.height(19.dp))

        if (state.pets.isNotEmpty()) {
            SettingsSectionTitle("Mes chiens")
            SettingsCard {
                state.pets.forEachIndexed { index, pet ->
                    if (index > 0) SettingsDivider()
                    DogRow(pet)
                }
            }
            Spacer(Modifier.height(19.dp))
        }

        SettingsSectionTitle("Données simulées")
        SettingsCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedPicker(
                    options = HealthScenario.entries,
                    selected = state.scenario,
                    label = { it.label },
                    onSelect = onScenario,
                    modifier = Modifier.semantics { contentDescription = "Scénario" },
                )
                Text(
                    "Tant que le collier n'envoie pas encore de vraies mesures, l'onglet Pawrise affiche des données de " +
                        "démonstration. Choisissez « Anomalie » pour voir comment une mesure inhabituelle est signalée.",
                    style = pwText(12f, color = PwColors.TextMuted),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsActionButton(
                "Se déconnecter",
                tint = PwColors.TextPrimary,
                border = PwColors.Border,
                onClick = onLogout,
                enabled = !state.loggingOut,
                loading = state.loggingOut,
            )
            SettingsActionButton(
                "Supprimer mon compte",
                tint = PwColors.StatusAlert,
                border = PwColors.StatusAlert.copy(alpha = 0.4f),
                onClick = onDeleteAccount,
                enabled = !state.loggingOut,
            )
        }
        Spacer(Modifier.height(20.dp))

        Text(
            version,
            style = pwText(12f, color = PwColors.TextMuted, align = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().testTag("settings-version"),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AccountCard(user: User, sending: Boolean, message: String?, onResend: () -> Unit) {
    SettingsCard {
        Row(
            Modifier.padding(13.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).background(PwColors.Action, RoundedCornerShape(16.dp)).clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(initials(user.fullName), style = pwText(19f, FontWeight.Bold, PwColors.ActionText, lineHeight = 1.2f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(user.fullName, style = pwText(16f, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user.email, style = pwText(12.5f, color = PwColors.TextMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                user.phoneNumber?.let { Text(it, style = pwText(12.5f, color = PwColors.TextMuted), maxLines = 1) }
            }
        }
        if (!user.emailVerified) {
            SettingsDivider()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !sending, role = Role.Button, onClick = onResend),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(SettingsIcons.Send)
                    Text(
                        "Renvoyer le lien de confirmation",
                        style = pwText(14f, FontWeight.Medium, PwColors.Action),
                        modifier = Modifier.weight(1f),
                    )
                    if (sending) CircularProgressIndicator(Modifier.size(16.dp), color = PwColors.Action, strokeWidth = 2.dp)
                }
                Text(
                    message ?: "Adresse e-mail pas encore confirmée : ouvrez le lien reçu par e-mail.",
                    style = pwText(12f, color = PwColors.TextMuted),
                )
            }
        }
    }
}

/** S17 Confidentialité : trois interrupteurs (lignes de 48, icône, titre Inter 14, détail Inter 11,5). */
@Composable
private fun PrivacySection(state: SettingsUiState, onToggle: (PrivacyToggle, Boolean) -> Unit, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsCard {
            when (val privacy = state.privacy) {
                PrivacyState.Loading -> Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Chargement…", style = pwText(14f, color = PwColors.TextMuted), modifier = Modifier.weight(1f))
                    CircularProgressIndicator(Modifier.size(18.dp), color = PwColors.TextMuted, strokeWidth = 2.dp)
                }
                is PrivacyState.Failed -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(privacy.message, style = pwText(13f, color = PwColors.StatusAlert))
                    Text(
                        "Réessayer",
                        style = pwText(14f, FontWeight.SemiBold, PwColors.Action),
                        modifier = Modifier.clickable(role = Role.Button, onClick = onRetry),
                    )
                }
                is PrivacyState.Loaded -> {
                    Spacer(Modifier.height(8.dp))
                    PrivacyRow(
                        SettingsIcons.Map, "Partage de localisation", "Partager la position en temps réel",
                        PrivacyToggle.Location, privacy, state, onToggle,
                    )
                    PrivacyRow(
                        SettingsIcons.Heart, "Notifications santé", "Alertes en cas d'anomalie détectée",
                        PrivacyToggle.HealthNotifications, privacy, state, onToggle,
                    )
                    PrivacyRow(
                        SettingsIcons.Lock, "Données anonymisées", "Contribuer à la recherche vétérinaire",
                        PrivacyToggle.AnonymizedData, privacy, state, onToggle,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        state.privacyError?.let {
            Text(it, style = pwText(12f, color = PwColors.StatusAlert), modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun PrivacyRow(
    icon: ImageVector,
    title: String,
    detail: String,
    toggle: PrivacyToggle,
    privacy: PrivacyState.Loaded,
    state: SettingsUiState,
    onToggle: (PrivacyToggle, Boolean) -> Unit,
) {
    SettingsSwitchRow(
        checked = toggle.read(privacy.preferences),
        onCheckedChange = { onToggle(toggle, it) },
        enabled = toggle !in state.savingPrivacy,
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingsIcon(icon)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = pwText(14f))
                Text(detail, style = pwText(11.5f, color = PwColors.TextMuted))
            }
        }
    }
}

/**
 * Légal : CGU (version acceptée à l'inscription) et politique de confidentialité. Adresses configurables
 * (`pawrise.termsUrl`, `pawrise.privacyUrl`) ; tant qu'elles sont vides, la ligne l'indique.
 */
@Composable
private fun LegalSection(termsUrl: String, privacyUrl: String) {
    val uriHandler = LocalUriHandler.current
    var unpublished by remember { mutableStateOf<String?>(null) }
    SettingsCard {
        Column(Modifier.padding(vertical = 8.dp)) {
            listOf("Conditions d’utilisation" to termsUrl, "Politique de confidentialité" to privacyUrl).forEach { (title, url) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            if (url.isNotBlank()) runCatching { uriHandler.openUri(url) } else unpublished = title
                        }
                        .padding(horizontal = 13.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = pwText(14f))
                    Text("›", style = pwText(13f), modifier = Modifier.clearAndSetSemantics {})
                }
            }
        }
    }
    unpublished?.let { title ->
        AlertDialog(
            onDismissRequest = { unpublished = null },
            title = { Text(title) },
            text = {
                Text("Le document sera disponible ici dès sa publication. Version des CGU acceptée : $CURRENT_TERMS_VERSION.")
            },
            confirmButton = { TextButton(onClick = { unpublished = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun DogRow(pet: Pet) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DogAvatar(pet)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pet.name, style = pwText(15f, FontWeight.SemiBold))
            dogSubtitle(pet).takeIf { it.isNotEmpty() }?.let { Text(it, style = pwText(12f, color = PwColors.TextMuted)) }
        }
        pet.collarSerial?.let { Text(it, style = pwText(12f, color = PwColors.TextMuted, family = DmMono)) }
    }
}

@Composable
private fun DogAvatar(pet: Pet) {
    val modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(PwColors.AvatarBackdrop)
    // Sans photo choisie, l'app affiche `sample_1` (règle du contrat).
    val sample = painterResource((pet.photoSample ?: SamplePhoto.Sample1).drawable)
    if (pet.photoUrl != null) {
        AsyncImage(pet.photoUrl, null, modifier, placeholder = sample, error = sample, contentScale = ContentScale.Crop)
    } else {
        Image(sample, null, modifier, contentScale = ContentScale.Crop)
    }
}

private val SamplePhoto.drawable: Int
    get() = when (this) {
        SamplePhoto.Sample1 -> R.drawable.pet_sample_1
        SamplePhoto.Sample2 -> R.drawable.pet_sample_2
        SamplePhoto.Sample3 -> R.drawable.pet_sample_3
        SamplePhoto.Sample4 -> R.drawable.pet_sample_4
    }

/** « Golden Retriever · 3 ans » (race, âge). */
internal fun dogSubtitle(pet: Pet, today: LocalDate = LocalDate.now()): String =
    listOfNotNull(
        pet.breeds.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
        pet.birthDate?.let { ageLabel(it, today) },
    ).joinToString(" · ")

internal fun ageLabel(birthDate: LocalDate, today: LocalDate = LocalDate.now()): String {
    val months = ChronoUnit.MONTHS.between(birthDate, today).coerceAtLeast(0)
    if (months < 12) return "$months mois"
    val years = months / 12
    return if (years >= 2) "$years ans" else "1 an"
}

internal fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

private fun versionLabel(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val build = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    "Pawrise ${info.versionName ?: "—"} ($build)"
}.getOrDefault("Pawrise")
