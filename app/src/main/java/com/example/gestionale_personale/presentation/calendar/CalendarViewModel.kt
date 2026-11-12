package com.example.gestionale_personale.presentation.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionale_personale.domain.model.DailyGeneralNotes
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.domain.model.WorkEntry
import com.example.gestionale_personale.domain.repository.DailyGeneralNotesRepository
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import com.example.gestionale_personale.domain.repository.WorkEntryRepository
import com.example.gestionale_personale.domain.usecase.PdfExportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val entriesForSelectedDate: List<EntryWithEmployee> = emptyList(),
    val generalNotesForSelectedDate: String = "",
    val daysWithEntries: Set<LocalDate> = emptySet(),
    val isExporting: Boolean = false,
    val exportedFile: File? = null
)

data class EntryWithEmployee(
    val entry: WorkEntry,
    val employee: Employee?
)

class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val employeeRepository: EmployeeRepository,
    private val dailyGeneralNotesRepository: DailyGeneralNotesRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isExporting = MutableStateFlow(false)
    private val _exportedFile = MutableStateFlow<File?>(null)

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

    // Le note generali del giorno selezionato
    private val selectedDateGeneralNotes = _selectedDate.flatMapLatest { date ->
        dailyGeneralNotesRepository.getNotesForDate(date)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        monthEntries,
        selectedDateEntries,
        employeeRepository.getAllEmployees(),
        selectedDateGeneralNotes,
        _isExporting,
        _exportedFile
    ) { args: Array<Any?> ->
        val currentMonth = args[0] as YearMonth
        val selectedDate = args[1] as LocalDate
        val monthEntries = args[2] as List<WorkEntry>
        val dayEntries = args[3] as List<WorkEntry>
        val allEmployees = args[4] as List<Employee>
        val generalNotes = args[5] as DailyGeneralNotes?
        val isExporting = args[6] as Boolean
        val exportedFile = args[7] as File?

        val daysWithEntries = monthEntries.map { it.date }.toSet()

        val entriesWithEmployee = dayEntries.map { entry ->
            EntryWithEmployee(
                entry = entry,
                employee = allEmployees.find { it.id == entry.employeeId }
            )
        }

        CalendarUiState(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            entriesForSelectedDate = entriesWithEmployee,
            generalNotesForSelectedDate = generalNotes?.notes ?: "",
            daysWithEntries = daysWithEntries,
            isExporting = isExporting,
            exportedFile = exportedFile
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

    fun exportDailyPdf(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            val useCase = PdfExportUseCase(workEntryRepository, employeeRepository, dailyGeneralNotesRepository, context)
            val file = useCase.exportDailyReport(_selectedDate.value)
            _exportedFile.value = file
            _isExporting.value = false
        }
    }

    fun onPdfShared() {
        _exportedFile.value = null
    }

    fun deleteWorkEntry(entry: WorkEntry) {
        viewModelScope.launch {
            workEntryRepository.deleteWorkEntry(entry)
        }
    }
}
