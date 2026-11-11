package com.example.gestionale_personale.presentation.registration

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class RegistrationRowState(
    val id: String = UUID.randomUUID().toString(),
    val employeeId: String? = null,
    val isTimeRangeMode: Boolean = false,
    val hours: String = "",
    val minutes: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val notes: String = "",
    val error: String? = null
)

data class RegistrationUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val rows: List<RegistrationRowState> = listOf(RegistrationRowState()),
    val activeEmployees: List<Employee> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val globalError: String? = null
)

class RegistrationViewModel(
    private val workEntryRepository: WorkEntryRepository,
    employeeRepository: EmployeeRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _rows = MutableStateFlow(listOf(RegistrationRowState()))
    private val _isSaving = MutableStateFlow(false)
    private val _saveSuccess = MutableStateFlow(false)
    private val _globalError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RegistrationUiState> = combine(
        _selectedDate,
        _rows,
        employeeRepository.getActiveEmployees(),
        _isSaving,
        _saveSuccess,
        _globalError
    ) { args: Array<Any?> ->
        RegistrationUiState(
            selectedDate = args[0] as LocalDate,
            rows = args[1] as List<RegistrationRowState>,
            activeEmployees = args[2] as List<Employee>,
            isSaving = args[3] as Boolean,
            saveSuccess = args[4] as Boolean,
            globalError = args[5] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RegistrationUiState()
    )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        // Reset everything if date changes
        _saveSuccess.value = false
        _globalError.value = null
    }

    fun addRow() {
        _rows.update { current ->
            current + RegistrationRowState()
        }
    }

    fun removeRow(rowId: String) {
        _rows.update { current ->
            if (current.size > 1) {
                current.filter { it.id != rowId }
            } else {
                listOf(RegistrationRowState()) // Keep at least one empty row
            }
        }
    }

    fun toggleTimeMode(rowId: String) {
        _rows.update { current ->
            current.map {
                if (it.id == rowId) {
                    it.copy(isTimeRangeMode = !it.isTimeRangeMode, error = null)
                } else it
            }
        }
    }

    fun updateRowEmployee(rowId: String, employeeId: String) {
        _rows.update { current ->
            current.map { if (it.id == rowId) it.copy(employeeId = employeeId, error = null) else it }
        }
    }

    fun updateRowHours(rowId: String, hours: String) {
        if (hours.isEmpty() || hours.toIntOrNull() != null) {
            _rows.update { current ->
                current.map { if (it.id == rowId) it.copy(hours = hours, error = null) else it }
            }
        }
    }

    fun updateRowMinutes(rowId: String, minutes: String) {
        if (minutes.isEmpty() || (minutes.toIntOrNull() != null && minutes.toInt() < 60)) {
            _rows.update { current ->
                current.map { if (it.id == rowId) it.copy(minutes = minutes, error = null) else it }
            }
        }
    }

    fun updateRowStartTime(rowId: String, time: String) {
        _rows.update { current ->
            current.map { if (it.id == rowId) it.copy(startTime = time, error = null) else it }
        }
    }

    fun updateRowEndTime(rowId: String, time: String) {
        _rows.update { current ->
            current.map { if (it.id == rowId) it.copy(endTime = time, error = null) else it }
        }
    }

    fun updateRowNotes(rowId: String, notes: String) {
        _rows.update { current ->
            current.map { if (it.id == rowId) it.copy(notes = notes) else it }
        }
    }

    fun resetSuccessState() {
        _saveSuccess.value = false
        _rows.value = listOf(RegistrationRowState())
    }

    fun saveEntries() {
        viewModelScope.launch {
            _isSaving.value = true
            _globalError.value = null

            val currentRows = _rows.value
            val date = _selectedDate.value

            // Validation 1: Check for completely empty forms
            val validRows = currentRows.filter { row ->
                row.employeeId != null && (
                    (!row.isTimeRangeMode && (row.hours.isNotBlank() || row.minutes.isNotBlank())) ||
                    (row.isTimeRangeMode && row.startTime.isNotBlank() && row.endTime.isNotBlank())
                )
            }

            if (validRows.isEmpty()) {
                _globalError.value = "Compila almeno una riga con dipendente e ore (o orario)."
                _isSaving.value = false
                return@launch
            }

            // Validation rimosse per permettere allo stesso dipendente di avere più turni nella stessa data
            val entriesToSave = mutableListOf<WorkEntry>()

            val updatedRows = currentRows.map { row ->
                val isValidDirect = !row.isTimeRangeMode && (row.hours.isNotBlank() || row.minutes.isNotBlank())
                val isValidTime = row.isTimeRangeMode && row.startTime.isNotBlank() && row.endTime.isNotBlank()

                if (row.employeeId != null && (isValidDirect || isValidTime)) {
                    var totalMinutes = 0
                    var finalStartTime: String? = null
                    var finalEndTime: String? = null

                    if (row.isTimeRangeMode) {
                        finalStartTime = row.startTime
                        finalEndTime = row.endTime

                        val startParts = row.startTime.split(":")
                        val endParts = row.endTime.split(":")

                        if (startParts.size == 2 && endParts.size == 2) {
                            val startMins = (startParts[0].toIntOrNull() ?: 0) * 60 + (startParts[1].toIntOrNull() ?: 0)
                            val endMins = (endParts[0].toIntOrNull() ?: 0) * 60 + (endParts[1].toIntOrNull() ?: 0)

                            totalMinutes = if (endMins >= startMins) {
                                endMins - startMins
                            } else {
                                // Crossed midnight
                                (24 * 60 - startMins) + endMins
                            }
                        }
                    } else {
                        val h = row.hours.toIntOrNull() ?: 0
                        val m = row.minutes.toIntOrNull() ?: 0
                        totalMinutes = (h * 60) + m
                    }

                    entriesToSave.add(
                        WorkEntry(
                            id = UUID.randomUUID().toString(),
                            employeeId = row.employeeId,
                            date = date,
                            workedMinutes = totalMinutes,
                            notes = row.notes.takeIf { it.isNotBlank() },
                            startTime = finalStartTime,
                            endTime = finalEndTime
                        )
                    )
                    row.copy(error = null)
                } else {
                    row
                }
            }

            _rows.value = updatedRows

            // Save to DB
            if (entriesToSave.isNotEmpty()) {
                workEntryRepository.saveWorkEntries(entriesToSave)
                _saveSuccess.value = true
            }

            _isSaving.value = false
        }
    }
}
