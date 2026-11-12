package com.example.gestionale_personale.presentation.registration

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.presentation.AppViewModelProvider
import com.example.gestionale_personale.presentation.components.CiottisTopAppBar
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // DatePicker Dialog logic
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            viewModel.onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        uiState.selectedDate.year,
        uiState.selectedDate.monthValue - 1,
        uiState.selectedDate.dayOfMonth
    )

    if (uiState.saveSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSuccessState() },
            title = { Text("Successo") },
            text = { Text("Turno registrato con successo") },
            confirmButton = {
                Button(onClick = { viewModel.resetSuccessState() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CiottisTopAppBar(
                actions = {
                    IconButton(
                        onClick = { viewModel.saveEntries() },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Salva", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addRow() }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Riga")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with Date Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { datePickerDialog.show() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedDate = "${uiState.selectedDate.dayOfMonth} ${uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)} ${uiState.selectedDate.year}"
                    Text(text = formattedDate, style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.DateRange, contentDescription = "Seleziona data")
                }
            }

            // General Notes for the day
            OutlinedTextField(
                value = uiState.generalNotes,
                onValueChange = { viewModel.updateGeneralNotes(it) },
                label = { Text("Note Generali del Giorno (Stato cantieri, ecc.)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 100.dp),
                maxLines = 5
            )

            if (uiState.globalError != null) {
                Text(
                    text = uiState.globalError!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.activeEmployees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun operaio attivo trovato. Aggiungili dalla sezione Operai.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.rows, key = { it.id }) { row ->
                        RegistrationRowItem(
                            rowState = row,
                            activeEmployees = uiState.activeEmployees,
                            onEmployeeSelected = { viewModel.updateRowEmployee(row.id, it) },
                            onToggleMode = { viewModel.toggleTimeMode(row.id) },
                            onHoursChanged = { viewModel.updateRowHours(row.id, it) },
                            onMinutesChanged = { viewModel.updateRowMinutes(row.id, it) },
                            onStartTimeChanged = { viewModel.updateRowStartTime(row.id, it) },
                            onEndTimeChanged = { viewModel.updateRowEndTime(row.id, it) },
                            onWorksiteChanged = { viewModel.updateRowWorksite(row.id, it) },
                            onNotesChanged = { viewModel.updateRowNotes(row.id, it) },
                            onRemove = { viewModel.removeRow(row.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationRowItem(
    rowState: RegistrationRowState,
    activeEmployees: List<Employee>,
    onEmployeeSelected: (String) -> Unit,
    onToggleMode: () -> Unit,
    onHoursChanged: (String) -> Unit,
    onMinutesChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onWorksiteChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val startTimeDialog = TimePickerDialog(context, { _, hour, min ->
        onStartTimeChanged(String.format(Locale.ITALIAN, "%02d:%02d", hour, min))
    }, 8, 0, true)

    val endTimeDialog = TimePickerDialog(context, { _, hour, min ->
        onEndTimeChanged(String.format(Locale.ITALIAN, "%02d:%02d", hour, min))
    }, 17, 0, true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Error Message (if any)
            if (rowState.error != null) {
                Text(
                    text = rowState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dropdown for Employee
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val selectedEmp = activeEmployees.find { it.id == rowState.employeeId }
                    val displayText = selectedEmp?.let { "${it.lastName} ${it.firstName}" } ?: "Seleziona Operaio"

                    OutlinedTextField(
                        value = displayText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        activeEmployees.forEach { employee ->
                            DropdownMenuItem(
                                text = { Text("${employee.lastName} ${employee.firstName}") },
                                onClick = {
                                    onEmployeeSelected(employee.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi riga", tint = MaterialTheme.colorScheme.error)
                }
            }

            // Worksite Name
            OutlinedTextField(
                value = rowState.worksiteName,
                onValueChange = onWorksiteChanged,
                label = { Text("Nome Cantiere (es. Civitanova Marche)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inserisci orario preciso", modifier = Modifier.weight(1f))
                Switch(checked = rowState.isTimeRangeMode, onCheckedChange = { onToggleMode() })
            }

            // Time inputs
            if (rowState.isTimeRangeMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rowState.startTime,
                        onValueChange = { },
                        label = { Text("Inizio (es. 08:00)") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { startTimeDialog.show() },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    OutlinedTextField(
                        value = rowState.endTime,
                        onValueChange = { },
                        label = { Text("Fine (es. 17:00)") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { endTimeDialog.show() },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rowState.hours,
                        onValueChange = onHoursChanged,
                        label = { Text("Ore") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rowState.minutes,
                        onValueChange = onMinutesChanged,
                        label = { Text("Minuti") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = rowState.notes,
                onValueChange = onNotesChanged,
                label = { Text("Note Turno (Opzionale)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
