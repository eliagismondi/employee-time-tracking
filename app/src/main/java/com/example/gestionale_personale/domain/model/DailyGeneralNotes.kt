package com.example.gestionale_personale.domain.model

import java.time.LocalDate

data class DailyGeneralNotes(
    val id: String,
    val date: LocalDate,
    val notes: String
)
