package id.dompetbersama.data

import android.content.Context

/**
 * Which apps' notifications the listener reads.
 * Only the user's own changes are stored, so a bank added to [KnownSources] in an update
 * is switched on for existing installs too, unless the user switched it off.
 */
class SourcePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("sources", Context.MODE_PRIVATE)

    fun enabledPackages(): Set<String> = resolveEnabled(
        defaults = KnownSources.enabledByDefault,
        userOn = prefs.getStringSet(KEY_USER_ON, null).orEmpty(),
        userOff = prefs.getStringSet(KEY_USER_OFF, null).orEmpty(),
    )

    fun setEnabled(packageName: String, enabled: Boolean) {
        val on = prefs.getStringSet(KEY_USER_ON, null).orEmpty() - packageName
        val off = prefs.getStringSet(KEY_USER_OFF, null).orEmpty() - packageName
        val isDefault = packageName in KnownSources.enabledByDefault
        prefs.edit()
            .putStringSet(KEY_USER_ON, if (enabled && !isDefault) on + packageName else on)
            .putStringSet(KEY_USER_OFF, if (!enabled && isDefault) off + packageName else off)
            .remove(KEY_LEGACY_ENABLED)
            .apply()
    }

    /** The user's own changes only, as stored; defaults are not part of a backup. */
    fun userChanges(): Pair<Set<String>, Set<String>> =
        prefs.getStringSet(KEY_USER_ON, null).orEmpty() to prefs.getStringSet(KEY_USER_OFF, null).orEmpty()

    fun replaceUserChanges(on: Set<String>, off: Set<String>) {
        prefs.edit().putStringSet(KEY_USER_ON, on).putStringSet(KEY_USER_OFF, off).remove(KEY_LEGACY_ENABLED).apply()
    }

    private companion object {
        const val KEY_USER_ON = "user_enabled"
        const val KEY_USER_OFF = "user_disabled"
        const val KEY_LEGACY_ENABLED = "enabled_packages" // older builds stored a full snapshot; ignored, removed on next change
    }
}

internal fun resolveEnabled(defaults: Set<String>, userOn: Set<String>, userOff: Set<String>): Set<String> =
    defaults - userOff + userOn

/**
 * Banking and e-wallet apps listed first in Pengaturan. Package names checked against their
 * Google Play listings (Nov 2026), except BNI Mobile, which is unverified.
 */
object KnownSources {
    val labels: Map<String, String> = mapOf(
        "com.bca" to "BCA mobile",
        "com.bca.mybca.omni.android" to "myBCA",
        "id.bmri.livin" to "Livin' by Mandiri",
        "id.co.bri.brimo" to "BRImo",
        "src.com.bni" to "BNI Mobile",
        "com.bsm.activity2" to "BSI Mobile",
        "id.co.cimbniaga.mobile.android" to "OCTO by CIMB Niaga",
        "com.btpn.dc" to "Jenius",
        "com.jago.digitalBanking" to "Jago",
        "com.victoriamobilebvic" to "Victoria Mobile",
        "com.dwidasa.victoria.mb.android" to "Victoria Mobile Banking",
        "com.bcadigital.blu" to "blu",
        "id.co.bankbkemobile.digitalbank" to "SeaBank",
        "com.gojek.gopay" to "GoPay",
        "com.shopeepay.id" to "ShopeePay",
        "ovo.id" to "OVO",
        "id.dana" to "DANA",
        "com.telkom.mwallet" to "LinkAja",
        "com.gojek.app" to "Gojek",
        "com.shopee.id" to "Shopee",
    )
    val packages: Set<String> = labels.keys

    // Gojek and Shopee also carry chats, promos and orders written by third parties: opt-in only.
    private val optIn = setOf("com.gojek.app", "com.shopee.id")
    val enabledByDefault: Set<String> = packages - optIn
}
