package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String = "",
    val password: String = "",
    val profilePhoto: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val medicineName: String,
    val medicineType: String = "Tablet",
    val dosageName: String,
    val dosageAmount: Double = 1.0,
    val dosageUnit: String = "tablet",
    val dailyDosage: String = "1 tablet",
    val dosageLimit: Int = 2,
    val frequency: String = "Twice a day",
    val reminderTimes: String, // e.g. "08:00 AM,08:00 PM"
    val startDate: String,
    val endDate: String = "",
    val foodInstruction: String = "After Food",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medicine_schedules")
data class MedicineSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val medicineId: Long,
    val medicineName: String,
    val dosageName: String,
    val reminderTime: String,
    val scheduleDate: String, // "YYYY-MM-DD"
    val status: String = "Pending", // Pending, Due Now, Taken, Missed, Skipped, Snoozed
    val foodInstruction: String = "After Food",
    val snoozeUntilMillis: Long = 0L
)

@Entity(tableName = "medicine_history")
data class MedicineHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String,
    val takenTime: String = "",
    val status: String, // Taken, Missed, Skipped
    val date: String, // "YYYY-MM-DD"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val userId: Long,
    val notificationsEnabled: Boolean = true,
    val voiceRemindersEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 10
)

@Entity(tableName = "voice_command_logs")
data class VoiceCommandLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val commandText: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
