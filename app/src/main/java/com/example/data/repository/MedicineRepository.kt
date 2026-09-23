package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.MedicineSchedule
import com.example.data.local.entity.User
import com.example.data.local.entity.UserSettings
import com.example.data.local.entity.VoiceCommandLog
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicineRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val medicineDao = database.medicineDao()
    private val scheduleDao = database.medicineScheduleDao()
    private val historyDao = database.medicineHistoryDao()
    private val settingsDao = database.userSettingsDao()
    private val voiceDao = database.voiceCommandLogDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun getTodayDate(): String = dateFormat.format(Date())
    fun getCurrentTime(): String = timeFormat.format(Date())

    suspend fun registerUser(name: String, email: String, phone: String, password: String):Result<User> {
        val existing = userDao.getUserByEmail(email.trim())
        if (existing != null) {
            return Result.failure(Exception("User with this email already exists"))
        }
        val user = User(
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            password = password
        )
        val id = userDao.insertUser(user)
        val created = user.copy(id = id)
        settingsDao.insertOrUpdate(UserSettings(userId = id))
        return Result.success(created)
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        val user = userDao.getUserByEmail(email.trim()) ?: return Result.failure(Exception("Invalid email or user not found"))
        if (user.password != password) {
            return Result.failure(Exception("Invalid password"))
        }
        return Result.success(user)
    }

    suspend fun getOrSeedDemoUser(): User {
        val existing = userDao.getFirstUser()
        if (existing != null) return existing

        val demoUser = User(
            name = "Sarah Jenkins",
            email = "sarah.jenkins@example.com",
            phone = "+1 (555) 234-5678",
            password = "password123"
        )
        val userId = userDao.insertUser(demoUser)
        settingsDao.insertOrUpdate(UserSettings(userId = userId))

        val today = getTodayDate()
        // Seed initial medicines
        val med1 = Medicine(
            userId = userId,
            medicineName = "Paracetamol",
            medicineType = "Tablet",
            dosageName = "500 mg tablet",
            dosageAmount = 1.0,
            dosageUnit = "tablet",
            dailyDosage = "1 tablet",
            dosageLimit = 2,
            frequency = "Twice a day",
            reminderTimes = "08:00 AM,08:00 PM",
            startDate = today,
            foodInstruction = "After Food",
            notes = "Take with a full glass of water. For mild headaches and joint comfort."
        )
        val medId1 = medicineDao.insertMedicine(med1)

        val med2 = Medicine(
            userId = userId,
            medicineName = "Amoxicillin",
            medicineType = "Capsule",
            dosageName = "500 mg capsule",
            dosageAmount = 1.0,
            dosageUnit = "capsule",
            dailyDosage = "2 capsules",
            dosageLimit = 2,
            frequency = "Twice a day",
            reminderTimes = "09:00 AM,09:00 PM",
            startDate = today,
            foodInstruction = "With Food",
            notes = "Complete the entire antibiotic course as prescribed by Dr. Reynolds."
        )
        val medId2 = medicineDao.insertMedicine(med2)

        val med3 = Medicine(
            userId = userId,
            medicineName = "Vitamin D3",
            medicineType = "Tablet",
            dosageName = "1000 IU tablet",
            dosageAmount = 1.0,
            dosageUnit = "tablet",
            dailyDosage = "1 tablet",
            dosageLimit = 1,
            frequency = "Once a day",
            reminderTimes = "02:00 PM",
            startDate = today,
            foodInstruction = "After Food",
            notes = "Daily dietary supplement recommended with lunch."
        )
        val medId3 = medicineDao.insertMedicine(med3)

        // Generate schedules for today
        scheduleDao.insertSchedule(
            MedicineSchedule(
                userId = userId,
                medicineId = medId1,
                medicineName = med1.medicineName,
                dosageName = med1.dosageName,
                reminderTime = "08:00 AM",
                scheduleDate = today,
                status = "Taken",
                foodInstruction = med1.foodInstruction
            )
        )
        // Add to history
        historyDao.insertHistory(
            MedicineHistory(
                userId = userId,
                medicineId = medId1,
                medicineName = med1.medicineName,
                dosage = med1.dosageName,
                scheduledTime = "08:00 AM",
                takenTime = "08:05 AM",
                status = "Taken",
                date = today,
                notes = "Taken with breakfast."
            )
        )

        scheduleDao.insertSchedule(
            MedicineSchedule(
                userId = userId,
                medicineId = medId2,
                medicineName = med2.medicineName,
                dosageName = med2.dosageName,
                reminderTime = "09:00 AM",
                scheduleDate = today,
                status = "Pending",
                foodInstruction = med2.foodInstruction
            )
        )

        scheduleDao.insertSchedule(
            MedicineSchedule(
                userId = userId,
                medicineId = medId3,
                medicineName = med3.medicineName,
                dosageName = med3.dosageName,
                reminderTime = "02:00 PM",
                scheduleDate = today,
                status = "Pending",
                foodInstruction = med3.foodInstruction
            )
        )

        scheduleDao.insertSchedule(
            MedicineSchedule(
                userId = userId,
                medicineId = medId1,
                medicineName = med1.medicineName,
                dosageName = med1.dosageName,
                reminderTime = "08:00 PM",
                scheduleDate = today,
                status = "Pending",
                foodInstruction = med1.foodInstruction
            )
        )

        scheduleDao.insertSchedule(
            MedicineSchedule(
                userId = userId,
                medicineId = medId2,
                medicineName = med2.medicineName,
                dosageName = med2.dosageName,
                reminderTime = "09:00 PM",
                scheduleDate = today,
                status = "Pending",
                foodInstruction = med2.foodInstruction
            )
        )

        return demoUser.copy(id = userId)
    }

    fun getMedicines(userId: Long): Flow<List<Medicine>> = medicineDao.getActiveMedicines(userId)

    suspend fun getMedicine(id: Long): Medicine? = medicineDao.getMedicineById(id)

    suspend fun addMedicine(medicine: Medicine): Long {
        val medId = medicineDao.insertMedicine(medicine)
        // Generate schedules for today if startDate <= today
        generateSchedulesForDate(medicine.copy(id = medId), getTodayDate())
        return medId
    }

    suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
        // Regenerate future/today's pending schedules
        scheduleDao.deleteUpcomingForMedicine(medicine.id, getTodayDate())
        generateSchedulesForDate(medicine, getTodayDate())
    }

    suspend fun deleteMedicine(medicineId: Long) {
        medicineDao.softDeleteMedicine(medicineId)
        scheduleDao.deleteUpcomingForMedicine(medicineId, getTodayDate())
    }

    private suspend fun generateSchedulesForDate(medicine: Medicine, date: String) {
        val times = medicine.reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val schedules = times.map { time ->
            MedicineSchedule(
                userId = medicine.userId,
                medicineId = medicine.id,
                medicineName = medicine.medicineName,
                dosageName = medicine.dosageName,
                reminderTime = time,
                scheduleDate = date,
                status = "Pending",
                foodInstruction = medicine.foodInstruction
            )
        }
        if (schedules.isNotEmpty()) {
            scheduleDao.insertSchedules(schedules)
        }
    }

    fun getTodaySchedules(userId: Long): Flow<List<MedicineSchedule>> {
        return scheduleDao.getSchedulesForDate(userId, getTodayDate())
    }

    suspend fun getTodaySchedulesList(userId: Long): List<MedicineSchedule> {
        return scheduleDao.getSchedulesForDateList(userId, getTodayDate())
    }

    suspend fun checkDosageLimitExceeded(userId: Long, medicineId: Long): Boolean {
        val medicine = medicineDao.getMedicineById(medicineId) ?: return false
        val takenCount = historyDao.countDosesTakenToday(userId, medicineId, getTodayDate())
        return takenCount >= medicine.dosageLimit
    }

    suspend fun markDoseTaken(scheduleId: Long, customTime: String? = null, notes: String = ""): Result<String> {
        val schedule = scheduleDao.getScheduleById(scheduleId) ?: return Result.failure(Exception("Schedule not found"))
        val takenCount = historyDao.countDosesTakenToday(schedule.userId, schedule.medicineId, schedule.scheduleDate)
        val medicine = medicineDao.getMedicineById(schedule.medicineId)
        val limit = medicine?.dosageLimit ?: 999

        val warning = if (takenCount >= limit) {
            "Warning: Daily dosage limit (${limit} doses) has already been reached for ${schedule.medicineName}."
        } else {
            null
        }

        scheduleDao.updateScheduleStatus(scheduleId, "Taken")
        val takenTimeStr = customTime ?: getCurrentTime()
        historyDao.insertHistory(
            MedicineHistory(
                userId = schedule.userId,
                medicineId = schedule.medicineId,
                medicineName = schedule.medicineName,
                dosage = schedule.dosageName,
                scheduledTime = schedule.reminderTime,
                takenTime = takenTimeStr,
                status = "Taken",
                date = schedule.scheduleDate,
                notes = if (warning != null) (if (notes.isNotEmpty()) "$notes ($warning)" else warning) else notes
            )
        )
        return Result.success(warning ?: "Dose recorded as taken at $takenTimeStr")
    }

    suspend fun markDoseSkipped(scheduleId: Long, reason: String = "") {
        val schedule = scheduleDao.getScheduleById(scheduleId) ?: return
        scheduleDao.updateScheduleStatus(scheduleId, "Skipped")
        historyDao.insertHistory(
            MedicineHistory(
                userId = schedule.userId,
                medicineId = schedule.medicineId,
                medicineName = schedule.medicineName,
                dosage = schedule.dosageName,
                scheduledTime = schedule.reminderTime,
                takenTime = "",
                status = "Skipped",
                date = schedule.scheduleDate,
                notes = reason
            )
        )
    }

    suspend fun markDoseMissed(scheduleId: Long) {
        val schedule = scheduleDao.getScheduleById(scheduleId) ?: return
        scheduleDao.updateScheduleStatus(scheduleId, "Missed")
        historyDao.insertHistory(
            MedicineHistory(
                userId = schedule.userId,
                medicineId = schedule.medicineId,
                medicineName = schedule.medicineName,
                dosage = schedule.dosageName,
                scheduledTime = schedule.reminderTime,
                takenTime = "",
                status = "Missed",
                date = schedule.scheduleDate,
                notes = "Scheduled time elapsed without confirmation."
            )
        )
    }

    suspend fun snoozeDose(scheduleId: Long, minutes: Int) {
        val snoozeUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
        scheduleDao.snoozeSchedule(scheduleId, snoozeUntil)
    }

    fun getAllHistory(userId: Long): Flow<List<MedicineHistory>> = historyDao.getAllHistory(userId)

    fun getHistoryForMedicine(userId: Long, medicineId: Long): Flow<List<MedicineHistory>> =
        historyDao.getHistoryForMedicine(userId, medicineId)

    fun getUserSettings(userId: Long): Flow<UserSettings?> = settingsDao.getSettings(userId)

    suspend fun updateUserSettings(settings: UserSettings) = settingsDao.insertOrUpdate(settings)

    fun getVoiceLogs(userId: Long): Flow<List<VoiceCommandLog>> = voiceDao.getRecentLogs(userId)

    suspend fun logVoiceCommand(userId: Long, command: String, response: String) {
        voiceDao.insertLog(VoiceCommandLog(userId = userId, commandText = command, aiResponse = response))
    }
}
