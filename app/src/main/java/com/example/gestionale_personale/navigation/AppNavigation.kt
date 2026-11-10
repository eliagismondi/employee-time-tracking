package com.example.gestionale_personale.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gestionale_personale.presentation.calendar.CalendarScreen
import com.example.gestionale_personale.presentation.employees.EmployeesScreen
import com.example.gestionale_personale.presentation.export.ExportScreen
import com.example.gestionale_personale.presentation.registration.RegistrationScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : BottomNavItem(Screen.Calendar.route, "Calendario", Icons.Default.DateRange)
    object Employees : BottomNavItem(Screen.Employees.route, "Operai", Icons.Default.Person)
    object Registration : BottomNavItem(Screen.Registration.route, "Registra", Icons.Default.Add)
    object Export : BottomNavItem(Screen.Export.route, "Report", Icons.Default.Share)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Calendar,
        BottomNavItem.Employees,
        BottomNavItem.Registration,
        BottomNavItem.Export
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Employees.route) { EmployeesScreen() }
            composable(Screen.Registration.route) { RegistrationScreen() }
            composable(Screen.Export.route) { ExportScreen() }
        }
    }
}
