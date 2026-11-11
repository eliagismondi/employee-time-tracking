package com.example.gestionale_personale.navigation

sealed class Screen(val route: String) {
    object Calendar : Screen("calendar")
    object Employees : Screen("employees")
    object Registration : Screen("registration")
}
