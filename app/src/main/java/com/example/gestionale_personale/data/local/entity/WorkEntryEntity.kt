package com.example.gestionale_personale.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gestionale_personale.domain.model.WorkEntry
import java.time.LocalDate

@Entity(
    tableName = "work_entries",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["employee_id"]),
        Index(value = ["date"])
    ]
)
data class WorkEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "employee_id") val employeeId: String,
    val date: String,
    @ColumnInfo(name = "worked_minutes") val workedMinutes: Int,
    val notes: String?,
    @ColumnInfo(name = "start_time") val startTime: String?,
    @ColumnInfo(name = "end_time") val endTime: String?,
    @ColumnInfo(name = "worksite_name") val worksiteName: String?
) {
    fun toDomain() = WorkEntry(
        id = id,
        employeeId = employeeId,
        date = LocalDate.parse(date),
        workedMinutes = workedMinutes,
        notes = notes,
        startTime = startTime,
        endTime = endTime,
        worksiteName = worksiteName
    )

    companion object {
        fun fromDomain(entry: WorkEntry) = WorkEntryEntity(
            id = entry.id,
            employeeId = entry.employeeId,
            date = entry.date.toString(),
            workedMinutes = entry.workedMinutes,
            notes = entry.notes,
            startTime = entry.startTime,
            endTime = entry.endTime,
            worksiteName = entry.worksiteName
        )
    }
}
