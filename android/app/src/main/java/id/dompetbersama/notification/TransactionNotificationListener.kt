package id.dompetbersama.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import id.dompetbersama.DompetApp
import id.dompetbersama.data.KnownSources
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.TxStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Runs while the user has granted notification access. Never logs notification content. */
class TransactionNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = application as DompetApp
        val pkg = sbn.packageName
        if (pkg == packageName || pkg !in app.sourcePrefs.enabledPackages()) return

        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        // Chat text is written by other people (sellers, drivers) and could fake a "payment received".
        if (notification.category == Notification.CATEGORY_MESSAGE || extras.containsKey(Notification.EXTRA_MESSAGES)) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()

        val userRules = app.keywordPrefs.all().filter { it.appliesTo(pkg) }
        val parsed = NotificationParser.parse(title, text, userRules) ?: return
        val tx = MoneyTransaction(
            type = parsed.type,
            amount = parsed.amount,
            category = parsed.category,
            note = parsed.note,
            source = labelFor(pkg),
            occurredAt = sbn.postTime,
            status = TxStatus.PENDING,
            dedupKey = DedupKey.of(
                pkg = pkg,
                notificationKey = sbn.key,
                whenMs = notification.`when`.takeIf { it > 0 } ?: sbn.postTime,
                type = parsed.type,
                amount = parsed.amount,
            ),
        )
        scope.launch { app.repository.addFromNotification(tx) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        reconnect(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Asks the system to bind the listener again. No-op while it is connected; costs nothing in between. */
        fun reconnect(context: Context) {
            runCatching { requestRebind(ComponentName(context, TransactionNotificationListener::class.java)) }
        }
    }

    private fun labelFor(pkg: String): String =
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }
            .getOrNull()
            ?: KnownSources.labels[pkg]
            ?: pkg
}
