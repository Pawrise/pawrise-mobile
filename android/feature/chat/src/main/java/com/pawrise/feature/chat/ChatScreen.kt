package com.pawrise.feature.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.pets.Pet
import com.pawrise.feature.chat.assistant.TurnRole
import com.pawrise.feature.home.ui.DogAvatar
import com.pawrise.feature.home.ui.MaquetteFont
import com.pawrise.feature.home.ui.PawriseGlyph
import com.pawrise.feature.home.ui.PawriseGlyphs
import com.pawrise.feature.home.ui.collar
import com.pawrise.feature.home.ui.maquetteStyle

/** Mesures de la maquette (US-H). */
private object ChatLayout {
    val avatarSize = 28.dp
    val avatarSpacing = 8.dp
    /** Largeur des bulles (279 dp sur un écran de 390 dp). */
    val bubbleWidth = 279.dp
    /** Suggestions : ajustées au texte, au plus 219 dp. */
    val quickReplyMaxWidth = 219.dp
    val assistantBubble = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    val ownerBubble = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
}

/** Texte des bulles : Inter 400 14,5 · 162 %. */
private val bubbleStyle = maquetteStyle(MaquetteFont.Inter, 14.5f, lineHeight = 1.62f)

/**
 * Onglet Chat IA (Figma US-H 02 à 06) pour le chien affiché. [pendingQuestion] (« Demander à Pawrise » depuis
 * un panneau d'indicateur) est envoyée dès l'ouverture. « Contacter un vétérinaire » → [onContactVet],
 * « Voir les indicateurs » → [onShowIndicators].
 */
@Composable
fun ChatRoute(
    dog: Pet?,
    pendingQuestion: String? = null,
    onQuestionConsumed: () -> Unit = {},
    onContactVet: () -> Unit = {},
    onShowIndicators: () -> Unit = {},
) {
    val viewModel: ChatViewModel = hiltViewModel()
    if (dog == null) {
        Box(Modifier.fillMaxSize().background(PwColors.SurfacePage))
        return
    }
    val conversation = remember(dog.id) { viewModel.conversation(dog.id) }
    val state by conversation.state.collectAsStateWithLifecycle()
    LaunchedEffect(pendingQuestion, dog.id) {
        if (pendingQuestion != null) {
            conversation.send(pendingQuestion)
            onQuestionConsumed()
        }
    }
    // Onglet quitté ou chien changé : le tour en cours est interrompu.
    DisposableEffect(dog.id) { onDispose { viewModel.cancelAll() } }
    ChatScreen(
        pet = dog,
        state = state,
        onSend = conversation::send,
        onRetry = conversation::retry,
        onContactVet = onContactVet,
        onShowIndicators = onShowIndicators,
    )
}

@Composable
fun ChatScreen(
    pet: Pet,
    state: ChatUiState,
    onSend: (String) -> Boolean,
    onRetry: () -> Unit,
    onContactVet: () -> Unit,
    onShowIndicators: () -> Unit,
) {
    var draft by rememberSaveable(pet.id) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scroll = rememberScrollState()
    // Le fil suit le dernier élément quand il déborde.
    LaunchedEffect(state.messages.size, state.isResponding, state.failure) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().background(PwColors.SurfacePage)) {
        ChatHeader(pet)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .verticalScroll(scroll)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AssistantRow {
                AssistantBubble("Bonjour 👋 Je suis Pawrise, l'assistant de ${pet.name}. Que souhaitez-vous consulter ?")
            }
            if (state.showsQuickReplies) {
                Column(Modifier.padding(start = ChatLayout.avatarSize + ChatLayout.avatarSpacing), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val howIs = "Comment va ${pet.name} aujourd'hui ?"
                    QuickReply(howIs) { onSend(howIs) }
                    QuickReply("Et son sommeil ?") { onSend("Et son sommeil ?") }
                    QuickReply("Voir les indicateurs", minWidth = 160.dp, onClick = onShowIndicators)
                }
            }
            state.messages.forEach { message ->
                when (message.role) {
                    TurnRole.Owner -> OwnerBubble(message.text)
                    TurnRole.Assistant -> AssistantRow { AssistantBubble(message.text, urgent = message.response?.isUrgent == true) }
                }
            }
            if (state.isResponding) TypingRow(state.statusText)
            state.failure?.let { FailureRow(it, onRetry) }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clickable(role = Role.Button, onClick = onContactVet),
            contentAlignment = Alignment.Center,
        ) {
            Text("Contacter un vétérinaire", style = maquetteStyle(MaquetteFont.Inter, 14f, lineHeight = 1.35f, color = PwColors.Action))
        }

        val canSubmit = state.canSend && draft.isNotBlank()
        val submit = {
            if (canSubmit && onSend(draft)) draft = ""
        }
        Composer(
            draft = draft,
            onDraftChange = { draft = it },
            canSubmit = canSubmit,
            onSubmit = submit,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        )
    }
}

