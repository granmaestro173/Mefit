package com.mefit.app.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mefit.app.ui.screens.CalendarScreen
import com.mefit.app.ui.screens.DietScreen
import com.mefit.app.ui.screens.ExerciseDetailScreen
import com.mefit.app.ui.screens.LibraryScreen
import com.mefit.app.ui.screens.SettingsScreen
import com.mefit.app.ui.screens.TodayScreen
import java.time.LocalDate

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("today", "Today", Icons.Rounded.Today),
    Tab("calendar", "Plan", Icons.Rounded.CalendarMonth),
    Tab("library", "Exercises", Icons.Rounded.FitnessCenter),
    Tab("diet", "Diet", Icons.Rounded.Restaurant),
)

@Composable
fun MeFitApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    // The date the Today tab is showing; the calendar can jump it to any day.
    var focusDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (TABS.any { it.route == route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("today") {
                TodayScreen(
                    vm = vm,
                    date = LocalDate.parse(focusDate),
                    onDateChange = { focusDate = it.toString() },
                    openExercise = { nav.navigate("exercise/$it") },
                    openSettings = { nav.navigate("settings") },
                )
            }
            composable("calendar") {
                CalendarScreen(
                    vm = vm,
                    openDay = { date ->
                        focusDate = date.toString()
                        nav.switchTab("today")
                    },
                )
            }
            composable("library") {
                LibraryScreen(vm = vm, openExercise = { nav.navigate("exercise/$it") })
            }
            composable("diet") {
                DietScreen(vm = vm, openSettings = { nav.navigate("settings") })
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable("exercise/{id}") { entry ->
                ExerciseDetailScreen(
                    vm = vm,
                    id = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
