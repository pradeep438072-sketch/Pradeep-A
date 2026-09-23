package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.local.entity.Medicine
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusTaken
import com.example.ui.viewmodel.MediVoiceViewModel

@Composable
fun MyMedicinesScreen(
    viewModel: MediVoiceViewModel,
    onNavigateToAddMedicine: () -> Unit,
    onNavigateToEditMedicine: (Long) -> Unit,
    onNavigateToMedicineDetail: (Long) -> Unit
) {
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val todaySchedules by viewModel.todaySchedules.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    val filteredMedicines = remember(medicines, searchQuery, selectedFilter) {
        medicines.filter { med ->
            val matchesQuery = med.medicineName.contains(searchQuery, ignoreCase = true) ||
                    med.dosageName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Active" -> med.isActive
                "Tablets" -> med.medicineType.equals("Tablet", ignoreCase = true)
                "Capsules" -> med.medicineType.equals("Capsule", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddMedicine,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Medicine") },
                modifier = Modifier.testTag("fab_add_medicine")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "My Medicines",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your prescriptions, dosage limits, and active medication regimens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search medicine by name or dosage...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_medicine_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Active", "Tablets", "Capsules").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            if (filteredMedicines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalInformation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No medicines recorded yet" else "No matching medicines found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the button below to add your first medicine prescription.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(onClick = onNavigateToAddMedicine) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Medicine")
                            }
                        }
                    }
                }
            } else {
                items(filteredMedicines) { medicine ->
                    val matchingTodaySchedule = todaySchedules.find {
                        it.medicineId == medicine.id && (it.status == "Pending" || it.status == "Due Now")
                    }

                    MedicineCard(
                        medicine = medicine,
                        hasPendingToday = matchingTodaySchedule != null,
                        onViewDetails = { onNavigateToMedicineDetail(medicine.id) },
                        onEdit = { onNavigateToEditMedicine(medicine.id) },
                        onDelete = { medicineToDelete = medicine },
                        onMarkTaken = {
                            if (matchingTodaySchedule != null) {
                                viewModel.markDoseTaken(matchingTodaySchedule.id)
                            } else {
                                // Record directly into history or schedule
                                val anyToday = todaySchedules.firstOrNull { it.medicineId == medicine.id }
                                if (anyToday != null) {
                                    viewModel.markDoseTaken(anyToday.id)
                                }
                            }
                        }
                    )
                }
            }

            item {
                MedicalDisclaimerCard()
            }
        }
    }

    // Delete Confirmation Dialog
    if (medicineToDelete != null) {
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete Medicine?") },
            text = {
                Text("Are you sure you want to remove ${medicineToDelete!!.medicineName}? Your past historical dose records will remain preserved in Medicine History.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicine(medicineToDelete!!.id)
                        medicineToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MedicineCard(
    medicine: Medicine,
    hasPendingToday: Boolean,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkTaken: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (medicine.medicineType) {
                                "Capsule" -> Icons.Default.Science
                                "Syrup" -> Icons.Default.LocalDrink
                                "Injection" -> Icons.Default.Vaccines
                                "Inhaler" -> Icons.Default.Air
                                else -> Icons.Default.Medication
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = medicine.medicineName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${medicine.medicineType} • ${medicine.dosageName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = if (medicine.isActive) "Active" else "Inactive")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Daily Dosage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(medicine.dailyDosage, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Max Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${medicine.dosageLimit} / day", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(medicine.frequency, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Food", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(medicine.foodInstruction, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reminder Times display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Times: ${medicine.reminderTimes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: View, Edit, Delete, Mark as Taken
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View")
                }

                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                if (hasPendingToday) {
                    Button(
                        onClick = onMarkTaken,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusTaken),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Taken")
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun MedicineDetailScreen(
    medicineId: Long,
    viewModel: MediVoiceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val medicine = remember(medicines, medicineId) { medicines.find { it.id == medicineId } }
    val historyList by viewModel.medicineHistory.collectAsStateWithLifecycle()
    val medHistory = remember(historyList, medicineId) { historyList.filter { it.medicineId == medicineId } }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (medicine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Medicine record not found.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = medicine.medicineName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${medicine.medicineType} • ${medicine.dosageName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(status = if (medicine.isActive) "Active" else "Inactive")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow(label = "Daily Dosage", value = medicine.dailyDosage)
                    DetailRow(label = "Max Daily Dosage Limit", value = "${medicine.dosageLimit} per day")
                    DetailRow(label = "Frequency", value = medicine.frequency)
                    DetailRow(label = "Reminder Times", value = medicine.reminderTimes)
                    DetailRow(label = "Food Instructions", value = medicine.foodInstruction)
                    DetailRow(label = "Start Date", value = medicine.startDate)
                    if (medicine.endDate.isNotEmpty()) {
                        DetailRow(label = "End Date", value = medicine.endDate)
                    }
                    if (medicine.notes.isNotEmpty()) {
                        DetailRow(label = "Doctor's Notes", value = medicine.notes)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToEdit(medicine.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Medication History for ${medicine.medicineName} (${medHistory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (medHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No history records yet for this medicine. Recorded doses will appear here automatically.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(medHistory) { history ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${history.date} • ${history.scheduledTime}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (history.takenTime.isNotEmpty()) {
                                Text(
                                    text = "Taken at: ${history.takenTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusTaken
                                )
                            }
                            if (history.notes.isNotEmpty()) {
                                Text(
                                    text = history.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(status = history.status)
                    }
                }
            }
        }

        item {
            MedicalDisclaimerCard()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Medicine") },
            text = { Text("Delete ${medicine.medicineName}? Historical dose records will remain preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicine(medicine.id)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
