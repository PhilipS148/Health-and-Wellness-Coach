package io.github.philips148.wellnesscoach.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import io.github.philips148.wellnesscoach.MainActivity
import io.github.philips148.wellnesscoach.R
import java.time.ZonedDateTime

/** Local delivery cache only. R1 remains the source of saved reminder preferences. */
class ReminderDelivery(context: Context) {
    private val context = context.applicationContext
    private val cache = this.context.getSharedPreferences("reminder_delivery", Context.MODE_PRIVATE)
    private val alarms = this.context.getSystemService(AlarmManager::class.java)

    enum class Status { READY, NOTIFICATIONS_OFF, EXACT_TIME_ACCESS_NEEDED }

    fun syncSavedPreferences(userId: String, preferences: io.github.philips148.wellnesscoach.data.reminders.ReminderPreferences) {
        // Saving preferences and installing the phone's schedule are separate outcomes.
        try {
            replaceSchedules(userId, preferences.deliverySchedules())
            syncError.value = null
        } catch (_: Exception) {
            syncError.value = "Your preferences are saved, but this phone couldn't update reminders. Reopen Reminders to retry."
        }
    }

    fun status(): Status = when {
        !notificationsAllowed() -> Status.NOTIFICATIONS_OFF
        Build.VERSION.SDK_INT >= 31 && !alarms.canScheduleExactAlarms() -> Status.EXACT_TIME_ACCESS_NEEDED
        else -> Status.READY
    }

    fun notificationsAllowed(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    /** Call only after R1 successfully loads/saves the current account's full settings. */
    @Synchronized
    fun replaceSchedules(userId: String, schedules: List<ReminderSchedule>): Status {
        require(userId.isNotBlank() && userId == FirebaseAuth.getInstance().currentUser?.uid) {
            "Reminder settings must belong to the signed-in account."
        }
        require(schedules.map { it.kind }.distinct().size == schedules.size)
        cancelAlarms()
        val editor = cache.edit().clear().putString("owner", userId)
        schedules.forEach {
            editor.putBoolean("${it.kind.name}_enabled", it.enabled)
                .putInt("${it.kind.name}_hour", it.hour).putInt("${it.kind.name}_minute", it.minute)
        }
        check(editor.commit()) { "Couldn't save the local delivery schedule. Retry saving reminders." }
        restore()
        return status()
    }

    /** Called on auth changes. A new account never inherits the previous account's cache. */
    @Synchronized
    fun onAccountChanged(userId: String?) {
        if (cache.getString("owner", null) != userId || userId == null) clear()
        else restore()
    }

    @Synchronized
    fun clear() {
        syncError.value = null
        cancelAlarms()
        cache.edit().clear().commit()
        ReminderKind.entries.forEach { NotificationManagerCompat.from(context).cancel(it.ordinal + 100) }
    }

    @Synchronized
    fun restore() {
        createChannel()
        val owner = cache.getString("owner", null)
        if (owner == null || owner != FirebaseAuth.getInstance().currentUser?.uid) {
            clear()
            return
        }
        cancelAlarms()
        if (status() != Status.READY) return
        ReminderKind.entries.forEach { kind ->
            if (cache.getBoolean("${kind.name}_enabled", false)) schedule(kind, owner)
        }
    }

    @Synchronized
    fun deliver(intent: Intent) {
        val kind = ReminderKind.entries.find { it.name == intent.getStringExtra("kind") } ?: return
        val owner = intent.getStringExtra("owner") ?: return
        val current = FirebaseAuth.getInstance().currentUser?.uid
        if (owner != current || owner != cache.getString("owner", null)) return
        if (!cache.getBoolean("${kind.name}_enabled", false)) return
        // Ignore broadcasts from a replaced schedule that were already queued.
        if (intent.getIntExtra("hour", -1) != cache.getInt("${kind.name}_hour", -2) ||
            intent.getIntExtra("minute", -1) != cache.getInt("${kind.name}_minute", -2)) return
        if (status() != Status.READY) return
        schedule(kind, owner)
        val open = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("wellness-reminder://open/${kind.name}")
            putExtra("reminder_owner", owner)
            putExtra("reminder_route", kind.route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val click = PendingIntent.getActivity(context, kind.ordinal + 100, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder).setContentTitle(kind.label)
            .setContentText(kind.message).setContentIntent(click).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        try { NotificationManagerCompat.from(context).notify(kind.ordinal + 100, notification) }
        catch (_: SecurityException) { /* Permission may change between check and delivery. */ }
    }

    private fun createChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Wellness reminders", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun alarmIntent(kind: ReminderKind, owner: String = ""): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.parse("wellness-reminder://alarm/${kind.name}")
            putExtra("kind", kind.name)
            putExtra("owner", owner)
            putExtra("hour", cache.getInt("${kind.name}_hour", 0))
            putExtra("minute", cache.getInt("${kind.name}_minute", 0))
        }
        return PendingIntent.getBroadcast(context, kind.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancelAlarms() {
        ReminderKind.entries.forEach { kind ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                data = Uri.parse("wellness-reminder://alarm/${kind.name}")
            }
            PendingIntent.getBroadcast(context, kind.ordinal, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                alarms.cancel(it)
                it.cancel()
            }
        }
    }

    private fun schedule(kind: ReminderKind, owner: String) {
        val next = nextReminderTime(ZonedDateTime.now(), cache.getInt("${kind.name}_hour", 0),
            cache.getInt("${kind.name}_minute", 0))
        try {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), alarmIntent(kind, owner))
        } catch (_: SecurityException) { /* The settings UI will report missing exact-time access. */ }
    }

    companion object {
        const val CHANNEL = "wellness_reminders"
        val syncError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    }
}
