package com.example.gestionale_personale.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionale_personale.domain.usecase.TimeUtils
import com.example.gestionale_personale.presentation.AppViewModelProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onPreviousMonth() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Mese precedente")
                }

                val monthName = uiState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
                val year = uiState.currentMonth.year
                Text(
                    text = "${monthName.replaceFirstChar { it.uppercase() }} $year",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { viewModel.onNextMonth() }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Mese successivo")
                }
            }

            // Calendar Grid
            CalendarGrid(
                currentMonth = uiState.currentMonth.monthValue,
                currentYear = uiState.currentMonth.year,
                selectedDate = uiState.selectedDate,
                daysWithEntries = uiState.daysWithEntries,
                onDateSelected = { viewModel.onDateSelected(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Day Details Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = "${uiState.selectedDate.dayOfMonth} ${uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)} ${uiState.selectedDate.year}"
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Totale: ${TimeUtils.formatMinutesToHoursAndMinutes(uiState.totalMinutesForSelectedDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Day Entries List
            if (uiState.entriesForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna registrazione per questa data.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.entriesForSelectedDate) { entryWithEmployee ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val empName = entryWithEmployee.employee?.let { "${it.lastName} ${it.firstName}" } ?: "Dipendente Rimosso"
                                    Text(text = empName, style = MaterialTheme.typography.titleMedium)
                                    if (!entryWithEmployee.entry.notes.isNullOrBlank()) {
                                        Text(
                                            text = "Note: ${entryWithEmployee.entry.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    text = TimeUtils.formatMinutesToHoursAndMinutes(entryWithEmployee.entry.workedMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Int,
    currentYear: Int,
    selectedDate: LocalDate,
    daysWithEntries: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val date = LocalDate.of(currentYear, currentMonth, 1)
    val daysInMonth = date.lengthOfMonth()
    // 1 (Monday) to 7 (Sunday)
    val firstDayOfWeek = date.dayOfWeek.value

    // Adjust for Monday as first day of week (Italian standard)
    val emptyCellsBefore = firstDayOfWeek - 1

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar cells
        var currentDay = 1
        var cellIndex = 0

        while (currentDay <= daysInMonth) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    if (cellIndex < emptyCellsBefore || currentDay > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) // Empty cell
                    } else {
                        val thisDate = LocalDate.of(currentYear, currentMonth, currentDay)
                        val isSelected = thisDate == selectedDate
                        val hasEntry = daysWithEntries.contains(thisDate)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (hasEntry) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable { onDateSelected(thisDate) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentDay.toString(),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else if (hasEntry) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected || hasEntry) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        currentDay++
                    }
                    cellIndex++
                }
            }
        }
    }
}
