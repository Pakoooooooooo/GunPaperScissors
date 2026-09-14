package com.example.shifumiplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.shifumiplus.presentation.MainViewModel
import com.example.shifumiplus.ui.screens.GameScreen
import com.example.shifumiplus.ui.screens.JoinScreen
import com.example.shifumiplus.ui.screens.LobbyScreen
import com.example.shifumiplus.ui.screens.MainMenuScreen
import com.example.shifumiplus.ui.screens.NameScreen

@Composable
fun AppRoot(
    uiState: MainViewModel.UiState,
    playerWins: Map<String, Int>,
    onNameConfirm: (String) -> Unit,
    onCreateGame: () -> Unit,
    onJoinRequest: (String) -> Unit,
    onStartGame: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onSubmitAction: (String, String?) -> Unit,
    onExitRequested: () -> Unit,
    onLeaveGame: () -> Unit,
    onFinishGame: () -> Unit
) {
    BackHandler(enabled = true) {
        when (uiState.screen) {
            is MainViewModel.Screen.Name -> onExitRequested()
            is MainViewModel.Screen.MainMenu -> onExitRequested()
            is MainViewModel.Screen.Join -> onNavigateToMain()
            is MainViewModel.Screen.Lobby -> onLeaveGame()
            is MainViewModel.Screen.Game -> onNavigateToMain()
            is MainViewModel.Screen.FinalRanking -> onNavigateToMain()
        }
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        when (uiState.screen) {
            is MainViewModel.Screen.Name -> {
                NameScreen(initial = uiState.playerName ?: "", onConfirm = onNameConfirm)
            }
            is MainViewModel.Screen.MainMenu -> {
                MainMenuScreen(onNewGame = onCreateGame, onJoin = onNavigateToJoin)
            }
            is MainViewModel.Screen.Join -> {
                JoinScreen(onJoin = onJoinRequest, onBack = onNavigateToMain)
            }
            is MainViewModel.Screen.Lobby -> {
                LobbyScreen(
                    gameId = uiState.currentGameId ?: "",
                    players = uiState.currentPlayers,
                    playerWins = playerWins,
                    onStart = onStartGame,
                    onBack = onLeaveGame
                )
            }
            is MainViewModel.Screen.Game -> {
                GameScreen(players = uiState.currentPlayers, me = uiState.playerName, choices = uiState.choices, onSubmit = onSubmitAction)
            }
            is MainViewModel.Screen.FinalRanking -> {
                FinalRankingScreen(ranking = uiState.finalRanking, onFinish = onFinishGame)
            }
        }
    }
}
