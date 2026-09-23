package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.local.entity.MedicineHistory
import com.example.data.local.entity.MedicineSchedule
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediVoiceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScheduleScreen(
    viewModel: MediVoiceViewModel,
    onNavigateToAddMedicine: () -> Unit
) {
    val schedules by viewModel.todaySchedules.collectAsStateWithLifecycle()
    var scheduleForSnooze by remember { mutableStateOf<MedicineSchedule?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Today's Medicine Schedule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Chronological timeline of all your scheduled doses for today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (schedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No doses scheduled for today",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add medicine prescriptions to populate your daily schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onNavigateToAddMedicine) {
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
                                label = { Text("${mins}m") }
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
                    Text("Confirm")
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
fun MedicineHistoryScreen(
    viewModel: MediVoiceViewModel
) {
    val historyList by viewModel.medicineHistory.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterdayDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    val filteredList = remember(historyList, searchQuery, selectedDateFilter, selectedStatusFilter) {
        historyList.filter { history ->
            val matchesQuery = history.medicineName.contains(searchQuery, ignoreCase = true) ||
                    history.dosage.contains(searchQuery, ignoreCase = true)

            val matchesDate = when (selectedDateFilter) {
                "Today" -> history.date == todayDateStr
                "Yesterday" -> history.date == yesterdayDateStr
                "Last 7 Days" -> {
                    // Simple check within last 7 days
                    val diff = System.currentTimeMillis() - history.timestamp
                    diff <= 7L * 24 * 60 * 60 * 1000
                }
                "Last 30 Days" -> {
                    val diff = System.currentTimeMillis() - history.timestamp
                    diff <= 30L * 24 * 60 * 60 * 1000
                }
                else -> true
            }

            val matchesStatus = when (selectedStatusFilter) {
                "Taken" -> history.status == "Taken"
                "Missed" -> history.status == "Missed"
                "Skipped" -> history.status == "Skipped"
                else -> true
            }

            matchesQuery && matchesDate && matchesStatus
        }
    }

    // Statistics Calculation (Section 7)
    val totalRecords = historyList.size
    val totalTaken = historyList.count { it.status == "Taken" }
    val totalMissed = historyList.count { it.status == "Missed" }
    val totalSkipped = historyList.count { it.status == "Skipped" }
    val adherencePercent = if (totalRecords > 0) {
        ((totalTaken.toFloat() / totalRecords.toFloat()) * 100).toInt()
    } else 100

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Medicine History & Adherence",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Permanent audit log of all recorded, skipped, and missed doses.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Adherence Statistics Card & Visual Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Medication Adherence Score",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$adherencePercent%",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (adherencePercent >= 80) StatusTaken else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Visual Progress Bar
                    LinearProgressIndicator(
                        progress = { (adherencePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (adherencePercent >= 80) StatusTaken else MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Stat Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HistoryStatItem(label = "Total Taken", value = totalTaken.toString(), color = StatusTaken)
                        HistoryStatItem(label = "Missed", value = totalMissed.toString(), color = StatusMissed)
                        HistoryStatItem(label = "Skipped", value = totalSkipped.toString(), color = StatusSkipped)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by medicine name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date Filters
            Text(text = "Filter by Timeframe:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Today", "Yesterday", "Last 7 Days", "Last 30 Days").forEach { dateF ->
                    FilterChip(
                        selected = selectedDateFilter == dateF,
                        onClick = { selectedDateFilter = dateF },
                        label = { Text(dateF, fontSize = 11.sp) }
                    )
                }
            }

            // Status Filters
            Text(text = "Filter by Status:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Taken", "Missed", "Skipped").forEach { st ->
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = { Text(st, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(28.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No history records matching the filter criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList) { history ->
                HistoryRecordCard(history = history)
            }
        }

        item {
            MedicalDisclaimerCard()
        }
    }
}

@Composable
fun HistoryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistoryRecordCard(history: MedicineHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.medicineName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dosage: ${history.dosage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scheduled: ${history.date} at ${history.scheduledTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (history.takenTime.isNotEmpty()) {
                    Text(
                        text = "Actual Taken Time: ${history.takenTime}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusTaken
                    )
                }
                if (history.notes.isNotEmpty()) {
                    Text(
                        text = "Note: ${history.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = history.status)
        }
    }
}
