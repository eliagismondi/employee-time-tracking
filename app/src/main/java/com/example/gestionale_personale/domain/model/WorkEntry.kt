package com.example.gestionale_personale.domain.model

import java.time.LocalDate

data class WorkEntry(
    val id: String,
    val employeeId: String,
    val date: LocalDate,
    val workedMinutes: Int,
    val notes: String? = null,
    val startTime: String? = null,
    val endTime: String? = null
)
