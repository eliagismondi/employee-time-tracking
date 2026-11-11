package com.example.gestionale_personale.domain.repository

import com.example.gestionale_personale.domain.model.WorkEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WorkEntryRepository {
    fun getEntriesForDate(date: LocalDate): Flow<List<WorkEntry>>
    fun getEntriesForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkEntry>>
    suspend fun getEntryById(id: String): WorkEntry?
    suspend fun getEntryByEmployeeAndDate(employeeId: String, date: LocalDate): WorkEntry?
    suspend fun saveWorkEntry(entry: WorkEntry)
    suspend fun saveWorkEntries(entries: List<WorkEntry>)
    suspend fun deleteWorkEntry(entry: WorkEntry)
}
