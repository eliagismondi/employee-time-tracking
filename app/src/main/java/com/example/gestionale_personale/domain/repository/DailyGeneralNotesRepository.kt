package com.example.gestionale_personale.domain.repository

import com.example.gestionale_personale.domain.model.DailyGeneralNotes
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DailyGeneralNotesRepository {
    fun getNotesForDate(date: LocalDate): Flow<DailyGeneralNotes?>
    suspend fun saveNotes(notes: DailyGeneralNotes)
}
