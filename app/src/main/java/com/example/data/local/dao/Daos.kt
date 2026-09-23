package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.MedicineSchedule
import com.example.data.local.entity.User
import com.example.data.local.entity.UserSettings
import com.example.data.local.entity.VoiceCommandLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): User?

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE userId = :userId AND isActive = 1 ORDER BY medicineName ASC")
    fun getActiveMedicines(userId: Long): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE userId = :userId AND isActive = 1 ORDER BY medicineName ASC")
    suspend fun getActiveMedicinesList(userId: Long): List<Medicine>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Query("UPDATE medicines SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteMedicine(id: Long)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicineById(id: Long)
}

@Dao
interface MedicineScheduleDao {
    @Query("SELECT * FROM medicine_schedules WHERE userId = :userId AND scheduleDate = :date ORDER BY reminderTime ASC")
    fun getSchedulesForDate(userId: Long, date: String): Flow<List<MedicineSchedule>>

    @Query("SELECT * FROM medicine_schedules WHERE userId = :userId AND scheduleDate = :date ORDER BY reminderTime ASC")
    suspend fun getSchedulesForDateList(userId: Long, date: String): List<MedicineSchedule>

    @Query("SELECT * FROM medicine_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): MedicineSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicineSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<MedicineSchedule>)

    @Update
    suspend fun updateSchedule(schedule: MedicineSchedule)

    @Query("UPDATE medicine_schedules SET status = :status WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, status: String)

    @Query("UPDATE medicine_schedules SET status = 'Snoozed', snoozeUntilMillis = :snoozeUntil WHERE id = :id")
    suspend fun snoozeSchedule(id: Long, snoozeUntil: Long)

    @Query("DELETE FROM medicine_schedules WHERE medicineId = :medicineId AND scheduleDate >= :fromDate")
    suspend fun deleteUpcomingForMedicine(medicineId: Long, fromDate: String)
}

@Dao
interface MedicineHistoryDao {
    @Query("SELECT * FROM medicine_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistory(userId: Long): Flow<List<MedicineHistory>>

    @Query("SELECT * FROM medicine_history WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllHistoryList(userId: Long): List<MedicineHistory>

    @Query("SELECT * FROM medicine_history WHERE userId = :userId AND medicineId = :medicineId ORDER BY timestamp DESC")
    fun getHistoryForMedicine(userId: Long, medicineId: Long): Flow<List<MedicineHistory>>

    @Query("SELECT * FROM medicine_history WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    suspend fun getHistoryForDate(userId: Long, date: String): List<MedicineHistory>

    @Query("SELECT COUNT(*) FROM medicine_history WHERE userId = :userId AND medicineId = :medicineId AND date = :date AND status = 'Taken'")
    suspend fun countDosesTakenToday(userId: Long, medicineId: Long, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MedicineHistory): Long
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun getSettings(userId: Long): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettingsSync(userId: Long): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettings)
}

@Dao
interface VoiceCommandLogDao {
    @Query("SELECT * FROM voice_command_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 20")
    fun getRecentLogs(userId: Long): Flow<List<VoiceCommandLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VoiceCommandLog): Long
}
