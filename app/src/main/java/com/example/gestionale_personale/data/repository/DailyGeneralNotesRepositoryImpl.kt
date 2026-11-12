package com.example.gestionale_personale.data.repository

import com.example.gestionale_personale.data.local.dao.DailyGeneralNotesDao
import com.example.gestionale_personale.data.local.entity.DailyGeneralNotesEntity
import com.example.gestionale_personale.domain.model.DailyGeneralNotes
import com.example.gestionale_personale.domain.repository.DailyGeneralNotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DailyGeneralNotesRepositoryImpl(
    private val dao: DailyGeneralNotesDao
) : DailyGeneralNotesRepository {
    override fun getNotesForDate(date: LocalDate): Flow<DailyGeneralNotes?> {
        return dao.getNotesForDate(date.toString()).map { it?.toDomain() }
    }

    override suspend fun saveNotes(notes: DailyGeneralNotes) {
        dao.insertOrUpdateNotes(DailyGeneralNotesEntity.fromDomain(notes))
    }
}
