package com.example.gestionale_personale.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gestionale_personale.domain.model.Employee

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    fun toDomain() = Employee(
        id = id,
        firstName = firstName,
        lastName = lastName,
        active = active,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(employee: Employee) = EmployeeEntity(
            id = employee.id,
            firstName = employee.firstName,
            lastName = employee.lastName,
            active = employee.active,
            createdAt = employee.createdAt
        )
    }
}
