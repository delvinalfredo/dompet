package id.dompetbersama.data

import android.content.Context

/**
 * A word the user taught the app: when it appears in a notification from [packageName]
 * (or from any source when null), the transaction is [type]. Checked before the built-in words.
 */
data class KeywordRule(val packageName: String?, val word: String, val type: TxType) {
    fun appliesTo(pkg: String): Boolean = packageName == null || packageName == pkg

    companion object {
        const val MIN_WORD = 3
        const val MAX_WORD = 40

        /** Lowercased and trimmed, or null when too short or too long to be a useful keyword. */
        fun normalize(word: String): String? =
            word.replace(Regex("""\p{Cntrl}"""), " ") // also drops a pasted \u001F, the storage separator
                .trim().replace(Regex("""\s+"""), " ").lowercase().takeIf { it.length in MIN_WORD..MAX_WORD }
    }
}

/** Stored as a string set in SharedPreferences; the list is small and edited by hand. */
class KeywordPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("keywords", Context.MODE_PRIVATE)

    fun all(): List<KeywordRule> =
        prefs.getStringSet(KEY_RULES, null).orEmpty().mapNotNull(KeywordCodec::decode).sortedBy { it.word }

    /** Replaces an existing rule with the same app and word. */
    fun add(rule: KeywordRule) {
        val kept = all().filterNot { it.packageName == rule.packageName && it.word == rule.word }
        save(kept + rule)
    }

    fun remove(rule: KeywordRule) = save(all() - rule)

    fun replaceAll(rules: List<KeywordRule>) = save(rules)

    private fun save(rules: List<KeywordRule>) {
        prefs.edit().putStringSet(KEY_RULES, rules.map(KeywordCodec::encode).toSet()).apply()
    }

    private companion object {
        const val KEY_RULES = "rules"
    }
}

internal object KeywordCodec {
    private const val SEP = '\u001F' // unit separator: cannot be typed into the word field
    private const val ANY_APP = "*"

    fun encode(rule: KeywordRule): String = listOf(rule.type.name, rule.packageName ?: ANY_APP, rule.word).joinToString(SEP.toString())

    fun decode(raw: String): KeywordRule? {
        val parts = raw.split(SEP)
        if (parts.size != 3) return null
        val type = TxType.entries.firstOrNull { it.name == parts[0] } ?: return null
        val word = KeywordRule.normalize(parts[2]) ?: return null
        return KeywordRule(packageName = parts[1].takeUnless { it == ANY_APP }, word = word, type = type)
    }
}
