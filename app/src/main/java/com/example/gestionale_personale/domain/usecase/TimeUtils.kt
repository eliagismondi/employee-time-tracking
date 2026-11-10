package com.example.gestionale_personale.domain.usecase

object TimeUtils {
    fun formatMinutesToHoursAndMinutes(totalMinutes: Int): String {
        if (totalMinutes == 0) return "0 h"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return buildString {
            if (hours > 0) append("$hours h")
            if (hours > 0 && minutes > 0) append(" ")
            if (minutes > 0) append("$minutes min")
        }
    }
}
