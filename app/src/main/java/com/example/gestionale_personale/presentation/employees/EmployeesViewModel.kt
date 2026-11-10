package com.example.gestionale_personale.presentation.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class EmployeesViewModel(
    private val repository: EmployeeRepository
) : ViewModel() {

    val employees: StateFlow<List<Employee>> = repository.getAllEmployees()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addEmployee(firstName: String, lastName: String) {
        if (firstName.isBlank() || lastName.isBlank()) return

        viewModelScope.launch {
            val newEmployee = Employee(
                id = UUID.randomUUID().toString(),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                active = true,
                createdAt = System.currentTimeMillis()
            )
            repository.saveEmployee(newEmployee)
        }
    }

    fun updateEmployee(id: String, firstName: String, lastName: String, active: Boolean, createdAt: Long) {
        if (firstName.isBlank() || lastName.isBlank()) return

        viewModelScope.launch {
            val updated = Employee(
                id = id,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                active = active,
                createdAt = createdAt
            )
            repository.saveEmployee(updated)
        }
    }

    fun toggleEmployeeStatus(employee: Employee) {
        viewModelScope.launch {
            repository.saveEmployee(employee.copy(active = !employee.active))
        }
    }
}
