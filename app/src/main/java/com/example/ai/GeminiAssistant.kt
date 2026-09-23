package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.Medicine
import com.example.data.local.entity.MedicineSchedule
import com.example.data.repository.MedicineRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class AssistantResult(
    val spokenText: String,
    val displayText: String,
    val actionTakenMedicine: String? = null,
    val warningMessage: String? = null
)

class GeminiAssistant(
    private val context: Context,
    private val repository: MedicineRepository
) {
    private val apiService: GeminiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun processCommand(
        userId: Long,
        userName: String,
        userInput: String
    ): AssistantResult = withContext(Dispatchers.IO) {
        val schedules = repository.getTodaySchedulesList(userId)
        val medicines = repository.getTodayDate().let {
            repository.getTodaySchedulesList(userId)
        }
        val activeMedicines = repository.getTodayDate().let {
            // get medicines list from dao
            try {
                // query
            } catch (e: Exception) {}
        }

        val currentTime = repository.getCurrentTime()
        val currentDate = repository.getTodayDate()

        // Check if Gemini API key is available
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                return@withContext callGemini(userId, userName, userInput, schedules, currentTime, currentDate, apiKey)
            } catch (e: Exception) {
                Log.e("GeminiAssistant", "Gemini call failed, falling back to local NLP: ${e.message}")
            }
        }

        // Fallback / Offline NLP processor
        processOfflineCommand(userId, userInput, schedules, currentTime)
    }

    private suspend fun callGemini(
        userId: Long,
        userName: String,
        userInput: String,
        schedules: List<MedicineSchedule>,
        currentTime: String,
        currentDate: String,
        apiKey: String
    ): AssistantResult {
        val scheduleSummary = if (schedules.isEmpty()) {
            "No medicines scheduled for today."
        } else {
            schedules.joinToString("\n") { s ->
                "- ${s.medicineName} (${s.dosageName}) at ${s.reminderTime}: Status = ${s.status}, Food: ${s.foodInstruction}"
            }
        }

        val systemPrompt = """
            You are MediVoice AI, a compassionate and precise medicine reminder voice assistant for $userName.
            Current Date: $currentDate, Current Time: $currentTime.
            Today's Scheduled Medicines:
            $scheduleSummary

            Rules:
            1. Keep responses clear, warm, and natural for text-to-speech (max 2-3 sentences).
            2. Never prescribe medications or make clinical medical diagnoses.
            3. If the user indicates they took a medicine (e.g., 'I took Paracetamol', 'Mark vitamin taken'), identify the medicine name and include this tag at the very end: [ACTION_TAKEN: ExactMedicineName].
            4. If the user asks about their schedule, next medicine, dosage, or missed pills, answer directly based on today's schedule.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userInput)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.2f, maxOutputTokens = 300)
        )

        val response = apiService.generateContent(apiKey, request)
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "I'm sorry, I couldn't process that request right now."

        var cleanText = rawText.trim()
        var actionMed: String? = null
        var warning: String? = null

        val actionRegex = Regex("""\[ACTION_TAKEN:\s*(.+?)\]""")
        val match = actionRegex.find(rawText)
        if (match != null) {
            actionMed = match.groupValues[1].trim()
            cleanText = rawText.replace(match.value, "").trim()
            // Perform action in database
            val matchingSchedule = schedules.find {
                it.medicineName.equals(actionMed, ignoreCase = true) && it.status != "Taken"
            } ?: schedules.find { it.medicineName.contains(actionMed, ignoreCase = true) }

            if (matchingSchedule != null) {
                val result = repository.markDoseTaken(matchingSchedule.id)
                result.onSuccess { msg ->
                    if (msg.startsWith("Warning")) warning = msg
                }
            }
        }

        repository.logVoiceCommand(userId, userInput, cleanText)
        return AssistantResult(
            spokenText = cleanText,
            displayText = cleanText,
            actionTakenMedicine = actionMed,
            warningMessage = warning
        )
    }

    private suspend fun processOfflineCommand(
        userId: Long,
        input: String,
        schedules: List<MedicineSchedule>,
        currentTime: String
    ): AssistantResult {
        val lower = input.lowercase().trim()

        // 1. Mark as taken
        if (lower.contains("taken") || lower.contains("took") || lower.contains("mark")) {
            val matchingSchedule = schedules.find { s ->
                lower.contains(s.medicineName.lowercase()) && s.status != "Taken"
            } ?: schedules.firstOrNull { it.status == "Pending" || it.status == "Due Now" }

            if (matchingSchedule != null) {
                val result = repository.markDoseTaken(matchingSchedule.id)
                var warning: String? = null
                result.onSuccess { msg ->
                    if (msg.startsWith("Warning")) warning = msg
                }
                val spoken = "I have marked ${matchingSchedule.medicineName} (${matchingSchedule.dosageName}) as taken."
                repository.logVoiceCommand(userId, input, spoken)
                return AssistantResult(
                    spokenText = spoken,
                    displayText = spoken,
                    actionTakenMedicine = matchingSchedule.medicineName,
                    warningMessage = warning
                )
            } else {
                val text = "I couldn't find any pending medicine matching that name today."
                repository.logVoiceCommand(userId, input, text)
                return AssistantResult(spokenText = text, displayText = text)
            }
        }

        // 2. What medicine now / next
        if (lower.contains("now") || lower.contains("next") || lower.contains("upcoming")) {
            val nextSchedule = schedules.firstOrNull { it.status == "Pending" || it.status == "Due Now" }
            val text = if (nextSchedule != null) {
                "Your next medicine is ${nextSchedule.medicineName}, ${nextSchedule.dosageName}, scheduled for ${nextSchedule.reminderTime} (${nextSchedule.foodInstruction})."
            } else {
                "You have no pending medicines for today. All scheduled doses are completed!"
            }
            repository.logVoiceCommand(userId, input, text)
            return AssistantResult(spokenText = text, displayText = text)
        }

        // 3. Today's schedule
        if (lower.contains("today") || lower.contains("schedule") || lower.contains("all medicine")) {
            val text = if (schedules.isEmpty()) {
                "You have no medicines scheduled for today."
            } else {
                val listStr = schedules.joinToString(", ") { "${it.medicineName} at ${it.reminderTime} (${it.status})" }
                "Today you have ${schedules.size} scheduled doses: $listStr."
            }
            repository.logVoiceCommand(userId, input, text)
            return AssistantResult(spokenText = text, displayText = text)
        }

        // 4. Missed or pending
        if (lower.contains("missed")) {
            val missed = schedules.filter { it.status == "Missed" }
            val text = if (missed.isEmpty()) {
                "Great news! You have no missed medicines today."
            } else {
                val mNames = missed.joinToString(", ") { "${it.medicineName} (${it.reminderTime})" }
                "You missed ${missed.size} medicine today: $mNames. Please take it or consult your doctor."
            }
            repository.logVoiceCommand(userId, input, text)
            return AssistantResult(spokenText = text, displayText = text)
        }

        if (lower.contains("pending") || lower.contains("remaining")) {
            val pending = schedules.filter { it.status == "Pending" || it.status == "Due Now" }
            val text = if (pending.isEmpty()) {
                "All medicines for today have been taken!"
            } else {
                val pNames = pending.joinToString(", ") { "${it.medicineName} at ${it.reminderTime}" }
                "You have ${pending.size} pending medicine: $pNames."
            }
            repository.logVoiceCommand(userId, input, text)
            return AssistantResult(spokenText = text, displayText = text)
        }

        // 5. Dosage query
        if (lower.contains("dosage") || lower.contains("limit")) {
            val target = schedules.find { lower.contains(it.medicineName.lowercase()) }
            val text = if (target != null) {
                "The scheduled dosage for ${target.medicineName} is ${target.dosageName}, to be taken ${target.foodInstruction}."
            } else {
                "You can view and configure the dosage and daily limit for each medicine in the My Medicines section."
            }
            repository.logVoiceCommand(userId, input, text)
            return AssistantResult(spokenText = text, displayText = text)
        }

        // 6. History query
        if (lower.contains("history")) {
            val taken = schedules.filter { it.status == "Taken" }
            val text = if (taken.isEmpty()) {
                "No medicines have been marked as taken yet today. You can view your full history in the Medicine History tab."
            } else {
                val tNames = taken.joinToString(", ") { "${it.medicineName} at ${it.reminderTime}" }
                "Today you have completed: $tNames. Your full history is stored in the Medicine History tab."
            }
            repository.logVoiceCommand(userId, input, text)
            return AssistantResult(spokenText = text, displayText = text)
        }

        // Default friendly response
        val defaultText = "I heard you say: '$input'. I can help check your next medicine, review today's schedule, or mark a medicine as taken."
        repository.logVoiceCommand(userId, input, defaultText)
        return AssistantResult(spokenText = defaultText, displayText = defaultText)
    }
}
