package com.example.gestionale_personale.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gestionale_personale.data.local.entity.DailyGeneralNotesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGeneralNotesDao {
    @Query("SELECT * FROM daily_general_notes WHERE date = :date")
    fun getNotesForDate(date: String): Flow<DailyGeneralNotesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNotes(notes: DailyGeneralNotesEntity)
}
