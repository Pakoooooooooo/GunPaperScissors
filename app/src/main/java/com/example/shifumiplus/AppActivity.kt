package com.example.shifumiplus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import com.example.shifumiplus.ui.theme.BackgroundColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import com.example.shifumiplus.data.FirestoreRepository
import com.example.shifumiplus.presentation.MainViewModel
import com.example.shifumiplus.ui.AppRoot
import com.example.shifumiplus.ui.theme.ShiFuMiPlusTheme
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class AppActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_GAME_ID = "open_game_id"
    }

    private val viewModel: MainViewModel by viewModels()
    private var isGameScreenVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setTheme(R.style.Theme_ShiFuMiPlus)

        // Make navigation bar match app background color while keeping navigation controls visible
        window.navigationBarColor = BackgroundColor.toArgb()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("player_name", null)
        var savedId = prefs.getString("player_id", null)
        if (!savedName.isNullOrBlank()) {
            if (savedId.isNullOrBlank()) {
                savedId = java.util.UUID.randomUUID().toString().replace("-", "").uppercase().take(8)
                prefs.edit { putString("player_id", savedId) }
            }
            viewModel.setPlayer(savedName, savedId)
            viewModel.navigateTo(MainViewModel.Screen.MainMenu)
        }
        openGameFromIntent(intent)

        setContent {
            ShiFuMiPlusTheme {
                val state by viewModel.uiState.collectAsState()
                SideEffect {
                    isGameScreenVisible = state.screen is MainViewModel.Screen.Game
                    setGameNavigationBarHidden(isGameScreenVisible)
                }
                val playerWins = state.currentPlayers.associate { player ->
                    player.id to prefs.getInt(playerWinKey(player.id), 0)
                }

                AppRoot(
                    uiState = state,
                    playerWins = playerWins,
                    onNameConfirm = { name ->
                        val id = prefs.getString("player_id", null) ?: java.util.UUID.randomUUID().toString().replace("-", "").uppercase().take(8)
                        prefs.edit { putString("player_name", name); putString("player_id", id) }
                        viewModel.setPlayer(name, id)
                        viewModel.navigateTo(MainViewModel.Screen.MainMenu)
                    },
                    onCreateGame = { viewModel.createGame() },
                    onJoinRequest = { id -> viewModel.joinGame(id) },
                    onJoinInviteRequest = { gameId, inviteId ->
                        val playerId = prefs.getString("player_id", null).orEmpty()
                        viewModel.joinGame(gameId) { joined ->
                            if (!joined) {
                                lifecycleScope.launch {
                                    try {
                                        when (FirestoreRepository.gameExists(gameId)) {
                                            false -> {
                                                FriendsStore.deleteGameInvite(playerId, inviteId)
                                                Toast.makeText(
                                                    this@AppActivity,
                                                    "Cette partie n'existe plus. Invitation supprimée.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            null -> Toast.makeText(
                                                this@AppActivity,
                                                "Impossible de vérifier cette invitation.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            true -> Unit
                                        }
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            this@AppActivity,
                                            "Impossible de vérifier cette invitation.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    onStartGame = { viewModel.startGame() },
                    onNavigateToJoin = { viewModel.navigateTo(MainViewModel.Screen.Join) },
                    onNavigateToFriends = {
                        val intent = Intent(this@AppActivity, FriendsActivity::class.java)
                        intent.putExtra("player_id", prefs.getString("player_id", null))
                        intent.putExtra("player_name", prefs.getString("player_name", null))
                        startActivity(intent)
                    },
                    onNavigateToMain = { viewModel.navigateTo(MainViewModel.Screen.MainMenu) },
                    onSubmitAction = { action, target -> viewModel.submitAction(action, target) },
                    onExitRequested = { finish() },
                    onLeaveGame = { viewModel.leaveGame() },
                    onFinishGame = {
                        val winners = resolveWinners(state.currentPlayers)
                        winners.forEach { id ->
                            val key = playerWinKey(id)
                            val total = prefs.getInt(key, 0) + 1
                            prefs.edit { putInt(key, total) }
                        }
                        viewModel.finishGame()
                    },
                    onReplayGame = { viewModel.replayGame() },
                    onAnimationsCompleted = { viewModel.notifyAnimationsCompleted() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openGameFromIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setGameNavigationBarHidden(isGameScreenVisible)
        }
    }

    private fun setGameNavigationBarHidden(hidden: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun openGameFromIntent(intent: Intent?) {
        val gameId = intent?.getStringExtra(EXTRA_OPEN_GAME_ID)
        if (!gameId.isNullOrBlank()) {
            viewModel.openCreatedGame(gameId)
        }
    }

    private fun playerWinKey(id: String): String = "player_win_count_${id}"

    private fun resolveWinners(players: List<com.example.shifumiplus.domain.PlayerState>): List<String> {
        val alive = players.filter { it.lives > 0 }
        if (alive.size == 1) return listOf(alive.first().id)
        if (alive.isNotEmpty()) return alive.map { it.id }

        val maxTurn = players.filter { it.lives <= 0 }.maxOfOrNull { it.eliminatedAtTurn ?: 0L } ?: 0L
        return players.filter { it.lives <= 0 && (it.eliminatedAtTurn ?: 0L) == maxTurn }.map { it.id }
    }
}
