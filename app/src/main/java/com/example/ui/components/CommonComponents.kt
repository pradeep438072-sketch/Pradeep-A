package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MedicineSchedule
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediVoiceTopBar(
    title: String,
    userName: String? = null,
    onVoiceClick: () -> Unit,
    onProfileClick: () -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!userName.isNullOrEmpty()) {
                    Text(
                        text = "User: $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back"
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "MediVoice AI Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier.testTag("top_bar_voice_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.testTag("top_bar_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Profile",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Taken" -> StatusTaken.copy(alpha = 0.15f) to StatusTaken
        "Missed" -> StatusMissed.copy(alpha = 0.15f) to StatusMissed
        "Skipped" -> StatusSkipped.copy(alpha = 0.15f) to StatusSkipped
        "Due Now" -> StatusDueNow.copy(alpha = 0.15f) to StatusDueNow
        "Snoozed" -> Color(0xFF8B5CF6).copy(alpha = 0.15f) to Color(0xFF8B5CF6)
        else -> StatusPending.copy(alpha = 0.15f) to StatusPending
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MedicalDisclaimerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7).copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Medical Safety Disclaimer",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MediVoice AI is intended for medicine organization and reminders only. It does not provide medical diagnosis or prescribe medication. Always follow instructions from your doctor or healthcare professional.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF92400E),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ReminderPopUpDialog(
    schedule: MedicineSchedule,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSnoozeMinutes by remember { mutableStateOf(10) }
    var showSnoozeOptions by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Reminder Alert",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = "Medicine Reminder",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Time to take ${schedule.medicineName} – ${schedule.dosageName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scheduled for ${schedule.reminderTime} • ${schedule.foodInstruction}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (showSnoozeOptions) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select Snooze Duration:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = selectedSnoozeMinutes == mins,
                                onClick = { selectedSnoozeMinutes = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTaken()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusTaken),
                modifier = Modifier.testTag("dialog_taken_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Taken")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onSkip()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("dialog_skip_button")
                ) {
                    Text("Skip")
                }
                FilledTonalButton(
                    onClick = {
                        if (!showSnoozeOptions) {
                            showSnoozeOptions = true
                        } else {
                            onSnooze(selectedSnoozeMinutes)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("dialog_snooze_button")
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showSnoozeOptions) "Confirm (${selectedSnoozeMinutes}m)" else "Snooze")
                }
            }
        }
    )
}

@Composable
fun DosageWarningBanner(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Dosage Limit Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Dosage Limit Protection",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daily dosage limit reached. Do not exceed the maximum daily dosage prescribed by your healthcare provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_dosage_warning_btn")
            ) {
                Text("I Understand")
            }
        }
    )
}
