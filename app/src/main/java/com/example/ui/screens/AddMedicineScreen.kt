package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.viewmodel.MediVoiceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    viewModel: MediVoiceViewModel,
    medicineId: Long = 0L,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val existingMed = remember(medicines, medicineId) {
        if (medicineId > 0) medicines.find { it.id == medicineId } else null
    }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var medicineName by remember(existingMed) { mutableStateOf(existingMed?.medicineName ?: "") }
    var medicineType by remember(existingMed) { mutableStateOf(existingMed?.medicineType ?: "Tablet") }
    var dosageName by remember(existingMed) { mutableStateOf(existingMed?.dosageName ?: "500 mg tablet") }
    var dosageAmount by remember(existingMed) { mutableStateOf(existingMed?.dosageAmount?.toString() ?: "1.0") }
    var dosageUnit by remember(existingMed) { mutableStateOf(existingMed?.dosageUnit ?: "tablet") }
    var dailyDosage by remember(existingMed) { mutableStateOf(existingMed?.dailyDosage ?: "1 tablet") }
    var dosageLimit by remember(existingMed) { mutableStateOf(existingMed?.dosageLimit?.toString() ?: "2") }
    var frequency by remember(existingMed) { mutableStateOf(existingMed?.frequency ?: "Twice a day") }
    var startDate by remember(existingMed) { mutableStateOf(existingMed?.startDate ?: todayDateStr) }
    var endDate by remember(existingMed) { mutableStateOf(existingMed?.endDate ?: "") }
    var foodInstruction by remember(existingMed) { mutableStateOf(existingMed?.foodInstruction ?: "After Food") }
    var notes by remember(existingMed) { mutableStateOf(existingMed?.notes ?: "") }

    // Dynamic list of reminder times
    var reminderTimesList by remember(existingMed) {
        mutableStateOf(
            existingMed?.reminderTimes?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: listOf("08:00 AM", "08:00 PM")
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val medicineTypes = listOf("Tablet", "Capsule", "Syrup", "Injection", "Inhaler", "Drops", "Topical")
    val foodOptions = listOf("Before Food", "After Food", "With Food", "No Restriction")
    val frequencyOptions = listOf("Once a day", "Twice a day", "Three times a day", "Four times a day", "As needed")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (medicineId > 0) "Edit Medicine" else "Add New Medicine",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Enter your prescribed medicine schedule and dosage instructions below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Medicine Name
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("Medicine Name *") },
                    placeholder = { Text("e.g. Paracetamol, Amoxicillin") },
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_medicine_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Medicine Type Selector
                Column {
                    Text(
                        text = "Medicine Form / Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        medicineTypes.take(4).forEach { type ->
                            FilterChip(
                                selected = medicineType == type,
                                onClick = { medicineType = type },
                                label = { Text(type, fontSize = 12.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        medicineTypes.drop(4).forEach { type ->
                            FilterChip(
                                selected = medicineType == type,
                                onClick = { medicineType = type },
                                label = { Text(type, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Dosage Section (Section 3 & 4)
                Text(
                    text = "Dosage & Limit Management",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = dosageName,
                    onValueChange = { dosageName = it },
                    label = { Text("Dosage Specification *") },
                    placeholder = { Text("e.g. 500 mg tablet, 10 ml syrup") },
                    leadingIcon = { Icon(Icons.Default.Healing, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dosage_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dosageAmount,
                        onValueChange = { dosageAmount = it },
                        label = { Text("Dose Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = dosageUnit,
                        onValueChange = { dosageUnit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("tablet, ml, puff") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dailyDosage,
                        onValueChange = { dailyDosage = it },
                        label = { Text("Daily Dosage") },
                        placeholder = { Text("e.g. 1 tablet") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = dosageLimit,
                        onValueChange = { dosageLimit = it },
                        label = { Text("Max Daily Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g. 2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                HorizontalDivider()

                // Frequency & Dynamic Reminder Times
                Text(
                    text = "Reminder Times & Frequency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(text = "Frequency", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        frequencyOptions.take(3).forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                label = { Text(freq, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Reminder times list with time picker
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configured Reminder Times (${reminderTimesList.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val ampm = if (hour >= 12) "PM" else "AM"
                                        val h = if (hour > 12) hour - 12 else (if (hour == 0) 12 else hour)
                                        val formatted = String.format("%02d:%02d %s", h, minute, ampm)
                                        reminderTimesList = reminderTimesList + formatted
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                        ) {
                            Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Time")
                        }
                    }

                    reminderTimesList.forEachIndexed { index, timeStr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dose ${index + 1}: $timeStr",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (reminderTimesList.size > 1) {
                                        reminderTimesList = reminderTimesList.filterIndexed { i, _ -> i != index }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Time", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Food Instructions
                Column {
                    Text(
                        text = "Food Instructions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        foodOptions.forEach { opt ->
                            FilterChip(
                                selected = foodInstruction == opt,
                                onClick = { foodInstruction = opt },
                                label = { Text(opt, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Dates
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date (Optional)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Additional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Doctor's Notes & Instructions") },
                    placeholder = { Text("e.g. Take with full glass of water, complete entire course...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        if (medicineName.isBlank() || dosageName.isBlank()) {
                            errorMessage = "Please enter both medicine name and dosage specification."
                            return@Button
                        }
                        if (reminderTimesList.isEmpty()) {
                            errorMessage = "Please configure at least one reminder time."
                            return@Button
                        }

                        val parsedLimit = dosageLimit.toIntOrNull() ?: 2
                        val parsedAmount = dosageAmount.toDoubleOrNull() ?: 1.0

                        viewModel.saveMedicine(
                            id = medicineId,
                            name = medicineName,
                            type = medicineType,
                            dosageName = dosageName,
                            dosageAmount = parsedAmount,
                            dosageUnit = dosageUnit,
                            dailyDosage = dailyDosage,
                            dosageLimit = parsedLimit,
                            frequency = frequency,
                            reminderTimes = reminderTimesList.joinToString(","),
                            startDate = startDate,
                            endDate = endDate,
                            foodInstruction = foodInstruction,
                            notes = notes,
                            onSuccess = {
                                onNavigateBack()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_medicine_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (medicineId > 0) "Update Medicine" else "Save Medicine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        MedicalDisclaimerCard()
    }
}
