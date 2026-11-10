package com.example.gestionale_personale.presentation.registration

import android.app.DatePickerDialog
import android.widget.DatePicker
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
            text = { Text("Le ore sono state salvate correttamente nel database.") },
            confirmButton = {
                Button(onClick = { viewModel.resetSuccessState() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registra ore") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.saveEntries() },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Salva")
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
                            onHoursChanged = { viewModel.updateRowHours(row.id, it) },
                            onMinutesChanged = { viewModel.updateRowMinutes(row.id, it) },
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
    onHoursChanged: (String) -> Unit,
    onMinutesChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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

            // Time inputs
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

            // Notes
            OutlinedTextField(
                value = rowState.notes,
                onValueChange = onNotesChanged,
                label = { Text("Note (Opzionale)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
