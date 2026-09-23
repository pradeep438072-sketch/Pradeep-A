package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.repository.MedicineRepository
import com.example.service.VoiceSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "medivoice_reminders_channel"
        const val CHANNEL_NAME = "MediVoice AI Reminders"
        const val ACTION_TRIGGER_REMINDER = "com.example.medivoice.ACTION_TRIGGER_REMINDER"
        const val ACTION_MARK_TAKEN = "com.example.medivoice.ACTION_MARK_TAKEN"
        const val ACTION_MARK_SKIP = "com.example.medivoice.ACTION_MARK_SKIP"
        const val ACTION_SNOOZE = "com.example.medivoice.ACTION_SNOOZE"

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Medicine Reminders and Medication Schedules"
                    enableVibration(true)
                    setShowBadge(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (System.currentTimeMillis() % 10000).toInt())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val repository = MedicineRepository(AppDatabase.getDatabase(context))

        when (intent.action) {
            ACTION_MARK_TAKEN -> {
                notificationManager.cancel(notificationId)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.markDoseTaken(scheduleId)
                    VoiceSpeaker.getInstance(context).speak("$medicineName recorded as taken.")
                }
            }
            ACTION_MARK_SKIP -> {
                notificationManager.cancel(notificationId)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.markDoseSkipped(scheduleId, "Skipped from notification")
                    VoiceSpeaker.getInstance(context).speak("$medicineName marked as skipped.")
                }
            }
            ACTION_SNOOZE -> {
                notificationManager.cancel(notificationId)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.snoozeDose(scheduleId, 10)
                    VoiceSpeaker.getInstance(context).speak("Reminder for $medicineName snoozed for 10 minutes.")
                }
            }
            ACTION_TRIGGER_REMINDER -> {
                showNotification(context, notificationManager, notificationId, scheduleId, userId, medicineName, dosage)
                // Voice announcement
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.getUserSettings(userId)
                    VoiceSpeaker.getInstance(context).speak(
                        "Medicine reminder. It is time to take $medicineName, $dosage."
                    )
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        manager: NotificationManager,
        notificationId: Int,
        scheduleId: Long,
        userId: Long,
        medicineName: String,
        dosage: String
    ) {
        createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_SKIP
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 30000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("MediVoice AI Reminder")
            .setContentText("Time to take $medicineName – $dosage")
            .setSubText("Medicine Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Taken", takenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip", skipPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 10m", snoozePendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }
}
