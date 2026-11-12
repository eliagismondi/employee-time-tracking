package com.example.gestionale_personale.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionale_personale.domain.model.DailyGeneralNotes
import com.example.gestionale_personale.domain.model.Employee
import com.example.gestionale_personale.domain.model.WorkEntry
import com.example.gestionale_personale.domain.repository.DailyGeneralNotesRepository
import com.example.gestionale_personale.domain.repository.EmployeeRepository
import com.example.gestionale_personale.domain.repository.WorkEntryRepository
import com.example.gestionale_personale.domain.usecase.AiProcessingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.UUID

sealed class VoiceUiState {
    object Idle : VoiceUiState()
    object Processing : VoiceUiState()
    data class Success(val savedCount: Int) : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}

class VoiceRegistrationViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val employeeRepository: EmployeeRepository,
    private val dailyGeneralNotesRepository: DailyGeneralNotesRepository
) : ViewModel() {

    private val aiUseCase = AiProcessingUseCase()

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoiceUiState.Idle
    )

    fun processAudio(audioFile: File, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _uiState.value = VoiceUiState.Processing

            try {
                val activeEmployees = employeeRepository.getActiveEmployees().first()
                val (generalNotes, extractedShifts) = aiUseCase.processAudioAndExtractShifts(audioFile, activeEmployees)

                if (extractedShifts.isEmpty() && generalNotes.isNullOrBlank()) {
                    _uiState.value = VoiceUiState.Error("Non ho capito nessun turno né note generali in questo audio.")
                    return@launch
                }

                // Salva le note generali se ci sono
                if (!generalNotes.isNullOrBlank()) {
                    dailyGeneralNotesRepository.saveNotes(
                        DailyGeneralNotes(
                            id = UUID.randomUUID().toString(),
                            date = date,
                            notes = generalNotes
                        )
                    )
                }

                val entriesToSave = mutableListOf<WorkEntry>()

                for (shift in extractedShifts) {
                    var totalMinutes = 0

                    if (shift.startTime != null && shift.endTime != null) {
                        val startParts = shift.startTime.split(":")
                        val endParts = shift.endTime.split(":")

                        if (startParts.size == 2 && endParts.size == 2) {
                            val startMins = (startParts[0].toIntOrNull() ?: 0) * 60 + (startParts[1].toIntOrNull() ?: 0)
                            val endMins = (endParts[0].toIntOrNull() ?: 0) * 60 + (endParts[1].toIntOrNull() ?: 0)

                            totalMinutes = if (endMins >= startMins) {
                                endMins - startMins
                            } else {
                                (24 * 60 - startMins) + endMins
                            }
                        }
                    }

                    entriesToSave.add(
                        WorkEntry(
                            id = UUID.randomUUID().toString(),
                            employeeId = shift.employeeId,
                            date = date,
                            workedMinutes = totalMinutes,
                            notes = shift.notes,
                            startTime = shift.startTime,
                            endTime = shift.endTime,
                            worksiteName = shift.worksite
                        )
                    )
                }

                if (entriesToSave.isNotEmpty()) {
                    workEntryRepository.saveWorkEntries(entriesToSave)
                }

                _uiState.value = VoiceUiState.Success(entriesToSave.size)

                // Pulizia file cache
                try { audioFile.delete() } catch (e: Exception) { }

            } catch (e: Exception) {
                _uiState.value = VoiceUiState.Error("Errore imprevisto: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = VoiceUiState.Idle
    }
}
