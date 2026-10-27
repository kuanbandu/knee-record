package com.kuanbandu.kneerecord.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kuanbandu.kneerecord.ui.screens.AssessScreen
import com.kuanbandu.kneerecord.ui.screens.EntryScreen
import com.kuanbandu.kneerecord.ui.screens.PlanScreen
import com.kuanbandu.kneerecord.ui.screens.RecordsScreen
import com.kuanbandu.kneerecord.ui.screens.SettingsScreen
import com.kuanbandu.kneerecord.ui.screens.WorkoutScreen

private data class TabItem(val route: String, val label: String)

private val TABS = listOf(
    TabItem("plan", "训练"),
    TabItem("records", "记录"),
    TabItem("assess", "我的"),
    TabItem("settings", "设置"),
)

@Composable
fun KneeRecordApp() {
    val navController = rememberNavController()
    val tabRoutes = TABS.map { it.route }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val destination = backStackEntry?.destination
            val showBar = destination?.hierarchy?.any { it.route in tabRoutes } ?: false
            if (showBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {},
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = "plan") {
                composable("plan") {
                    PlanScreen(
                        onStart = { navController.navigate("workout") },
                        onGoAssess = { navController.navigate("assess") },
                    )
                }
                composable("workout") {
                    WorkoutScreen(onFinish = { navController.popBackStack() })
                }
                composable("records") {
                    RecordsScreen(onAdd = { navController.navigate("entry") })
                }
                composable("entry") {
                    EntryScreen(onSaved = { navController.popBackStack() })
                }
                composable("assess") { AssessScreen() }
                composable("settings") { SettingsScreen() }
            }
        }
    }
}
