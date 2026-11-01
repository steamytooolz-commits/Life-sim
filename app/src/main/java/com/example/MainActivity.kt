package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NavigationDrawerContent
import com.example.ui.components.NewGameDialog
import com.example.ui.components.TopBar
import com.example.ui.screens.ActionScreen
import com.example.ui.screens.ChronicleScreen
import com.example.ui.screens.ModelDownloadScreen
import com.example.ui.screens.SubCategoryScreen
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.SovereignTheme
import com.example.ui.viewmodel.DrawerDestination
import com.example.ui.viewmodel.SovereignViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SovereignViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SovereignTheme {
                val isModelReady by viewModel.isModelReady.collectAsStateWithLifecycle()
                val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
                val progressPercent by viewModel.downloadProgressPercent.collectAsStateWithLifecycle()
                val statusMessage by viewModel.downloadStatusMessage.collectAsStateWithLifecycle()

                if (!isModelReady) {
                    ModelDownloadScreen(
                        isDownloading = isDownloading,
                        progressPercent = progressPercent,
                        statusMessage = statusMessage,
                        onStartDownload = { wifiOnly ->
                            viewModel.startModelDownload(wifiOnly)
                        },
                        onSkipToOfflineSimulation = {
                            viewModel.skipToOfflineSimulation()
                        }
                    )
                } else {
                    SovereignApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SovereignApp(viewModel: SovereignViewModel) {
    val worldState by viewModel.worldState.collectAsStateWithLifecycle()
    val chronicleEntries by viewModel.chronicleEntries.collectAsStateWithLifecycle()
    val turnState by viewModel.turnState.collectAsStateWithLifecycle()
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedSubCategory by viewModel.selectedSubCategory.collectAsStateWithLifecycle()
    val actionText by viewModel.actionText.collectAsStateWithLifecycle()
    val isNewGameModalOpen by viewModel.isNewGameModalOpen.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CharcoalBackground
            ) {
                NavigationDrawerContent(
                    worldState = worldState,
                    currentDestination = currentDestination,
                    engineStatus = viewModel.engineStatus,
                    onSelectDestination = { dest ->
                        viewModel.setDrawerDestination(dest)
                        scope.launch { drawerState.close() }
                    },
                    onStartNewCareer = {
                        viewModel.openNewGameModal()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    worldState = worldState,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onAdvanceWeek = {
                        viewModel.advanceWeek()
                    },
                    isGenerating = turnState is com.example.engine.orchestrator.TurnState.Generating ||
                            turnState is com.example.engine.orchestrator.TurnState.ExecutingTools
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CharcoalBackground)
            ) {
                when (currentDestination) {
                    DrawerDestination.ACTIONS -> {
                        ActionScreen(
                            worldState = worldState,
                            turnState = turnState,
                            actionText = actionText,
                            quickActions = viewModel.getQuickActions(worldState),
                            onActionTextChanged = viewModel::onActionTextChanged,
                            onRunTurn = { viewModel.runTurn() },
                            onSelectQuickAction = { chip ->
                                viewModel.runTurn(chip)
                            }
                        )
                    }

                    DrawerDestination.CHRONICLE -> {
                        ChronicleScreen(entries = chronicleEntries)
                    }

                    DrawerDestination.PERSONAL,
                    DrawerDestination.POLITICS,
                    DrawerDestination.NATION -> {
                        SubCategoryScreen(
                            destination = currentDestination,
                            worldState = worldState,
                            subCategories = viewModel.getSubCategories(currentDestination, worldState),
                            selectedSubCategory = selectedSubCategory,
                            onSelectSubCategory = viewModel::selectSubCategory,
                            onExecuteActionFromDossier = { action ->
                                viewModel.setActionTextAndNavigate(action)
                            }
                        )
                    }
                }
            }
        }
    }

    if (isNewGameModalOpen) {
        NewGameDialog(
            onDismiss = { viewModel.closeNewGameModal() },
            onStartGame = { origin, nation ->
                viewModel.startNewGame(origin, nation)
            }
        )
    }
}
