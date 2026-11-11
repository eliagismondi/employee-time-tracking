package com.example.gestionale_personale.domain.model

import java.time.LocalDate
import java.time.YearMonth

// Modelli di dominio specifici per la generazione dei PDF
// Mantenuti separati da WorkEntry per pura responsabilità architetturale

data class DailySummary(
    val date: LocalDate,
    val entries: List<EmployeeDailyEntry>,
    val totalMinutes: Int
)

data class EmployeeDailyEntry(
    val employeeName: String,
    val workedMinutes: Int,
    val notes: String?
)

data class MonthlySummary(
    val month: YearMonth,
    val employeeSummaries: List<EmployeeMonthlyEntry>,
    val totalCompanyMinutes: Int
)

data class EmployeeMonthlyEntry(
    val employeeName: String,
    val totalMinutes: Int
)
