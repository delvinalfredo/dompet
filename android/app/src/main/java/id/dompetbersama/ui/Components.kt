package id.dompetbersama.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.dompetbersama.ui.theme.Dompet

private val KeyShape = RoundedCornerShape(6.dp)
val EntryShape = RoundedCornerShape(14.dp)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = KeyShape,
        border = BorderStroke(1.dp, if (enabled) Dompet.AccentEdge else Dompet.Rule),
        colors = ButtonDefaults.buttonColors(
            containerColor = Dompet.Accent,
            contentColor = Dompet.Ink,
            disabledContainerColor = Dompet.Paper3,
            disabledContentColor = Dompet.Muted,
        ),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = KeyShape,
        border = BorderStroke(1.dp, Dompet.Rule),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Dompet.Paper2, contentColor = Dompet.Ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Dompet.Muted)
        }
    }
}

/** A heading over a 1px ink rule, the Furrow way to open a list. */
@Composable
fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        HorizontalDivider(thickness = 1.dp, color = Dompet.Ink)
    }
}

@Composable
fun EmptyState(title: String, body: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Dompet.Muted)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun NoticeCard(title: String, body: String, actionText: String, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = EntryShape,
        color = Dompet.AccentSoft,
        border = BorderStroke(1.dp, Dompet.Ink),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Dompet.Ink2)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(actionText, onAction)
        }
    }
}
