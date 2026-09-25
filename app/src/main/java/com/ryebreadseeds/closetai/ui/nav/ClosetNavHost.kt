package com.ryebreadseeds.closetai.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ryebreadseeds.closetai.ui.ClosetViewModel
import com.ryebreadseeds.closetai.ui.closet.ClosetScreen
import com.ryebreadseeds.closetai.ui.outfits.OutfitsScreen
import com.ryebreadseeds.closetai.ui.settings.SettingsScreen
import com.ryebreadseeds.closetai.ui.theme.ClosetBackground
import com.ryebreadseeds.closetai.ui.theme.ClosetColors
import com.ryebreadseeds.closetai.ui.today.TodayScreen

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Today : Dest("today", "Today", Icons.Outlined.Today)
    data object Closet : Dest("closet", "Closet", Icons.Outlined.Checkroom)
    data object Outfits : Dest("outfits", "Outfits", Icons.Outlined.Style)
    data object Settings : Dest("settings", "Settings", Icons.Outlined.Settings)
}

private val tabs = listOf(Dest.Today, Dest.Closet, Dest.Outfits, Dest.Settings)

@Composable
fun ClosetNavHost(viewModel: ClosetViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    ClosetBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = ClosetColors.InkMid.copy(alpha = 0.96f)) {
                    tabs.forEach { dest ->
                        val selected = current?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(dest.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ClosetColors.Rose,
                                selectedTextColor = ClosetColors.Rose,
                                indicatorColor = ClosetColors.Plum,
                                unselectedIconColor = ClosetColors.TextSecondary,
                                unselectedTextColor = ClosetColors.TextSecondary
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Dest.Today.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Dest.Today.route) { TodayScreen(viewModel) }
                composable(Dest.Closet.route) { ClosetScreen(viewModel) }
                composable(Dest.Outfits.route) { OutfitsScreen(viewModel) }
                composable(Dest.Settings.route) { SettingsScreen(viewModel) }
            }
        }
    }
}
