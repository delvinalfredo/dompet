package id.dompetbersama.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.dompetbersama.data.KeywordRule
import id.dompetbersama.notification.NotificationParser
import id.dompetbersama.ui.theme.Dompet

/** The user's own words plus the built-in ones, so they can see what is already recognised before adding. */
@Composable
fun KeywordScreen(
    rules: List<KeywordRule>,
    sources: List<SourceApp>?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onAddRule: (KeywordRule) -> Unit,
    onRemoveRule: (KeywordRule) -> Unit,
) {
    var addingRule by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Dompet.Ink)
                Text("Pengaturan", color = Dompet.Ink, modifier = Modifier.padding(start = 8.dp))
            }
            ScreenTitle(
                "Kata kunci",
                "Dicari di judul dan isi notifikasi, di posisi mana pun. Kata yang muncul paling awal menentukan masuk atau keluar. Notifikasi tetap harus memuat nominal dengan Rp atau IDR.",
            )
        }

        item {
            SectionHeader("Kata kamu")
            Text(
                "Dicek lebih dulu dari kata bawaan, dan harus cocok satu kata utuh: “dana” tidak kena “Danamon”.",
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        if (rules.isEmpty()) {
            item {
                Text(
                    "Belum ada. Contoh: “dana diterima” dari Victoria Mobile berarti uang masuk.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(rules, key = { "${it.packageName}|${it.word}" }) { rule ->
                KeywordRuleRow(rule, appLabel(rule.packageName, sources), onRemove = { onRemoveRule(rule) })
            }
        }
        item {
            PrimaryButton(
                "Tambah kata kunci",
                onClick = { addingRule = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        item {
            BuiltInWords(
                "Bawaan: uang masuk",
                "Cocok walau menempel di kata lain. “Kredit” di dalam “kartu kredit” tidak dihitung.",
                NotificationParser.incomeWords,
            )
        }
        item { BuiltInWords("Bawaan: uang keluar", null, NotificationParser.expenseWords) }
        item {
            BuiltInWords(
                "Dilewati sebagai promo",
                "Notifikasi dengan kata ini tidak dicatat, kecuali ada kata kamu di dalamnya.",
                NotificationParser.ignoredUnlessTaught,
            )
        }
        item {
            BuiltInWords(
                "Selalu dilewati",
                "Notifikasi kode OTP tidak pernah dicatat, kata kamu pun tidak bisa mengubahnya.",
                NotificationParser.alwaysIgnored.map { it.trim() },
            )
        }
    }

    if (addingRule) {
        KeywordRuleSheet(
            sources = sources,
            onDismiss = { addingRule = false },
            onSave = {
                onAddRule(it)
                addingRule = false
            },
        )
    }
}

private val WordShape = RoundedCornerShape(6.dp)

/** Read-only list; these words live in code, not in preferences. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuiltInWords(title: String, note: String?, words: List<String>) {
    SectionHeader(title)
    if (note != null) {
        Text(
            note,
            style = MaterialTheme.typography.bodyMedium,
            color = Dompet.Muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        )
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEach { word ->
            Surface(shape = WordShape, color = Dompet.Paper2, border = BorderStroke(1.dp, Dompet.Rule)) {
                Text(
                    word,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
