package com.aventaro.nandan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Nandan reminder"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val id = intent.getIntExtra(EXTRA_ID, 1)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Nandan reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(note.ifBlank { "Your reminder is due." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.ifBlank { "Your reminder is due." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(id, notification) } catch (_: SecurityException) { }
    }
    companion object {
        const val CHANNEL_ID = "nandan_reminders"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_NOTE = "reminder_note"
    }
}
