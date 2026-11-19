package id.dompetbersama.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Everything the app keeps on the phone. Row ids are not carried; the database gives new ones. */
data class BackupData(
    val createdAt: Long,
    val transactions: List<MoneyTransaction>,
    val keywords: List<KeywordRule>,
    val openings: Map<String, Opening>,
    val mainWallet: String?,
    val monthlyLimit: Long?,
    val sourcesOn: Set<String>,
    val sourcesOff: Set<String>,
)

/** Message is shown to the user as is. */
class BackupException(message: String) : Exception(message)

/**
 * JSON so the file stays readable and survives app updates. A restore replaces all data, so [decode] rejects
 * the whole file on the first bad value instead of restoring half of it.
 */
object BackupCodec {
    const val FORMAT = "dompet-bersama-backup"
    const val VERSION = 1

    /** Larger than years of daily use; a bigger file is not ours. */
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val MAX_ROWS = 200_000

    // Small lists edited by hand. A file stuffed with rules would slow every notification check.
    private const val MAX_RULES = 500
    private val packageName = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private const val MAX_AMOUNT = 10_000_000_000_000L
    private const val MAX_TEXT = 300
    private const val MAX_NAME = 100

    fun encode(data: BackupData): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("createdAt", data.createdAt)
        .put("transactions", JSONArray(data.transactions.map(::txJson)))
        .put(
            "keywords",
            JSONArray(
                data.keywords.map {
                    JSONObject().put("type", it.type.name).put("app", it.packageName ?: JSONObject.NULL).put("word", it.word)
                },
            ),
        )
        .put(
            "wallets",
            JSONObject()
                .put(
                    "openings",
                    JSONArray(
                        data.openings.map { (name, o) -> JSONObject().put("name", name).put("amount", o.amount).put("since", o.since) },
                    ),
                )
                .put("main", data.mainWallet ?: JSONObject.NULL)
                .put("monthlyLimit", data.monthlyLimit ?: JSONObject.NULL),
        )
        .put("sources", JSONObject().put("on", JSONArray(data.sourcesOn.sorted())).put("off", JSONArray(data.sourcesOff.sorted())))
        .toString()

    private fun txJson(tx: MoneyTransaction) = JSONObject()
        .put("type", tx.type.name)
        .put("amount", tx.amount)
        .put("category", tx.category)
        .put("note", tx.note)
        .put("source", tx.source)
        .put("occurredAt", tx.occurredAt)
        .put("status", tx.status.name)
        .put("dedupKey", tx.dedupKey ?: JSONObject.NULL)
        .put("transferGroup", tx.transferGroup ?: JSONObject.NULL)

    fun decode(text: String): BackupData = try {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) fail("Ini bukan file backup Dompet Bersama.")
        val version = root.optInt("version", -1)
        if (version > VERSION) fail("Backup ini dibuat versi app yang lebih baru. Perbarui app dulu.")
        if (version < 1) fail("Versi file backup tidak dikenali.")

        val txs = root.getJSONArray("transactions")
        if (txs.length() > MAX_ROWS) fail("File backup terlalu besar.")
        val wallets = root.getJSONObject("wallets")
        val sources = root.getJSONObject("sources")
        val openingRows = wallets.getJSONArray("openings")
        val keywordRows = root.getJSONArray("keywords")
        val onRows = sources.getJSONArray("on")
        val offRows = sources.getJSONArray("off")
        if (listOf(openingRows, keywordRows, onRows, offRows).any { it.length() > MAX_RULES }) fail("File backup terlalu besar.")
        val openings = objects(openingRows).associate { o ->
            name(o.getString("name")) to Opening(amount = o.getLong("amount").also { check(it in 0..MAX_AMOUNT) }, since = o.getLong("since"))
        }
        BackupData(
            createdAt = root.getLong("createdAt"),
            transactions = objects(txs).map(::decodeTx).also { rows ->
                // The database index is unique; catch it here instead of after the user confirmed.
                val keys = rows.mapNotNull { it.dedupKey }
                check(keys.size == keys.toSet().size)
            },
            keywords = objects(keywordRows).map { k ->
                KeywordRule(
                    packageName = nullableString(k, "app")?.let(::pkg),
                    word = KeywordRule.normalize(k.getString("word")) ?: fail("Kata kunci di backup tidak valid."),
                    type = TxType.valueOf(k.getString("type")),
                )
            },
            openings = openings,
            mainWallet = nullableString(wallets, "main")?.takeIf { it in openings },
            monthlyLimit = if (wallets.isNull("monthlyLimit")) null else wallets.getLong("monthlyLimit").also { check(it in 1..MAX_AMOUNT) },
            sourcesOn = strings(onRows).map(::pkg).toSet(),
            sourcesOff = strings(offRows).map(::pkg).toSet(),
        )
    } catch (e: BackupException) {
        throw e
    } catch (e: JSONException) {
        throw BackupException("File backup rusak atau tidak lengkap.")
    } catch (e: IllegalArgumentException) { // bad enum name or a failed check()
        throw BackupException("File backup berisi data yang tidak valid.")
    } catch (e: IllegalStateException) {
        throw BackupException("File backup berisi data yang tidak valid.")
    }

    private fun decodeTx(o: JSONObject) = MoneyTransaction(
        type = TxType.valueOf(o.getString("type")),
        amount = o.getLong("amount").also { check(it in 1..MAX_AMOUNT) },
        category = text(o.getString("category"), MAX_NAME),
        note = text(o.getString("note"), MAX_TEXT),
        source = name(o.getString("source")),
        occurredAt = o.getLong("occurredAt"),
        status = TxStatus.valueOf(o.getString("status")),
        dedupKey = nullableString(o, "dedupKey")?.let { text(it, MAX_NAME) },
        transferGroup = nullableString(o, "transferGroup")?.let { text(it, MAX_NAME) },
    )

    private fun text(value: String, max: Int): String = value.also { check(it.length <= max) }

    // Wallet and app names: also stored with a \u001F separator in preferences, so it may not appear here.
    private fun name(value: String): String = text(value, MAX_NAME).also { check(it.isNotBlank() && '\u001F' !in it) }

    private fun pkg(value: String): String = text(value, MAX_NAME).also { check(packageName.matches(it)) }

    private fun nullableString(o: JSONObject, key: String): String? = if (o.isNull(key)) null else o.getString(key)

    private fun objects(array: JSONArray): List<JSONObject> = List(array.length()) { array.getJSONObject(it) }

    private fun strings(array: JSONArray): List<String> = List(array.length()) { array.getString(it) }

    private fun fail(message: String): Nothing = throw BackupException(message)
}

class BackupPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)

    fun lastBackupAt(): Long? = prefs.getLong(KEY_LAST, 0L).takeIf { it > 0 }

    fun setLastBackupAt(millis: Long) = prefs.edit().putLong(KEY_LAST, millis).apply()

    private companion object {
        const val KEY_LAST = "last_backup_at"
    }
}
