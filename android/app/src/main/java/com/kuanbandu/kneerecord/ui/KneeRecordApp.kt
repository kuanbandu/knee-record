package com.kuanbandu.kneerecord.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kuanbandu.kneerecord.ui.screens.EntryScreen
import com.kuanbandu.kneerecord.ui.screens.ProfileScreen
import com.kuanbandu.kneerecord.ui.screens.RecordsScreen
import com.kuanbandu.kneerecord.ui.screens.SettingsScreen

private const val ROUTE_RECORDS = "records"
private const val ROUTE_ENTRY = "entry"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun KneeRecordApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // 录入页不需要底部导航，避免误触丢失已填内容
            if (currentRoute != ROUTE_ENTRY) {
                BottomTabs(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_RECORDS,
            modifier = Modifier.padding(padding),
        ) {
            composable(ROUTE_RECORDS) {
                RecordsScreen(onAdd = { navController.navigate(ROUTE_ENTRY) })
            }
            composable(ROUTE_ENTRY) {
                EntryScreen(onSaved = { navController.popBackStack() })
            }
            composable(ROUTE_PROFILE) { ProfileScreen() }
            composable(ROUTE_SETTINGS) { SettingsScreen() }
        }
    }
}

@Composable
private fun BottomTabs(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == ROUTE_RECORDS,
            onClick = { navController.openTab(ROUTE_RECORDS) },
            icon = {},
            label = { Text("我的记录", fontSize = 12.sp) },
        )
        NavigationBarItem(
            selected = currentRoute == ROUTE_PROFILE,
            onClick = { navController.openTab(ROUTE_PROFILE) },
            icon = {},
            label = { Text("我的情况", fontSize = 12.sp) },
        )
        NavigationBarItem(
            selected = currentRoute == ROUTE_SETTINGS,
            onClick = { navController.openTab(ROUTE_SETTINGS) },
            icon = {},
            label = { Text("设置", fontSize = 12.sp) },
        )
    }
}

private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
