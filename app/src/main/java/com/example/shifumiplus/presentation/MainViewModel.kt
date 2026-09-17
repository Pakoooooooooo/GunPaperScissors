package com.example.shifumiplus.presentation

import androidx.lifecycle.ViewModel
import com.example.shifumiplus.data.FirestoreRepository
import com.example.shifumiplus.domain.PlayerState
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    sealed class Screen {
        object Name : Screen()
        object MainMenu : Screen()
        object Join : Screen()
        data class Lobby(val gameId: String) : Screen()
        data class Game(val gameId: String) : Screen()
        data class FinalRanking(val gameId: String, val ranking: List<String>) : Screen()
    }

    data class UiState(
        val screen: Screen = Screen.Name,
        val playerName: String? = null,
        val playerId: String? = null,
        val currentGameId: String? = null,
        val currentPlayers: List<PlayerState> = emptyList(),
        val started: Boolean = false,
        val choices: Map<String, Map<String, Any>> = emptyMap(),
        val turn: Long = 0,
        val finalRanking: List<String> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var listener: ListenerRegistration? = null

    fun setPlayer(name: String, id: String) {
        _uiState.value = _uiState.value.copy(playerName = name, playerId = id)
    }

    fun setPlayerName(name: String) {
        _uiState.value = _uiState.value.copy(playerName = name)
    }

    fun navigateTo(screen: Screen) {
        // detach previous listener when moving away from a game/lobby
        if (screen !is Screen.Lobby && screen !is Screen.Game) {
            listener?.remove()
            listener = null
        }

        when (screen) {
            is Screen.Lobby -> attachListener(screen.gameId)
            is Screen.Game -> attachListener(screen.gameId)
            else -> _uiState.value = _uiState.value.copy(screen = screen)
        }
    }

    private fun attachListener(gameId: String) {
        listener?.remove()
        listener = FirestoreRepository.listenToGame(gameId) { playersStateRaw, choicesRaw, started, turn ->
            val players = playersStateRaw.map { m ->
                PlayerState(
                    id = (m["id"] as? String) ?: "",
                    name = (m["name"] as? String) ?: "",
                    lives = (m["lives"] as? Long)?.toInt() ?: (m["lives"] as? Int) ?: 3,
                    bullets = (m["bullets"] as? Long)?.toInt() ?: (m["bullets"] as? Int) ?: 2,
                    protectedLastTurn = (m["protectedLastTurn"] as? Boolean) ?: false,
                    usedDoubleShoot = (m["usedDoubleShoot"] as? Boolean) ?: false,
                    usedSuperProtection = (m["usedDoubleShoot"] as? Boolean) ?: false,
                    usedBomb = (m["usedBomb"] as? Boolean) ?: false,
                    usedBlock = (m["usedBlock"] as? Boolean) ?: false,
                    eliminatedAtTurn = (m["eliminatedAtTurn"] as? Long)
                )
            }
            val choices = choicesRaw ?: emptyMap()
            val activePlayers = players.filter { it.lives > 0 }
            val activeChoices = choices.filterKeys { key -> activePlayers.any { it.id == key } }

            val ranking = computeFinalRanking(players)
            val gameEnded = started && isGameFinished(players)
            val screen = when {
                gameEnded -> Screen.FinalRanking(gameId, ranking)
                started -> Screen.Game(gameId)
                else -> Screen.Lobby(gameId)
            }

            _uiState.value = _uiState.value.copy(
                screen = screen,
                currentGameId = gameId,
                currentPlayers = players,
                started = started,
                choices = choices,
                turn = turn ?: 0,
                finalRanking = ranking
            )

            // Only still-alive players must pick an action; eliminated players are ignored.
            if (!gameEnded && activePlayers.isNotEmpty() && activeChoices.size == activePlayers.size) {
                tryResolveTurn(gameId, players, activeChoices)
            }
        }
    }

    fun createGame() {
        val name = _uiState.value.playerName ?: return
        val myId = _uiState.value.playerId ?: return
        scope.launch {
            val id = FirestoreRepository.createGame(myId, name)
            attachListener(id)
        }
    }

    fun joinGame(idRaw: String) {
        val id = idRaw.trim()
        val name = _uiState.value.playerName ?: return
        val myId = _uiState.value.playerId ?: return
        scope.launch {
            val success = FirestoreRepository.joinGame(id, myId, name)
            if (success) attachListener(id)
        }
    }

    fun leaveGame() {
        val id = _uiState.value.currentGameId ?: return
        val myId = _uiState.value.playerId ?: return
        scope.launch {
            FirestoreRepository.leaveGame(id, myId)
            listener?.remove()
            listener = null
            _uiState.value = _uiState.value.copy(
                screen = Screen.MainMenu,
                currentGameId = null,
                currentPlayers = emptyList(),
                started = false,
                choices = emptyMap(),
                turn = 0,
                finalRanking = emptyList()
            )
        }
    }

    fun finishGame() {
        val id = _uiState.value.currentGameId ?: return
        scope.launch {
            FirestoreRepository.deleteGame(id)
            listener?.remove()
            listener = null
            _uiState.value = _uiState.value.copy(
                screen = Screen.MainMenu,
                currentGameId = null,
                currentPlayers = emptyList(),
                started = false,
                choices = emptyMap(),
                turn = 0,
                finalRanking = emptyList()
            )
        }
    }

    fun startGame() {
        val id = _uiState.value.currentGameId ?: return
        scope.launch {
            val ok = FirestoreRepository.startGame(id)
            if (ok) {
                println("Could not start game")
            }
        }
    }

    fun submitAction(action: String, target: String?) {
        val id = _uiState.value.currentGameId ?: return
        val meId = _uiState.value.playerId ?: return
        val myState = _uiState.value.currentPlayers.find { it.id == meId } ?: return
        if (myState.lives <= 0) return
        if (!_uiState.value.started || isGameFinished(_uiState.value.currentPlayers)) return
        scope.launch {
            FirestoreRepository.submitChoice(id, meId, action, target)
        }
    }

    private fun isGameFinished(players: List<PlayerState>): Boolean {
        val alive = players.filter { it.lives > 0 }
        return alive.size <= 1
    }

    private fun computeFinalRanking(players: List<PlayerState>): List<String> {
        val survivors = players.filter { it.lives > 0 }.map { it.name }
        val eliminated = players.filter { it.lives <= 0 }
            .sortedWith(compareByDescending<PlayerState> { it.eliminatedAtTurn ?: 0L }.thenBy { it.name })
            .map { it.name }
        return survivors + eliminated
    }

    private fun tryResolveTurn(gameId: String, players: List<PlayerState>, choices: Map<String, Map<String, Any>>) {
        // simple client-side resolver: one of the clients will perform the resolution
        // resolution must be idempotent; we will compute new playersState and write it back
        scope.launch {
            val activePlayers = players.filter { it.lives > 0 }
            val activeIds = activePlayers.map { it.id }.toSet()
            val activeChoices = choices.filterKeys { it in activeIds }

            // Build action maps and initial state using ids
            val actions = activeChoices.mapValues { it.value["action"] as? String ?: "" }
            val targetsRaw = activeChoices.mapValues { it.value["target"] as? String }

            // determine blocks (who blocked whom)
            val blockedBy = mutableMapOf<String, String>() // targetId -> blockerId
            actions.forEach { (playerId, action) ->
                if (action == "Block") {
                    val target = targetsRaw[playerId]
                    if (!target.isNullOrBlank()) blockedBy[target.trim()] = playerId
                }
            }

            // copy current players to mutable map by id
            val stateById = players.associateBy({ it.id }, { it }).toMutableMap()
            val currentTurn = _uiState.value.turn

            // Track which actions are canceled due to being blocked
            val canceled = mutableSetOf<String>()
            // When blocked, mark used flags for the canceled action per spec
            blockedBy.forEach { (targetId, blockerId) ->
                // mark blocker usedBlock
                stateById[blockerId]?.let { stateById[blockerId] = it.copy(usedBlock = true) }
                val targetAction = actions[targetId]
                if (targetAction != null) {
                    canceled.add(targetId)
                    val cur = stateById[targetId]
                    if (cur != null) {
                        when (targetAction) {
                            "DoubleShoot" -> stateById[targetId] = cur.copy(usedDoubleShoot = true)
                            "SuperProtect" -> stateById[targetId] = cur.copy(usedSuperProtection = true)
                            "Bomb" -> stateById[targetId] = cur.copy(usedBomb = true)
                            "Protect" -> stateById[targetId] = cur.copy(protectedLastTurn = true)
                            "Shoot" -> {
                                // shooter keeps bullet per spec; nothing to change
                            }
                        }
                    }
                }
            }

            // Compute protections (simple Protect and SuperProtect) for non-canceled actions
            val protectedThisTurn = mutableSetOf<String>()
            val superProtected = mutableSetOf<String>()
            actions.forEach { (playerId, action) ->
                if (!canceled.contains(playerId)) {
                    if (action == "Protect") protectedThisTurn.add(playerId)
                    if (action == "SuperProtect") {
                        protectedThisTurn.add(playerId)
                        superProtected.add(playerId)
                        // mark usedSuperProtection
                        stateById[playerId]?.let { stateById[playerId] = it.copy(usedSuperProtection = true) }
                    }
                }
            }

            // Prepare list of hits: each entry is (shooterId, targetId)
            val hits = mutableListOf<Pair<String, String>>()

            // Process actions (reload, shoot, double, bomb) for non-canceled
            actions.forEach { (playerId, action) ->
                if (canceled.contains(playerId)) return@forEach
                val cur = stateById[playerId] ?: return@forEach
                when (action) {
                    "Reload" -> stateById[playerId] = cur.copy(bullets = cur.bullets + 1)
                    "Shoot" -> if (cur.bullets > 0) {
                        val target = targetsRaw[playerId]?.trim()
                        if (!target.isNullOrBlank()) {
                            hits.add(playerId to target)
                            stateById[playerId] = cur.copy(bullets = cur.bullets - 1)
                        }
                    }
                    "DoubleShoot" -> {
                        // condition required at least 2 bullets; consume 1 bullet per spec
                        val tRaw = targetsRaw[playerId] ?: ""
                        val parts = tRaw.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                        // expect two targets; if fewer, ignore
                        if (cur.bullets >= 2 && !stateById[playerId]!!.usedDoubleShoot) {
                            // consume 1 bullet as per spec
                            stateById[playerId] = cur.copy(bullets = cur.bullets - 1, usedDoubleShoot = true)
                            val t1 = parts.getOrNull(0)
                            val t2 = parts.getOrNull(1)
                            if (!t1.isNullOrBlank()) hits.add(playerId to t1)
                            if (!t2.isNullOrBlank()) hits.add(playerId to t2)
                        }
                    }
                    "Bomb" -> {
                        if (cur.bullets >= 2 && !cur.usedBomb) {
                            stateById[playerId] = cur.copy(bullets = cur.bullets - 2, usedBomb = true)
                        }
                    }
                    // Protect and SuperProtect handled
                }
            }

            // Apply SuperProtect consequences: shooters who hit a superProtected player lose life per incoming bullet and regain their bullets
            val hitsByTarget = hits.groupBy { it.second }
            for (protectedPlayerId in superProtected) {
                val shooters = hitsByTarget[protectedPlayerId] ?: emptyList()
                shooters.groupingBy { it.first }.eachCount().forEach { (shooterId, count) ->
                    // shooter loses 'count' lives and regains 'count' bullets
                    stateById[shooterId]?.let { s ->
                        stateById[shooterId] = s.copy(lives = s.lives - count, bullets = s.bullets + count)
                    }
                }
                // shots toward protectedPlayer do not harm them (handled below by checking superProtected)
            }

            // Apply normal hits (excluding those already handled by superProtected)
            hits.forEach { (_, targetId) ->
                val tgt = stateById[targetId]
                if (tgt != null) {
                    // if target is superProtected or protectedThisTurn, they don't lose life
                    if (superProtected.contains(targetId) || protectedThisTurn.contains(targetId)) {
                        // no damage
                    } else {
                        stateById[targetId] = tgt.copy(lives = tgt.lives - 1)
                    }
                }
            }

            // Apply bombs: each bomber causes all non-protected players (excluding those protectedThisTurn or superProtected) to lose 1 life
            actions.forEach { (playerId, action) ->
                if (action == "Bomb" && !canceled.contains(playerId)) {
                    stateById.forEach { (pid, pst) ->
                        if (!protectedThisTurn.contains(pid) && !superProtected.contains(pid)) {
                            stateById[pid] = pst.copy(lives = pst.lives - 1)
                        }
                    }
                }
            }

            // Apply block side effects already handled: blocked target's actions canceled and flags set

            // Update protectedLastTurn flags: those who used Protect this turn (even if canceled earlier by block and we marked) should have protectedLastTurn=true; others false
            val finalPlayersState = stateById.values.map { ps ->
                val protectedLast = protectedThisTurn.contains(ps.name)
                val eliminatedAt = when {
                    ps.lives > 0 -> null
                    ps.eliminatedAtTurn != null -> ps.eliminatedAtTurn
                    else -> currentTurn
                }
                PlayerState(
                    id = ps.id,
                    name = ps.name,
                    lives = ps.lives,
                    bullets = ps.bullets,
                    protectedLastTurn = protectedLast,
                    usedDoubleShoot = ps.usedDoubleShoot,
                    usedSuperProtection = ps.usedSuperProtection,
                    usedBomb = ps.usedBomb,
                    usedBlock = ps.usedBlock,
                    eliminatedAtTurn = eliminatedAt
                )
            }

            // write the new game state and clear choices and increment turn
            val nextTurn = currentTurn + 1
            FirestoreRepository.writeGameState(
                gameId,
                finalPlayersState.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "lives" to it.lives,
                        "bullets" to it.bullets,
                        "protectedLastTurn" to it.protectedLastTurn,
                        "usedDoubleShoot" to it.usedDoubleShoot,
                        "usedSuperProtection" to it.usedSuperProtection,
                        "usedBomb" to it.usedBomb,
                        "usedBlock" to it.usedBlock,
                        "eliminatedAtTurn" to it.eliminatedAtTurn
                    )
                },
                clearChoices = true,
                turn = nextTurn
            )
        }
    }
}
