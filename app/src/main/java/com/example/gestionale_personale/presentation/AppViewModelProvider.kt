package com.example.gestionale_personale.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gestionale_personale.CiottisWorkersApp
import com.example.gestionale_personale.presentation.calendar.CalendarViewModel
import com.example.gestionale_personale.presentation.employees.EmployeesViewModel
import com.example.gestionale_personale.presentation.registration.RegistrationViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            EmployeesViewModel(ciottisWorkersApp().container.employeeRepository)
        }
        initializer {
            CalendarViewModel(
                ciottisWorkersApp().container.workEntryRepository,
                ciottisWorkersApp().container.employeeRepository,
                ciottisWorkersApp().container.dailyGeneralNotesRepository
            )
        }
        initializer {
            RegistrationViewModel(
                ciottisWorkersApp().container.workEntryRepository,
                ciottisWorkersApp().container.employeeRepository,
                ciottisWorkersApp().container.dailyGeneralNotesRepository
            )
        }
    }
}

fun CreationExtras.ciottisWorkersApp(): CiottisWorkersApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CiottisWorkersApp)
