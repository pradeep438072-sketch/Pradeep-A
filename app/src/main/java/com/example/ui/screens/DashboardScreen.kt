package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.MedicineSchedule
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediVoiceViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MediVoiceViewModel,
    onNavigateToAddMedicine: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToMedicines: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val schedules by viewModel.todaySchedules.collectAsStateWithLifecycle()

    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    // Live clock ticker
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeStr = timeFmt.format(now)
            currentDateStr = dateFmt.format(now)
            delay(1000)
        }
    }

    val totalCount = schedules.size
    val takenCount = schedules.count { it.status == "Taken" }
    val pendingCount = schedules.count { it.status == "Pending" || it.status == "Due Now" || it.status == "Snoozed" }
    val missedCount = schedules.count { it.status == "Missed" }
    val nextReminder = schedules.firstOrNull { it.status == "Pending" || it.status == "Due Now" || it.status == "Snoozed" }

    var scheduleForSnooze by remember { mutableStateOf<MedicineSchedule?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Header & Real-time clock
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${currentUser?.name ?: "Patient"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentDateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentTimeStr,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Voice Assistant Circle Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onNavigateToVoice() }
                            .testTag("dashboard_quick_voice_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Open Voice Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Summary Metric Cards Grid
        item {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Total & Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Medicines",
                    count = totalCount.toString(),
                    icon = Icons.Default.Medication,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Next Reminder",
                    count = nextReminder?.reminderTime ?: "All Done",
                    icon = Icons.Default.Alarm,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    subtitle = nextReminder?.medicineName ?: "No pending doses",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Taken, Pending, Missed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Taken",
                    count = takenCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    iconTint = StatusTaken,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Pending",
                    count = pendingCount.toString(),
                    icon = Icons.Default.HourglassEmpty,
                    iconTint = StatusPending,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Missed",
                    count = missedCount.toString(),
                    icon = Icons.Default.Cancel,
                    iconTint = StatusMissed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Next Upcoming Reminder Action Banner
        if (nextReminder != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Up Next at ${nextReminder.reminderTime}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF0284C7)
                                )
                            }
                            StatusBadge(status = nextReminder.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${nextReminder.medicineName} – ${nextReminder.dosageName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Instruction: ${nextReminder.foodInstruction}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.markDoseTaken(nextReminder.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusTaken),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("next_reminder_taken_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Taken")
                            }

                            FilledTonalButton(
                                onClick = { scheduleForSnooze = nextReminder },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("next_reminder_snooze_btn")
                            ) {
                                Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Snooze")
                            }

                            IconButton(
                                onClick = { viewModel.triggerTestReminder(nextReminder) },
                                modifier = Modifier.testTag("test_voice_reminder_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Test Voice Announcement",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Medicine Schedule Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Schedule (${schedules.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToSchedule) {
                    Text("View Full Timeline")
                }
            }
        }

        // Today's Scheduled Dose Items
        if (schedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No medicines scheduled for today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your prescribed medicines to start tracking reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNavigateToAddMedicine) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Medicine")
                        }
                    }
                }
            }
        } else {
            items(schedules) { schedule ->
                ScheduleItemCard(
                    schedule = schedule,
                    onTaken = { viewModel.markDoseTaken(schedule.id) },
                    onSkip = { viewModel.markDoseSkipped(schedule.id) },
                    onSnooze = { scheduleForSnooze = schedule },
                    onSpeak = { viewModel.triggerTestReminder(schedule) }
                )
            }
        }

        item {
            MedicalDisclaimerCard()
        }
    }

    // Snooze selector modal dialog
    if (scheduleForSnooze != null) {
        var snoozeDuration by remember { mutableStateOf(10) }
        AlertDialog(
            onDismissRequest = { scheduleForSnooze = null },
            title = { Text("Snooze Reminder") },
            text = {
                Column {
                    Text("Remind again for ${scheduleForSnooze!!.medicineName} in:")
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = snoozeDuration == mins,
                                onClick = { snoozeDuration = mins },
                                label = { Text("${mins} min") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.snoozeDose(scheduleForSnooze!!.id, snoozeDuration)
                    scheduleForSnooze = null
                }) {
                    Text("Confirm Snooze")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleForSnooze = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    schedule: MedicineSchedule,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = schedule.reminderTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = schedule.foodInstruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = schedule.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = schedule.medicineName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dosage: ${schedule.dosageName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            if (schedule.status != "Taken") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTaken,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusTaken),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Taken")
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Skip")
                    }

                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze")
                    }

                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Reminder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusTaken.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusTaken,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Completed for today",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusTaken,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
