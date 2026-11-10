package com.example.gestionale_personale.domain.model

data class Employee(
    val id: String,
    val firstName: String,
    val lastName: String,
    val active: Boolean,
    val createdAt: Long
)