// En-tête

@Composable
private fun ChatHeader(pet: Pet) {
    val connected = pet.collar?.connected == true
    Row(
        Modifier
            .fillMaxWidth()
            .background(PwColors.SurfacePage)
            // Maquette : cadre de 82 dp, contenu centré (20 dp au-dessus, 18 dp au-dessous).
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DogAvatar(pet, 44.dp, cornerRadius = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pawrise", style = maquetteStyle(MaquetteFont.Inter, 17f, FontWeight.SemiBold, 1.35f, PwColors.TextPrimary))
                Box(
                    Modifier
                        .size(16.dp)
                        .background(PwColors.Action, CircleShape)
                        .semantics { contentDescription = "Assistant officiel" },
                    contentAlignment = Alignment.Center,
                ) {
                    PawriseGlyph(PawriseGlyphs.Paw, 10.dp, PwColors.ActionText)
                }
            }
            Text(
                "● Assistant de ${pet.name} · ${if (connected) "collier connecté" else "collier non connecté"}",
                style = maquetteStyle(MaquetteFont.Inter, 12f, lineHeight = 1.35f, color = PwColors.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Bulles

/** Ligne de l'assistant : avatar rond de 28 dp aligné en bas, puis la bulle. */
@Composable
private fun AssistantRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ChatLayout.avatarSpacing), verticalAlignment = Alignment.Bottom) {
        Box(
            Modifier.size(ChatLayout.avatarSize).background(PwColors.Action, CircleShape).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            PawriseGlyph(PawriseGlyphs.Paw, 15.dp, PwColors.ActionText)
        }
        content()
    }
}

/** Bulle blanche de l'assistant (bandeau « Urgence » si besoin) ; pas de sources ni de bouton véto dans le fil. */
@Composable
private fun AssistantBubble(text: String, urgent: Boolean = false) {
    val shape = ChatLayout.assistantBubble
    Column(
        Modifier
            .widthIn(max = ChatLayout.bubbleWidth)
            .fillMaxWidth()
            .background(if (urgent) PwColors.StatusAlertSurface else PwColors.SurfaceCard, shape)
            .border(1.dp, if (urgent) PwColors.StatusAlert.copy(alpha = 0.4f) else PwColors.Border, shape)
            // Figma : texte de 247 dp à 17 dp du bord gauche (filet compris).
            .padding(start = 17.dp, end = 15.dp, top = 11.dp, bottom = 11.dp)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (urgent) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                PawriseGlyph(PawriseGlyphs.Alert, 14.dp, PwColors.StatusAlert)
                Text("Urgence", style = maquetteStyle(MaquetteFont.Inter, 13f, FontWeight.SemiBold, color = PwColors.StatusAlert))
            }
        }
        Text(text.breakAfterHyphens(), style = bubbleStyle.copy(color = PwColors.TextPrimary))
    }
}

/**
 * Coupure possible après un trait d'union (« souhaitez- / vous »), comme dans Figma et les navigateurs :
 * Android ne coupe pas aux traits d'union sans césure automatique, d'où une espace sans chasse après chacun.
 */
internal fun String.breakAfterHyphens(): String = replace(Regex("(?<=\\p{L})-(?=\\p{L})"), "-\u200B")

@Composable
private fun OwnerBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text.breakAfterHyphens(),
            style = bubbleStyle.copy(color = PwColors.ActionText),
            modifier = Modifier
                .widthIn(max = ChatLayout.bubbleWidth)
                .fillMaxWidth()
                .background(PwColors.Action, ChatLayout.ownerBubble)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { contentDescription = "Vous : $text" },
        )
    }
}

