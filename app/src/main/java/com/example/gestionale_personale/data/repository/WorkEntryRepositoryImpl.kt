package com.example.gestionale_personale.data.repository

import com.example.gestionale_personale.data.local.dao.WorkEntryDao
import com.example.gestionale_personale.data.local.entity.WorkEntryEntity
import com.example.gestionale_personale.domain.model.WorkEntry
import com.example.gestionale_personale.domain.repository.WorkEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WorkEntryRepositoryImpl(
    private val workEntryDao: WorkEntryDao
) : WorkEntryRepository {

    override fun getEntriesForDate(date: LocalDate): Flow<List<WorkEntry>> {
        return workEntryDao.getEntriesForDate(date.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEntriesForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkEntry>> {
        return workEntryDao.getEntriesForPeriod(startDate.toString(), endDate.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: String): WorkEntry? {
        return workEntryDao.getEntryById(id)?.toDomain()
    }

    override suspend fun getEntryByEmployeeAndDate(employeeId: String, date: LocalDate): WorkEntry? {
        return workEntryDao.getEntryByEmployeeAndDate(employeeId, date.toString())?.toDomain()
    }

    override suspend fun saveWorkEntry(entry: WorkEntry) {
        workEntryDao.insertWorkEntry(WorkEntryEntity.fromDomain(entry))
    }

    override suspend fun saveWorkEntries(entries: List<WorkEntry>) {
        workEntryDao.insertWorkEntries(entries.map { WorkEntryEntity.fromDomain(it) })
    }

    override suspend fun deleteWorkEntry(entry: WorkEntry) {
        workEntryDao.deleteWorkEntry(WorkEntryEntity.fromDomain(entry))
    }
}
