package com.example.gestionale_personale.domain.usecase

import android.content.Context
import com.example.gestionale_personale.domain.model.DailySummary
import com.example.gestionale_personale.domain.model.EmployeeDailyEntry
import com.example.gestionale_personale.domain.model.EmployeeMonthlyEntry
import com.example.gestionale_personale.domain.model.MonthlySummary
import com.example.gestionale_personale.domain.repository.DailyGeneralNotesRepository
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import com.example.gestionale_personale.domain.repository.WorkEntryRepository
import com.example.gestionale_personale.pdf.PdfGenerator
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class PdfExportUseCase(
    private val workEntryRepository: WorkEntryRepository,
    private val employeeRepository: EmployeeRepository,
    private val dailyGeneralNotesRepository: DailyGeneralNotesRepository,
    private val context: Context
) {
    private val pdfGenerator = PdfGenerator(context)

    suspend fun exportDailyReport(date: LocalDate): File? {
        val entries = workEntryRepository.getEntriesForDate(date).first()
        val generalNotes = dailyGeneralNotesRepository.getNotesForDate(date).first()?.notes ?: ""

        if (entries.isEmpty() && generalNotes.isBlank()) return null

        val employees = employeeRepository.getAllEmployees().first()

        val dailyEntries = entries.map { entry ->
            val emp = employees.find { it.id == entry.employeeId }
            val name = emp?.let { "${it.lastName} ${it.firstName}" } ?: "Dipendente Rimosso"

            EmployeeDailyEntry(
                employeeName = name,
                workedMinutes = entry.workedMinutes,
                startTime = entry.startTime,
                endTime = entry.endTime,
                worksiteName = entry.worksiteName,
                notes = entry.notes
            )
        }

        val summary = DailySummary(
            date = date,
            entries = dailyEntries
        )

        return pdfGenerator.generateDailyReport(summary, generalNotes)
    }

    suspend fun exportMonthlyReport(month: YearMonth): File? {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()

        val entries = workEntryRepository.getEntriesForPeriod(start, end).first()
        if (entries.isEmpty()) return null

        val employees = employeeRepository.getAllEmployees().first()

        // Raggruppa le ore per dipendente
        val entriesByEmployee = entries.groupBy { it.employeeId }

        val monthlyEntries = entriesByEmployee.map { (empId, empEntries) ->
            val emp = employees.find { it.id == empId }
            val name = emp?.let { "${it.lastName} ${it.firstName}" } ?: "Dipendente Rimosso"
            val totalMins = empEntries.sumOf { it.workedMinutes }

            EmployeeMonthlyEntry(
                employeeName = name,
                totalMinutes = totalMins
            )
        }.sortedBy { it.employeeName } // Ordine alfabetico

        val summary = MonthlySummary(
            month = month,
            employeeSummaries = monthlyEntries
        )

        return pdfGenerator.generateMonthlyReport(summary)
    }
}
