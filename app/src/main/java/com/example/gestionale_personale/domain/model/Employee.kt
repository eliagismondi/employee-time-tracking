package com.example.gestionale_personale.domain.model

data class Employee(
    val id: Int,
    val name: String,
    val lastName: String,
    val isActive: Boolean
)