/** Réponse en cours : bulle de 66 × 32 avec trois points de 6 dp espacés de 4 dp (maquette « Trois points animés »). */
@Composable
private fun TypingRow(status: String?) {
    Box(Modifier.clearAndSetSemantics { contentDescription = status ?: "Pawrise prépare sa réponse" }) {
        AssistantRow {
            val shape = ChatLayout.assistantBubble
            Box(
                Modifier
                    .size(66.dp, 32.dp)
                    .background(PwColors.SurfaceCard, shape)
                    .border(1.dp, PwColors.Border, shape),
                contentAlignment = Alignment.CenterStart,
            ) {
                TypingDots(Modifier.padding(start = 17.dp))
            }
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val lift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = androidx.compose.animation.core.keyframes {
                        durationMillis = 1050
                        0f at (index * 140)
                        -2f at (index * 140 + 260)
                        0f at (index * 140 + 520)
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dot$index",
            )
            Box(Modifier.offset(y = lift.dp).size(6.dp).background(PwColors.TextMuted, CircleShape))
        }
    }
}

@Composable
private fun FailureRow(message: String, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PwColors.StatusAlertSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PawriseGlyph(PawriseGlyphs.Alert, 18.dp, PwColors.StatusAlert)
        Text(
            message,
            style = maquetteStyle(MaquetteFont.Inter, 13f, lineHeight = 1.35f, color = PwColors.StatusAlert),
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onRetry), contentAlignment = Alignment.Center) {
            Text("Réessayer", style = maquetteStyle(MaquetteFont.Inter, 13f, FontWeight.SemiBold, color = PwColors.Action))
        }
    }
}

/**
 * Suggestion en capsule (Inter 600 13, fond #E5F1F7, filet primaire, 36 dp pour une ligne) : ajustée au texte,
 * au plus 219 dp de large ; au-delà le texte passe à la ligne comme dans Figma (« Comment va Nala aujourd'hui » / « ? »).
 */
@Composable
private fun QuickReply(title: String, minWidth: androidx.compose.ui.unit.Dp = 0.dp, onClick: () -> Unit) {
    var wraps by remember(title) { mutableStateOf(false) }
    Box(
        Modifier
            .widthIn(min = minWidth, max = ChatLayout.quickReplyMaxWidth)
            .then(if (wraps) Modifier.fillMaxWidth() else Modifier)
            .background(PwColors.SurfaceElevated, CircleShape)
            .border(1.dp, PwColors.Action, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            // Sur deux lignes : texte de 193 dp (comme iOS) pour couper « Comment va Nala aujourd'hui » / « ? ».
            .padding(horizontal = if (wraps) 13.dp else 15.dp, vertical = 9.dp),
    ) {
        Text(
            title,
            style = maquetteStyle(MaquetteFont.Inter, 13f, FontWeight.SemiBold, 1.35f, PwColors.Action),
            onTextLayout = { if (it.lineCount > 1) wraps = true },
        )
    }
}

// Saisie

@Composable
private fun Composer(draft: String, onDraftChange: (String) -> Unit, canSubmit: Boolean, onSubmit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .background(PwColors.SurfaceCard, CircleShape)
            .border(1.dp, PwColors.Border, CircleShape)
            .padding(start = 17.dp, end = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textStyle = maquetteStyle(MaquetteFont.Inter, 15f, lineHeight = 1.35f, color = PwColors.TextPrimary)
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(PwColors.Action),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            maxLines = 4,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Écrire un message…" },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) Text("Écrire un message…", style = textStyle.copy(color = PwColors.TextMuted), maxLines = 1)
                    inner()
                }
            },
        )
        // Opacité 0,4 de la maquette quand l'envoi est impossible (pas d'atténuation supplémentaire).
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .requiredSize(44.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null, enabled = canSubmit, onClick = onSubmit)
                    .clearAndSetSemantics {
                        contentDescription = "Envoyer"
                        role = Role.Button
                        if (!canSubmit) disabled()
                        onClick { onSubmit(); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(36.dp).alpha(if (canSubmit) 1f else 0.4f).background(PwColors.Action, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PawriseGlyph(PawriseGlyphs.Send, 17.dp, PwColors.ActionText)
                }
            }
        }
    }
}
