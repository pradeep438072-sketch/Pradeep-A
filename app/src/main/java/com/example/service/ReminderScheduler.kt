package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.entity.MedicineSchedule
import com.example.receiver.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(schedule: MedicineSchedule) {
        try {
            val timeParts = schedule.reminderTime.split(" ")
            if (timeParts.size < 2) return

            val hm = timeParts[0].split(":")
            if (hm.size < 2) return

            var hour = hm[0].toIntOrNull() ?: return
            val minute = hm[1].toIntOrNull() ?: return
            val amPm = timeParts[1].uppercase(Locale.getDefault())

            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If time is already passed today, set for next day or ignore
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                // If it was snoozed:
                if (schedule.status == "Snoozed" && schedule.snoozeUntilMillis > System.currentTimeMillis()) {
                    calendar.timeInMillis = schedule.snoozeUntilMillis
                } else {
                    return
                }
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TRIGGER_REMINDER
                putExtra(ReminderReceiver.EXTRA_SCHEDULE_ID, schedule.id)
                putExtra(ReminderReceiver.EXTRA_USER_ID, schedule.userId)
                putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, schedule.medicineName)
                putExtra(ReminderReceiver.EXTRA_DOSAGE, schedule.dosageName)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, schedule.id.toInt())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm: ${e.message}")
        }
    }

    fun triggerTestReminder(schedule: MedicineSchedule) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(ReminderReceiver.EXTRA_USER_ID, schedule.userId)
            putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, schedule.medicineName)
            putExtra(ReminderReceiver.EXTRA_DOSAGE, schedule.dosageName)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, (System.currentTimeMillis() % 10000).toInt())
        }
        context.sendBroadcast(intent)
    }

    fun cancelReminder(scheduleId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
