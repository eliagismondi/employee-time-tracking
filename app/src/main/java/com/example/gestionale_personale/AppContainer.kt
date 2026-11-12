package com.example.gestionale_personale

import android.content.Context
import com.example.gestionale_personale.data.local.database.AppDatabase
import com.example.gestionale_personale.data.repository.DailyGeneralNotesRepositoryImpl
import com.example.gestionale_personale.data.repository.EmployeeRepositoryImpl
import com.example.gestionale_personale.data.repository.WorkEntryRepositoryImpl
import com.example.gestionale_personale.domain.repository.DailyGeneralNotesRepository
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import com.example.gestionale_personale.domain.repository.WorkEntryRepository

interface AppContainer {
    val employeeRepository: EmployeeRepository
    val workEntryRepository: WorkEntryRepository
    val dailyGeneralNotesRepository: DailyGeneralNotesRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    private val database = AppDatabase.getDatabase(context)

    override val employeeRepository: EmployeeRepository by lazy {
        EmployeeRepositoryImpl(database.employeeDao())
    }

    override val workEntryRepository: WorkEntryRepository by lazy {
        WorkEntryRepositoryImpl(database.workEntryDao())
    }

    override val dailyGeneralNotesRepository: DailyGeneralNotesRepository by lazy {
        DailyGeneralNotesRepositoryImpl(database.dailyGeneralNotesDao())
    }
}
