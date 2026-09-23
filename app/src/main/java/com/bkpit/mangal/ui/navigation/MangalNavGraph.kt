package com.bkpit.mangal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bkpit.mangal.ui.chat.ChatScreen
import com.bkpit.mangal.ui.modelmanager.ModelManagerScreen
import com.bkpit.mangal.ui.settings.SettingsScreen

object MangalDestinations {
    const val CHAT = "chat"
    const val MODEL_MANAGER = "model_manager"
    const val SETTINGS = "settings"
}

@Composable
fun MangalNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MangalDestinations.CHAT) {
        composable(MangalDestinations.CHAT) {
            ChatScreen(
                onOpenModelManager = { navController.navigate(MangalDestinations.MODEL_MANAGER) },
                onOpenSettings = { navController.navigate(MangalDestinations.SETTINGS) }
            )
        }
        composable(MangalDestinations.MODEL_MANAGER) {
            ModelManagerScreen(onBack = { navController.popBackStack() })
        }
        composable(MangalDestinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
