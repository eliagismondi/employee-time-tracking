package com.example.gestionale_personale.domain.model

import java.sql.Timestamp

data class WorkEntry(
    val id: Int,
    val employeeId: Int,
    val date: Timestamp,
    val workedMinutes: Int,
    val notes: String?,
    val worksite: String,
)