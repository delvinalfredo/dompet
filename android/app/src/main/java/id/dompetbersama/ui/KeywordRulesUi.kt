package id.dompetbersama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.dompetbersama.data.KeywordRule
import id.dompetbersama.data.KnownSources
import id.dompetbersama.data.TxType
import id.dompetbersama.ui.theme.Dompet

fun appLabel(packageName: String?, sources: List<SourceApp>?): String = when (packageName) {
    null -> "semua aplikasi"
    else -> sources?.firstOrNull { it.packageName == packageName }?.label
        ?: KnownSources.labels[packageName]
        ?: packageName
}

@Composable
fun KeywordRuleRow(rule: KeywordRule, appName: String, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("“${rule.word}”", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (rule.type == TxType.INCOME) "Uang masuk, dari " else "Uang keluar, dari ") + appName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rule.type == TxType.INCOME) Dompet.Green else Dompet.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRemove) { Text("Hapus", color = Dompet.Danger) }
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}

/** Teach the app a word from a bank notification. Sources offered are the ones switched on. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeywordRuleSheet(sources: List<SourceApp>?, onDismiss: () -> Unit, onSave: (KeywordRule) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var wordText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TxType.INCOME) }
    var packageName by rememberSaveable { mutableStateOf<String?>(null) }
    val word = KeywordRule.normalize(wordText)
    val enabledApps = sources.orEmpty().filter { it.enabled }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Dompet.Paper2) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text("Tambah kata kunci", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Salin kata dari notifikasi bank yang belum tercatat atau arahnya salah.",
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = wordText,
                onValueChange = { wordText = it.take(KeywordRule.MAX_WORD) },
                label = { Text("Kata atau frasa") },
                singleLine = true,
                isError = wordText.isNotBlank() && word == null,
                supportingText = {
                    Text(if (wordText.isNotBlank() && word == null) "Minimal ${KeywordRule.MIN_WORD} huruf" else "Contoh: dana diterima")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Text("Artinya", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(TxType.INCOME to "Uang masuk", TxType.EXPENSE to "Uang keluar").forEachIndexed { index, (option, label) ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = { type = option },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = Dompet.Accent, activeContentColor = Dompet.Ink),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Berlaku untuk", style = MaterialTheme.typography.labelLarge)
            Text(
                "Hanya aplikasi yang dinyalakan di Aplikasi sumber yang muncul di sini.",
                style = MaterialTheme.typography.bodySmall,
                color = Dompet.Muted,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf<String?>(null) + enabledApps.map { it.packageName }
                options.forEach { option ->
                    FilterChip(
                        selected = packageName == option,
                        onClick = { packageName = option },
                        label = { Text(if (option == null) "Semua aplikasi" else appLabel(option, sources)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Dompet.Accent, selectedLabelColor = Dompet.Ink),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "Simpan kata kunci",
                enabled = word != null,
                onClick = { word?.let { onSave(KeywordRule(packageName, it, type)) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
