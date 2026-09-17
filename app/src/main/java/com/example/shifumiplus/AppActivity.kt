package com.example.shifumiplus

import android.os.Bundle
import androidx.compose.ui.graphics.toArgb
import com.example.shifumiplus.ui.theme.BackgroundColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.shifumiplus.presentation.MainViewModel
import com.example.shifumiplus.ui.AppRoot
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme
import androidx.core.content.edit

@Suppress("DEPRECATION")
class AppActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setTheme(R.style.Theme_ShiFuMiPlus)

        // Make navigation bar match app background color while keeping navigation controls visible
        window.navigationBarColor = BackgroundColor.toArgb()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("player_name", null)
        if (!savedName.isNullOrBlank()) {
            viewModel.setPlayerName(savedName)
            viewModel.navigateTo(MainViewModel.Screen.MainMenu)
        }

        setContent {
            ShiFuMiPlusTheme {
                val state by viewModel.uiState.collectAsState()
                val playerWins = state.currentPlayers.associate { player ->
                    player.name to prefs.getInt(playerWinKey(player.name), 0)
                }

                AppRoot(
                    uiState = state,
                    playerWins = playerWins,
                    onNameConfirm = { name ->
                        prefs.edit { putString("player_name", name) }
                        viewModel.setPlayerName(name)
                        viewModel.navigateTo(MainViewModel.Screen.MainMenu)
                    },
                    onCreateGame = { viewModel.createGame() },
                    onJoinRequest = { id -> viewModel.joinGame(id) },
                    onStartGame = { viewModel.startGame() },
                    onNavigateToJoin = { viewModel.navigateTo(MainViewModel.Screen.Join) },
                    onNavigateToMain = { viewModel.navigateTo(MainViewModel.Screen.MainMenu) },
                    onSubmitAction = { action, target -> viewModel.submitAction(action, target) },
                    onExitRequested = { finish() },
                    onLeaveGame = { viewModel.leaveGame() },
                    onFinishGame = {
                        val winners = resolveWinners(state.currentPlayers)
                        winners.forEach { name ->
                            val key = playerWinKey(name)
                            val total = prefs.getInt(key, 0) + 1
                            prefs.edit { putInt(key, total) }
                        }
                        viewModel.finishGame()
                    }
                )
            }
        }
    }

    private fun playerWinKey(name: String): String = "player_win_count_${name.trim().lowercase()}"

    private fun resolveWinners(players: List<PlayerState>): List<String> {
        val alive = players.filter { it.lives > 0 }
        if (alive.size == 1) return listOf(alive.first().name)
        if (alive.isNotEmpty()) return alive.map { it.name }

        val maxTurn = players.filter { it.lives <= 0 }.maxOfOrNull { it.eliminatedAtTurn ?: 0L } ?: 0L
        return players.filter { it.lives <= 0 && (it.eliminatedAtTurn ?: 0L) == maxTurn }.map { it.name }
    }
}
