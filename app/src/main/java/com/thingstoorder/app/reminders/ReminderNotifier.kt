package com.thingstoorder.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.thingstoorder.app.MainActivity
import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.permissions.Permissions

private const val VOICE_ACTIVITY = "com.thingstoorder.app.widget.VoiceAddActivity"

object ReminderNotifier {
    const val ACTION_SNOOZE = "com.thingstoorder.app.action.SNOOZE_REMINDER"
    const val NOTIFICATION_ID = 2
    const val ADD_PROMPT_ID = 3
    private const val CHANNEL_ID = "reminders"
    private const val ADD_CHANNEL_ID = "add_prompt_v2"

    fun post(context: Context, remaining: List<Item>) {
        val allowed = Permissions.notificationsGranted(context)
        android.util.Log.i(ReminderScheduler.TAG, "post: remaining=${remaining.size} notificationsAllowed=$allowed")
        if (remaining.isEmpty() || !allowed) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Nudges when something is still on your list."
                },
            )
        }

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ReminderActionReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val count = remaining.size
        val names = remaining.take(3).joinToString(", ") { it.name } + if (count > 3) " +${count - 3}" else ""
        val title = if (count == 1) "1 thing still on your list" else "$count things still on your list"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_watch)
            .setColor(0xFFFF5E1A.toInt())
            .setContentTitle(title)
            .setContentText("Order them in one go?")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$names\nOrder them in one go?"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Open list", open)
            .addAction(0, "Snooze 1h", snooze)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        postAddPrompt(context, manager, snooze)
    }

    /** Quiet companion to the reminder: "Say it" opens the voice sheet, even over the lock screen. */
    private fun postAddPrompt(context: Context, manager: NotificationManager, snooze: PendingIntent) {
        if (manager.getNotificationChannel(ADD_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(ADD_CHANNEL_ID, "Add by voice", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                    description = "A quiet prompt to add anything you forgot, by voice."
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
            )
        }
        val sayIt = PendingIntent.getActivity(
            context, 2,
            Intent().setClassName(context.packageName, VOICE_ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ADD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_watch)
            .setColor(0xFFFF5E1A.toInt())
            .setContentTitle("Things to Order")
            .setContentText("Anything to add before you order?")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(sayIt)
            .addAction(NotificationCompat.Action.Builder(0, "Say it", sayIt).setAuthenticationRequired(false).build())
            .addAction(0, "Snooze 1h", snooze)
            .build()
        manager.notify(ADD_PROMPT_ID, notification)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(ADD_PROMPT_ID)
    }
}
