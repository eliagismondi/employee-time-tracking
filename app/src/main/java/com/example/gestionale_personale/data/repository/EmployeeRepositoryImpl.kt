package com.example.gestionale_personale.data.repository

import com.example.gestionale_personale.data.local.dao.EmployeeDao
import com.example.gestionale_personale.data.local.entity.EmployeeEntity
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmployeeRepositoryImpl(
    private val employeeDao: EmployeeDao
) : EmployeeRepository {

    override fun getAllEmployees(): Flow<List<Employee>> {
        return employeeDao.getAllEmployees().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveEmployees(): Flow<List<Employee>> {
        return employeeDao.getActiveEmployees().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEmployeeById(id: String): Employee? {
        return employeeDao.getEmployeeById(id)?.toDomain()
    }

    override suspend fun saveEmployee(employee: Employee) {
        employeeDao.insertEmployee(EmployeeEntity.fromDomain(employee))
    }
}
