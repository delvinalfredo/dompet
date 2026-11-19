package id.dompetbersama.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import id.dompetbersama.notification.TransactionNotificationListener

fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Re-checked on every resume, so returning from system Settings updates the screen. */
@Composable
fun rememberNotificationAccess(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasNotificationAccess(context)) }
    LifecycleResumeEffect(Unit) {
        granted = hasNotificationAccess(context)
        // Some phones (vivo/iQOO, Xiaomi, Oppo) unbind the listener in the background; opening the app reconnects it.
        if (granted) TransactionNotificationListener.reconnect(context)
        onPauseOrDispose { }
    }
    return granted
}

/** False when the device has no screen for this setting. */
fun openSystemSetting(context: Context, action: String): Boolean =
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

const val NOTIFICATION_ACCESS = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
const val BATTERY_OPTIMIZATION = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
