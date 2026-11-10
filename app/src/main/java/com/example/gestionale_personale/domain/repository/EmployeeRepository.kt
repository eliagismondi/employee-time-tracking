package com.example.gestionale_personale.domain.repository

import com.example.gestionale_personale.domain.model.Employee
import kotlinx.coroutines.flow.Flow

interface EmployeeRepository {
    fun getAllEmployees(): Flow<List<Employee>>
    fun getActiveEmployees(): Flow<List<Employee>>
    suspend fun getEmployeeById(id: String): Employee?
    suspend fun saveEmployee(employee: Employee)
}
