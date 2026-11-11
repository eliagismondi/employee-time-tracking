package com.example.gestionale_personale.presentation.employees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.presentation.AppViewModelProvider
import com.example.gestionale_personale.presentation.components.CiottisTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    viewModel: EmployeesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val employees by viewModel.employees.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }

    Scaffold(
        topBar = { CiottisTopAppBar() },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Operaio")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(employees) { employee ->
                EmployeeCard(
                    employee = employee,
                    onEditClick = { employeeToEdit = employee },
                    onStatusToggle = { viewModel.toggleEmployeeStatus(employee) }
                )
            }
        }
    }

    if (showAddDialog) {
        EmployeeDialog(
            onDismiss = { showAddDialog = false },
            onSave = { firstName, lastName ->
                viewModel.addEmployee(firstName, lastName)
                showAddDialog = false
            }
        )
    }

    employeeToEdit?.let { employee ->
        EmployeeDialog(
            initialFirstName = employee.firstName,
            initialLastName = employee.lastName,
            onDismiss = { employeeToEdit = null },
            onSave = { firstName, lastName ->
                viewModel.updateEmployee(
                    id = employee.id,
                    firstName = firstName,
                    lastName = lastName,
                    active = employee.active,
                    createdAt = employee.createdAt
                )
                employeeToEdit = null
            }
        )
    }
}

@Composable
fun EmployeeCard(
    employee: Employee,
    onEditClick: () -> Unit,
    onStatusToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${employee.lastName} ${employee.firstName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (employee.active) "Attivo" else "Disattivato",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (employee.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Switch(
                checked = employee.active,
                onCheckedChange = { onStatusToggle() }
            )

            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Modifica")
            }
        }
    }
}

@Composable
fun EmployeeDialog(
    initialFirstName: String = "",
    initialLastName: String = "",
    onDismiss: () -> Unit,
    onSave: (firstName: String, lastName: String) -> Unit
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFirstName.isEmpty()) "Nuovo Operaio" else "Modifica Operaio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Cognome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(firstName, lastName) },
                enabled = firstName.isNotBlank() && lastName.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
