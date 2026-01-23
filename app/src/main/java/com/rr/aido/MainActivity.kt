

package com.rr.aido

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AiProvider
import com.rr.aido.ui.screens.AIChatScreen
import com.rr.aido.ui.screens.DemoScreen
import com.rr.aido.ui.screens.HomeScreen
import com.rr.aido.ui.screens.MarketplaceScreen
import com.rr.aido.ui.screens.OnboardingScreen
import com.rr.aido.ui.screens.PlaygroundScreen
import com.rr.aido.ui.screens.PrepromptsScreen
import com.rr.aido.ui.screens.SettingsScreen
import com.rr.aido.ui.screens.KeyboardSettingsScreen
import com.rr.aido.ui.theme.AidoTheme
import com.rr.aido.ui.viewmodels.AIChatViewModel
import com.rr.aido.ui.viewmodels.MarketplaceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AidoTheme {
                AidoApp(
                    navigateTo = intent.getStringExtra("navigate_to")
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun AidoApp(navigateTo: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val dataStoreManager = remember { DataStoreManager(context) }

    var isFirstLaunch by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    // Check if first launch
    LaunchedEffect(Unit) {
        isFirstLaunch = dataStoreManager.isFirstLaunchFlow.first()
        isLoading = false
    }

    // Handle deep navigation
    LaunchedEffect(navigateTo) {
        if (!isLoading && navigateTo != null) {
try {
                navController.navigate(navigateTo) {
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error navigating to $navigateTo", e)
            }
        }
    }

    if (isLoading) {
        // Show loading or splash screen
        return
    }

    val startDestination = if (isFirstLaunch) "onboarding" else "home"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // Onboarding Screen
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    // Mark onboarding as complete
                    (context as? MainActivity)?.lifecycleScope?.launch {
                        dataStoreManager.markFirstLaunchComplete()
                    }
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSaveSettings = { provider, apiKey, model, customApiUrl, customApiKey, customModelName, triggerMethod ->
                    (context as? MainActivity)?.lifecycleScope?.launch {
                        dataStoreManager.saveProvider(provider)
                        dataStoreManager.saveSelectedModel(model)
                        when (provider) {
                            AiProvider.GEMINI -> {
                                dataStoreManager.saveApiKey(apiKey)
                            }
                            AiProvider.CUSTOM -> {
                                dataStoreManager.saveCustomApiUrl(customApiUrl)
                                dataStoreManager.saveCustomApiKey(customApiKey)
                                dataStoreManager.saveCustomModelName(customModelName)
                            }
                            else -> {
                                dataStoreManager.saveApiKey("")
                            }
                        }
                        // Save trigger method
                        dataStoreManager.saveTriggerMethod(triggerMethod)
                    }
                }
            )
        }

        // Home Screen
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToPlayground = { navController.navigate("playground") },
                onNavigateToChat = { navController.navigate("ai_chat") },
                onNavigateToPreprompts = { navController.navigate("preprompts") },
                onNavigateToSpecialCommands = { navController.navigate("special_commands") },
                onNavigateToTextShortcuts = { navController.navigate("text_shortcuts") },
                onNavigateToFeaturesSettings = { navController.navigate("settings?section=Features") }
            )
        }

        // Settings Screen
        composable(
            "settings?section={section}",
            arguments = listOf(androidx.navigation.navArgument("section") {
                type = androidx.navigation.NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToKeyboardSettings = { navController.navigate("keyboard_settings") },
                onNavigateToSpecialCommands = { navController.navigate("special_commands") },
                onNavigateToManageApps = { navController.navigate("manage_apps") },
                onNavigateToTextShortcuts = { navController.navigate("text_shortcuts") },
                targetSection = backStackEntry.arguments?.getString("section")
            )
        }

        // Keyboard Settings Screen
        composable("keyboard_settings") {
            KeyboardSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Special Commands Screen
        composable("special_commands") {
            com.rr.aido.ui.screens.SpecialCommandsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Preprompts Screen
        composable("preprompts") {
            PrepromptsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Demo Screen
        composable("demo") {
            DemoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Marketplace Screen
        composable("marketplace") {
            val marketplaceViewModel = remember {
                MarketplaceViewModel(
                    dataStoreManager = dataStoreManager
                )
            }
            MarketplaceScreen(
                viewModel = marketplaceViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Playground Screen
        composable("playground") {
            PlaygroundScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // AI Chat Screen
        composable("ai_chat") {
            val aiChatViewModel = remember {
                AIChatViewModel(
                    dataStoreManager = dataStoreManager,
                    geminiRepository = com.rr.aido.data.repository.GeminiRepositoryImpl()
                )
            }
            AIChatScreen(
                viewModel = aiChatViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Manage Apps Screen
        composable("manage_apps") {
            val appRepository = remember { com.rr.aido.data.repository.AppRepository(context) }
            val manageAppsViewModel = remember {
                com.rr.aido.ui.viewmodels.ManageAppsViewModel(
                    appRepository = appRepository,
                    dataStoreManager = dataStoreManager
                )
            }
            com.rr.aido.ui.screens.ManageAppsScreen(
                viewModel = manageAppsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Text Shortcuts Screen
        composable("text_shortcuts") {
            val textShortcutsViewModel = remember {
                com.rr.aido.ui.viewmodels.TextShortcutsViewModel(
                    dataStoreManager = dataStoreManager
                )
            }
            com.rr.aido.ui.screens.TextShortcutsScreen(
                viewModel = textShortcutsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}