package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MedicineDao
import com.example.data.local.dao.MedicineHistoryDao
import com.example.data.local.dao.MedicineScheduleDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.dao.VoiceCommandLogDao
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.MedicineSchedule
import com.example.data.local.entity.User
import com.example.data.local.entity.UserSettings
import com.example.data.local.entity.VoiceCommandLog

@Database(
    entities = [
        User::class,
        Medicine::class,
        MedicineSchedule::class,
        MedicineHistory::class,
        UserSettings::class,
        VoiceCommandLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun medicineDao(): MedicineDao
    abstract fun medicineScheduleDao(): MedicineScheduleDao
    abstract fun medicineHistoryDao(): MedicineHistoryDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun voiceCommandLogDao(): VoiceCommandLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medivoice_ai.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
