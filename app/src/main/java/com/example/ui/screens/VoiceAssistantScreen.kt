package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.viewmodel.MediVoiceViewModel
import java.util.Locale

@Composable
fun VoiceAssistantScreen(
    viewModel: MediVoiceViewModel
) {
    val lastResult by viewModel.lastAssistantResult.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessingVoice.collectAsStateWithLifecycle()
    val statusMsg by viewModel.voiceStatusMessage.collectAsStateWithLifecycle()
    val voiceLogs by viewModel.voiceLogs.collectAsStateWithLifecycle()

    var recognizedText by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    // Speech Recognizer Intent Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val topSpoken = spokenMatches?.firstOrNull()
            if (!topSpoken.isNullOrBlank()) {
                recognizedText = topSpoken
                viewModel.processVoiceCommand(topSpoken)
            }
        }
    }

    // Pulse animation for microphone button
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isProcessing) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val sampleQueries = listOf(
        "What medicine do I need to take now?",
        "What medicines do I have today?",
        "What is my next medicine?",
        "What is my dosage limit?",
        "Mark Paracetamol as taken",
        "Have I taken my medicine today?",
        "What medicines are pending?",
        "What medicines did I miss today?"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "MediVoice AI Assistant",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Speak naturally to check schedules, dosages, or record taken doses.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Animated Microphone Centerpiece
        item {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer ripple
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (isListening) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                )

                // Central Large Mic Button (Section 21)
                Button(
                    onClick = {
                        isListening = true
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening for medicine command...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            isListening = false
                            // Fallback if device has no speech recognizer app
                            recognizedText = "Speech recognizer unavailable on emulator. You can type commands below."
                        }
                    },
                    modifier = Modifier
                        .size(86.dp)
                        .testTag("voice_assistant_mic_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        modifier = Modifier.size(44.dp),
                        tint = Color.White
                    )
                }
            }

            Text(
                text = when {
                    isListening -> "Listening..."
                    isProcessing -> "Processing with Gemini AI..."
                    else -> "Tap microphone to speak"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        // Recognized Speech & AI Response Card
        if (recognizedText != null || lastResult != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (recognizedText != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You said:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$recognizedText\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        if (lastResult != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MediVoice Assistant:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.speaker.speak(lastResult!!.spokenText) }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Replay Speech",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = lastResult!!.displayText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (lastResult!!.actionTakenMedicine != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✓ Action performed: ${lastResult!!.actionTakenMedicine} marked as Taken in database.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Text command input (fallback for typing)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Or type command (e.g. 'Mark Paracetamol taken')") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val cmd = textInput.trim()
                            recognizedText = cmd
                            textInput = ""
                            viewModel.processVoiceCommand(cmd)
                        }
                    },
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Command")
                }
            }
        }

        // Suggestion Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Suggested Voice Commands",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(sampleQueries) { query ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        recognizedText = query
                        viewModel.processVoiceCommand(query)
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "\"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            MedicalDisclaimerCard()
        }
    }
}
