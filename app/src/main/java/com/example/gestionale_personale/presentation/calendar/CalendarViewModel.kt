package com.example.gestionale_personale.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.domain.model.WorkEntry
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import com.example.gestionale_personale.domain.repository.WorkEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val entriesForSelectedDate: List<EntryWithEmployee> = emptyList(),
    val daysWithEntries: Set<LocalDate> = emptySet(),
    val totalMinutesForSelectedDate: Int = 0
)

data class EntryWithEmployee(
    val entry: WorkEntry,
    val employee: Employee?
)

class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    // Tutte le entries del mese corrente per evidenziare i giorni sul calendario
    private val monthEntries = _currentMonth.flatMapLatest { month ->
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        workEntryRepository.getEntriesForPeriod(start, end)
    }

    // Le entries del giorno selezionato, con i dettagli del dipendente associato
    private val selectedDateEntries = _selectedDate.flatMapLatest { date ->
        workEntryRepository.getEntriesForDate(date)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        monthEntries,
        selectedDateEntries,
        employeeRepository.getAllEmployees()
    ) { currentMonth, selectedDate, monthEntries, dayEntries, allEmployees ->
        val daysWithEntries = monthEntries.map { it.date }.toSet()

        val entriesWithEmployee = dayEntries.map { entry ->
            EntryWithEmployee(
                entry = entry,
                employee = allEmployees.find { it.id == entry.employeeId }
            )
        }

        val totalMinutes = dayEntries.sumOf { it.workedMinutes }

        CalendarUiState(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            entriesForSelectedDate = entriesWithEmployee,
            daysWithEntries = daysWithEntries,
            totalMinutesForSelectedDate = totalMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun onPreviousMonth() {
        _currentMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        _currentMonth.update { it.plusMonths(1) }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        // Se si seleziona un giorno di un altro mese, aggiorna anche il mese corrente
        if (YearMonth.from(date) != _currentMonth.value) {
            _currentMonth.value = YearMonth.from(date)
        }
    }
}
