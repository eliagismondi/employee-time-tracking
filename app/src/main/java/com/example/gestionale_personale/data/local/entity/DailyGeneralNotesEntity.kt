package com.example.gestionale_personale.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gestionale_personale.domain.model.DailyGeneralNotes
import java.time.LocalDate

@Entity(
    tableName = "daily_general_notes",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyGeneralNotesEntity(
    @PrimaryKey val id: String,
    val date: String,
    val notes: String
) {
    fun toDomain() = DailyGeneralNotes(
        id = id,
        date = LocalDate.parse(date),
        notes = notes
    )

    companion object {
        fun fromDomain(model: DailyGeneralNotes) = DailyGeneralNotesEntity(
            id = model.id,
            date = model.date.toString(),
            notes = model.notes
        )
    }
}
