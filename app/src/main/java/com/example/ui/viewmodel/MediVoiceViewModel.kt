package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AssistantResult
import com.example.ai.GeminiAssistant
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.MedicineSchedule
import com.example.data.local.entity.User
import com.example.data.local.entity.UserSettings
import com.example.data.local.entity.VoiceCommandLog
import com.example.data.repository.MedicineRepository
import com.example.service.ReminderScheduler
import com.example.service.VoiceSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MediVoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = MedicineRepository(db)
    private val scheduler = ReminderScheduler(application)
    val speaker = VoiceSpeaker.getInstance(application)
    private val gemini = GeminiAssistant(application, repository)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _activeReminderAlert = MutableStateFlow<MedicineSchedule?>(null)
    val activeReminderAlert: StateFlow<MedicineSchedule?> = _activeReminderAlert.asStateFlow()

    private val _dosageWarning = MutableStateFlow<String?>(null)
    val dosageWarning: StateFlow<String?> = _dosageWarning.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    private val _isProcessingVoice = MutableStateFlow(false)
    val isProcessingVoice: StateFlow<Boolean> = _isProcessingVoice.asStateFlow()

    private val _lastAssistantResult = MutableStateFlow<AssistantResult?>(null)
    val lastAssistantResult: StateFlow<AssistantResult?> = _lastAssistantResult.asStateFlow()

    val todaySchedules: StateFlow<List<MedicineSchedule>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getTodaySchedules(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicines: StateFlow<List<Medicine>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getMedicines(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicineHistory: StateFlow<List<MedicineHistory>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getAllHistory(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings?> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getUserSettings(user.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val voiceLogs: StateFlow<List<VoiceCommandLog>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getVoiceLogs(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-seed demo user so app is immediately usable
        viewModelScope.launch {
            val user = repository.getOrSeedDemoUser()
            _currentUser.value = user
            // Schedule alarms for today's medicines
            val schedules = repository.getTodaySchedulesList(user.id)
            schedules.forEach { s -> scheduler.scheduleReminder(s) }
        }
    }

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.loginUser(email, pass)
            res.onSuccess { user ->
                _currentUser.value = user
                val schedules = repository.getTodaySchedulesList(user.id)
                schedules.forEach { s -> scheduler.scheduleReminder(s) }
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message)
            }
        }
    }

    fun register(name: String, email: String, phone: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.registerUser(name, email, phone, pass)
            res.onSuccess { user ->
                _currentUser.value = user
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        speaker.stop()
    }

    fun saveMedicine(
        id: Long = 0,
        name: String,
        type: String,
        dosageName: String,
        dosageAmount: Double,
        dosageUnit: String,
        dailyDosage: String,
        dosageLimit: Int,
        frequency: String,
        reminderTimes: String,
        startDate: String,
        endDate: String,
        foodInstruction: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val medicine = Medicine(
                id = id,
                userId = user.id,
                medicineName = name.trim(),
                medicineType = type,
                dosageName = dosageName.trim(),
                dosageAmount = dosageAmount,
                dosageUnit = dosageUnit,
                dailyDosage = dailyDosage.trim(),
                dosageLimit = dosageLimit,
                frequency = frequency,
                reminderTimes = reminderTimes.trim(),
                startDate = startDate,
                endDate = endDate,
                foodInstruction = foodInstruction,
                notes = notes.trim()
            )
            if (id == 0L) {
                val newId = repository.addMedicine(medicine)
                val updated = medicine.copy(id = newId)
                // Schedule alarm for new medicine
                val schedules = repository.getTodaySchedulesList(user.id)
                schedules.filter { it.medicineId == newId }.forEach { scheduler.scheduleReminder(it) }
            } else {
                repository.updateMedicine(medicine)
                val schedules = repository.getTodaySchedulesList(user.id)
                schedules.filter { it.medicineId == medicine.id }.forEach { scheduler.scheduleReminder(it) }
            }
            onSuccess()
        }
    }

    fun deleteMedicine(medicineId: Long) {
        viewModelScope.launch {
            repository.deleteMedicine(medicineId)
            scheduler.cancelReminder(medicineId)
        }
    }

    fun markDoseTaken(scheduleId: Long, customTime: String? = null, notes: String = "") {
        viewModelScope.launch {
            val res = repository.markDoseTaken(scheduleId, customTime, notes)
            res.onSuccess { msg ->
                if (msg.startsWith("Warning")) {
                    _dosageWarning.value = msg
                }
                speaker.speak("Dose recorded as taken.")
            }
        }
    }

    fun markDoseSkipped(scheduleId: Long, reason: String = "") {
        viewModelScope.launch {
            repository.markDoseSkipped(scheduleId, reason)
            speaker.speak("Dose skipped.")
        }
    }

    fun snoozeDose(scheduleId: Long, minutes: Int = 10) {
        viewModelScope.launch {
            repository.snoozeDose(scheduleId, minutes)
            speaker.speak("Reminder snoozed for $minutes minutes.")
        }
    }

    fun triggerTestReminder(schedule: MedicineSchedule) {
        _activeReminderAlert.value = schedule
        scheduler.triggerTestReminder(schedule)
        speaker.speak("Medicine reminder. It is time to take ${schedule.medicineName}, ${schedule.dosageName}.")
    }

    fun dismissReminderAlert() {
        _activeReminderAlert.value = null
    }

    fun clearDosageWarning() {
        _dosageWarning.value = null
    }

    fun processVoiceCommand(command: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isProcessingVoice.value = true
            _voiceStatusMessage.value = "Thinking..."
            try {
                val result = gemini.processCommand(user.id, user.name, command)
                _lastAssistantResult.value = result
                _voiceStatusMessage.value = null
                speaker.speak(result.spokenText)
                if (result.warningMessage != null) {
                    _dosageWarning.value = result.warningMessage
                }
            } catch (e: Exception) {
                _voiceStatusMessage.value = "Error: ${e.message}"
            } finally {
                _isProcessingVoice.value = false
            }
        }
    }

    fun updateSettings(notificationsEnabled: Boolean, voiceRemindersEnabled: Boolean, snoozeMinutes: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val settings = UserSettings(
                userId = user.id,
                notificationsEnabled = notificationsEnabled,
                voiceRemindersEnabled = voiceRemindersEnabled,
                snoozeDurationMinutes = snoozeMinutes
            )
            repository.updateUserSettings(settings)
        }
    }
}
