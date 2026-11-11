package com.example.gestionale_personale.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestionale_personale.data.local.entity.WorkEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {
    @Query("SELECT * FROM work_entries WHERE date = :date ORDER BY employee_id ASC")
    fun getEntriesForDate(date: String): Flow<List<WorkEntryEntity>>

    @Query("SELECT * FROM work_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getEntriesForPeriod(startDate: String, endDate: String): Flow<List<WorkEntryEntity>>

    @Query("SELECT * FROM work_entries WHERE id = :id")
    suspend fun getEntryById(id: String): WorkEntryEntity?

    @Query("SELECT * FROM work_entries WHERE employee_id = :employeeId AND date = :date")
    suspend fun getEntryByEmployeeAndDate(employeeId: String, date: String): WorkEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkEntry(entry: WorkEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkEntries(entries: List<WorkEntryEntity>): List<Long>

    @Update
    suspend fun updateWorkEntry(entry: WorkEntryEntity): Int

    @Delete
    suspend fun deleteWorkEntry(entry: WorkEntryEntity)
}
