package io.github.philips148.wellnesscoach.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** R1 can place this below its saved reminder switches. Request access on user action only. */
@Composable
fun ReminderPermissionControls() {
    val context = LocalContext.current
    val delivery = remember { ReminderDelivery(context) }
    var status by remember { mutableStateOf(delivery.status()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        delivery.restore()
        status = delivery.status()
    }
    LifecycleResumeEffect(Unit) {
        delivery.restore()
        status = delivery.status()
        onPauseOrDispose { }
    }
    val syncError by ReminderDelivery.syncError.collectAsStateWithLifecycle()
    syncError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    when (status) {
        ReminderDelivery.Status.NOTIFICATIONS_OFF -> {
            Text("Notifications are off. Allow notifications to receive reminders. Your saved choices are kept.")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Enable notifications") }
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Open Settings") }
        }
        ReminderDelivery.Status.EXACT_TIME_ACCESS_NEEDED -> {
            Text("Allow alarms and reminders so notifications can arrive at your chosen times.")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 31) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")))
            }) { Text("Allow scheduled reminders") }
        }
        ReminderDelivery.Status.READY -> Text("Notifications are on. Reminders use this phone's local time.")
    }
}
