package com.pawrise.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pawrise.core.designsystem.theme.PwColors
import com.pawrise.core.designsystem.theme.PwSpacing

/** En-tête des parcours plein écran (ajout du chien, collier) : retour rond, centre, fermer rond. */
@Composable
fun PwFlowHeader(
    onBack: (() -> Unit)?,
    onClose: (() -> Unit)?,
    center: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PwSpacing.Page, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIconSlot(PwIcons.ChevronLeft, "Retour", onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { center() }
        RoundIconSlot(PwIcons.Close, "Fermer", onClose)
    }
}

@Composable
private fun RoundIconSlot(icon: ImageVector, label: String, onClick: (() -> Unit)?) {
    if (onClick == null) return Spacer(Modifier.size(40.dp))
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = PwColors.SurfaceCard),
        modifier = Modifier.size(40.dp),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) }
}

/** Titre d'étape (26 sp) + aide facultative, comme `Question` dans la maquette. */
@Composable
fun PwQuestion(title: String, hint: String? = null) {
    Column(Modifier.padding(top = 16.dp, bottom = 24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
