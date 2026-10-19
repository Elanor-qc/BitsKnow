package com.gao.xizhi.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gao.xizhi.chat.ChatRoomScreen
import com.gao.xizhi.chat.ChatScreen
import com.gao.xizhi.chat.ChatViewModel
import com.gao.xizhi.chat.ModuleListViewModel
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.review.ReviewScreen
import com.gao.xizhi.settings.SettingsScreen

object Routes {
    const val CHAT = "chat"
    const val CHAT_ROOM = "chat/{moduleId}"
    const val REVIEW = "review"
    const val SETTINGS = "settings"

    fun chatRoom(moduleId: Long) = "chat/$moduleId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.CHAT, "对话", Icons.Filled.Forum),
    Tab(Routes.REVIEW, "复盘", Icons.Filled.GridView),
    Tab(Routes.SETTINGS, "设置", Icons.Filled.Settings),
)

@Composable
fun XizhiApp(db: AppDatabase) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.CHAT) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHAT,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CHAT) {
                ChatScreen(db = db, openModule = { navController.navigate(Routes.chatRoom(it)) }, onOpenSettings = {})
            }
            composable(Routes.CHAT_ROOM) { entry ->
                val moduleId = entry.arguments?.getString("moduleId")?.toLongOrNull() ?: return@composable
                ChatRoomScreen(moduleId = moduleId, db = db, onBack = { navController.popBackStack() })
            }
            composable(Routes.REVIEW) { ReviewScreen(db = db) }
            composable(Routes.SETTINGS) { SettingsScreen(db = db) }
        }
    }
}
