package id.dompetbersama.notification

import id.dompetbersama.data.TxType
import java.security.MessageDigest

object DedupKey {
    /**
     * SHA-256 of source app + notification identity + amount + type.
     * [notificationKey] (StatusBarNotification.key) and [whenMs] (Notification.when) separate two real
     * payments of the same amount, while a repost of the same notification keeps both and is ignored.
     */
    fun of(pkg: String, notificationKey: String, whenMs: Long, type: TxType, amount: Long): String {
        val input = "$pkg|$notificationKey|$whenMs|$type|$amount"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
