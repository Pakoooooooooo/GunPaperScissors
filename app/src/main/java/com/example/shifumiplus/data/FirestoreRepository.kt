package com.example.shifumiplus.data

import android.annotation.SuppressLint
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object FirestoreRepository {
    enum class InviteGameStatus {
        AVAILABLE,
        MISSING,
        FINISHED,
        UNKNOWN
    }

    @SuppressLint("StaticFieldLeak")
    private val db = Firebase.firestore
    private val games = db.collection("games")
    private val replayStatus = db.collection("game_replay_status")

    private fun normalizeId(id: String) = id.trim().uppercase()

    private suspend fun <T> safeFirestoreCall(block: suspend () -> T): T? {
        return try {
            block()
        } catch (_: FirebaseFirestoreException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveGameSnapshot(idRaw: String): DocumentSnapshot? {
        val id = normalizeId(idRaw)
        if (id.isBlank()) return null

        val direct = safeFirestoreCall { games.document(id).get().await() } ?: return null
        if (direct.exists()) return direct

        val fallback = safeFirestoreCall { games.whereEqualTo("code", id).limit(1).get().await() } ?: return null
        return fallback.documents.firstOrNull()
    }

    private suspend fun resolveGameRef(idRaw: String): DocumentReference? {
        return resolveGameSnapshot(idRaw)?.reference
    }

    suspend fun gameExists(idRaw: String): Boolean? {
        val id = normalizeId(idRaw)
        if (id.isBlank()) return false

        val direct = safeFirestoreCall {
            games.document(id).get(Source.SERVER).await()
        } ?: return null
        if (direct.exists()) return true

        val fallback = safeFirestoreCall {
            games.whereEqualTo("code", id).limit(1).get(Source.SERVER).await()
        } ?: return null
        return fallback.documents.isNotEmpty()
    }

    suspend fun markReplayFinished(gameId: String, playerId: String): Boolean {
        if (gameId.isBlank() || playerId.isBlank()) return false
        return try {
            replayStatus.document(normalizeId(gameId))
                .collection("finished")
                .document(playerId)
                .set(mapOf("playerId" to playerId))
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun finishedReplayPlayerIds(gameId: String): Set<String> {
        return replayStatus.document(normalizeId(gameId))
            .collection("finished")
            .get(Source.SERVER)
            .await()
            .documents
            .map { it.id }
            .toSet()
    }

    private suspend fun clearReplayFinished(gameId: String, playerId: String) {
        replayStatus.document(normalizeId(gameId))
            .collection("finished")
            .document(playerId)
            .delete()
            .await()
    }

    suspend fun removeFinishedGameIfNeeded(idRaw: String): InviteGameStatus {
        val id = normalizeId(idRaw)
        if (id.isBlank()) return InviteGameStatus.MISSING

        val gameRef = games.document(id)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(gameRef)
                if (!snapshot.exists()) return@runTransaction InviteGameStatus.MISSING

                val started = snapshot.getBoolean("started") ?: false
                if (!started) return@runTransaction InviteGameStatus.AVAILABLE

                val playerStates = (snapshot.get("playersState") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?: emptyList()
                val playerEntries = if (playerStates.isNotEmpty()) {
                    playerStates
                } else {
                    (snapshot.get("players") as? List<*>)
                        ?.mapNotNull { it as? Map<*, *> }
                        ?: emptyList()
                }
                val aliveCount = playerEntries.count { player ->
                    val lives = (player["lives"] as? Number)?.toInt() ?: 3
                    lives > 0
                }

                if (aliveCount <= 1) {
                    transaction.delete(gameRef)
                    InviteGameStatus.FINISHED
                } else {
                    InviteGameStatus.AVAILABLE
                }
            }.await()
        } catch (_: Exception) {
            InviteGameStatus.UNKNOWN
        }
    }

    suspend fun createGame(hostId: String, hostName: String): String {
        val id = generateId()
        val docRef = games.document(id)
        val payload = mapOf(
            "code" to id,
            "players" to listOf(mapOf("id" to hostId, "name" to hostName)),
            "started" to false,
            "turn" to 0
        )
        return safeFirestoreCall { docRef.set(payload).await(); id } ?: ""
    }

    suspend fun joinGame(idRaw: String, playerId: String, name: String): Boolean {
        val id = normalizeId(idRaw)
        if (id.isBlank()) return false

        val snap = resolveGameSnapshot(id) ?: return false
        val docRef = snap.reference
        val snapRead = safeFirestoreCall { docRef.get().await() } ?: return false
        if (!snapRead.exists()) return false

        val playersRaw = (snapRead.get("players") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        val players = playersRaw.mapNotNull { it["id"] as? String }
        val initialPlayers = (snapRead.get("initialPlayers") as? List<*>)?.mapNotNull { it as? String } ?: players
        val started = snapRead.getBoolean("started") ?: false

        if (started) {
            if (initialPlayers.isEmpty()) return false
            if (!initialPlayers.contains(playerId)) return false
            if (players.contains(playerId)) return true

            val defaultState = mapOf(
                "id" to playerId,
                "name" to name,
                "lives" to 3,
                "bullets" to 2,
                "protectedLastTurn" to false,
                "usedDoubleShoot" to false,
                "usedSuperProtection" to false,
                "usedBomb" to false,
                "usedBlock" to false
            )
            val currentPlayersState = (snapRead.get("playersState") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            val nextPlayersState = currentPlayersState + defaultState
            val updated = safeFirestoreCall {
                docRef.update(
                    mapOf(
                        "players" to FieldValue.arrayUnion(mapOf("id" to playerId, "name" to name)),
                        "playersState" to nextPlayersState
                    )
                ).await()
            }
            val joined = updated != null
            if (joined) {
                try {
                    clearReplayFinished(id, playerId)
                } catch (_: Exception) {
                    // A stale marker is harmless; it will be replaced when the player finishes again.
                }
            }
            return joined
        }

        val joined = try {
            db.runTransaction { transaction ->
                val latest = transaction.get(docRef)
                if (!latest.exists()) return@runTransaction false

                val latestPlayers = (latest.get("players") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?: emptyList()
                val latestPlayerIds = latestPlayers.mapNotNull { it["id"] as? String }
                if (playerId in latestPlayerIds) return@runTransaction true

                val latestStarted = latest.getBoolean("started") ?: false
                val latestInitialPlayers = (latest.get("initialPlayers") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: latestPlayerIds
                if (latestStarted && playerId !in latestInitialPlayers) {
                    return@runTransaction false
                }

                val nextPlayers = latestPlayers + mapOf("id" to playerId, "name" to name)
                val existingStates = (latest.get("playersState") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?.associateBy { it["id"] as? String }
                    ?: emptyMap()
                val nextPlayersState = nextPlayers.mapNotNull { player ->
                    val id = player["id"] as? String ?: return@mapNotNull null
                    existingStates[id] ?: mapOf(
                        "id" to id,
                        "name" to (player["name"] as? String ?: ""),
                        "lives" to 3,
                        "bullets" to 2,
                        "protectedLastTurn" to false,
                        "usedDoubleShoot" to false,
                        "usedSuperProtection" to false,
                        "usedBomb" to false,
                        "usedBlock" to false
                    )
                }
                val updates = mutableMapOf<String, Any>(
                    "players" to nextPlayers,
                    "playersState" to nextPlayersState
                )
                if (latestStarted) {
                    updates["initialPlayers"] = latestInitialPlayers + playerId
                }
                transaction.update(docRef, updates)
                true
            }.await()
        } catch (_: Exception) {
            false
        }
        if (joined) {
            try {
                clearReplayFinished(id, playerId)
            } catch (_: Exception) {
                // A stale marker is harmless; it will be replaced when the player finishes again.
            }
        }
        return joined
    }

    suspend fun leaveGame(idRaw: String, playerId: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = safeFirestoreCall { docRef.get().await() } ?: return false
        if (!snap.exists()) return false

        val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        val nextPlayersRaw = playersRaw.filterNot { (it["id"] as? String) == playerId }

        if (nextPlayersRaw.isEmpty()) {
            return safeFirestoreCall { docRef.delete().await(); true } ?: false
        }

        val updates = mutableMapOf<String, Any>(
            "players" to nextPlayersRaw
        )

        val playersState = (snap.get("playersState") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        if (playersState.isNotEmpty()) {
            updates["playersState"] = playersState.filterNot { (it["id"] as? String) == playerId }
        }

        val choices = (snap.get("choices") as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String
            val value = v as? Map<*, *>
            if (key != null && value != null) key to value else null
        }?.toMap() ?: emptyMap()
        if (choices.isNotEmpty()) {
            val nextChoices = choices.toMutableMap()
            nextChoices.remove(playerId)
            updates["choices"] = nextChoices
        }

        return safeFirestoreCall { docRef.update(updates).await(); true } ?: false
    }

    suspend fun submitChoice(idRaw: String, playerId: String, action: String, target: String?) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val choiceMap = if (target != null) mapOf("action" to action, "target" to target) else mapOf("action" to action)
        safeFirestoreCall { docRef.update("choices.${playerId}", choiceMap).await() }
    }

    // listen to full game doc and expose playersState, choices, started, turn
    fun listenToGame(idRaw: String, onUpdate: (playersState: List<Map<String, Any>>, choices: Map<String, Map<String, Any>>?, started: Boolean, turn: Long?) -> Unit): ListenerRegistration {
        val id = normalizeId(idRaw)
        val ref = games.document(id)
        return ref.addSnapshotListener { snap, exception ->
            if (exception != null || snap == null || !snap.exists()) return@addSnapshotListener

            val playersState = resolvePlayersState(snap)
            val choices = resolveChoices(snap)
            val started = snap.getBoolean("started") ?: false
            val turn = snap.getLong("turn")
            onUpdate(playersState, choices, started, turn)
        }
    }

    private fun resolvePlayersState(snap: com.google.firebase.firestore.DocumentSnapshot): List<Map<String, Any>> {
        val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        if (playersRaw.isEmpty()) return emptyList()

        val playersStateById = (snap.get("playersState") as? List<*>)
            ?.mapNotNull { it as? Map<String, Any> }
            ?.associateBy { it["id"] as? String }
            ?: emptyMap()

        return playersRaw.mapNotNull { entry ->
            val id = entry["id"] as? String ?: return@mapNotNull null
            val name = entry["name"] as? String ?: ""
            val existingState = playersStateById[id]
            if (existingState != null) {
                existingState + mapOf("id" to id, "name" to name)
            } else {
                mapOf(
                    "id" to id,
                    "name" to name,
                    "lives" to 3,
                    "bullets" to 2,
                    "protectedLastTurn" to false,
                    "usedDoubleShoot" to false,
                    "usedSuperProtection" to false,
                    "usedBomb" to false,
                    "usedBlock" to false
                )
            }
        }
    }

    private fun resolveChoices(snap: com.google.firebase.firestore.DocumentSnapshot): Map<String, Map<String, Any>>? {
        return (snap.get("choices") as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String
            val value = v as? Map<String, Any>
            if (key != null && value != null) key to value else null
        }?.toMap()
    }

    suspend fun writeGameState(idRaw: String, playersState: List<Map<String, Any?>>, clearChoices: Boolean = true, turn: Long? = null) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val payload = mutableMapOf<String, Any>("playersState" to playersState)
        if (clearChoices) payload["choices"] = mapOf<String, Any>()
        if (turn != null) payload["turn"] = turn
        safeFirestoreCall { docRef.update(payload).await() }
    }

    suspend fun resetGameForReplay(
        idRaw: String,
        replayingPlayerId: String,
        fallbackPlayers: List<Map<String, String>>
    ): List<String>? {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        return try {
            val finishedPlayerIds = finishedReplayPlayerIds(id)
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                val currentPlayers = (snap.get("players") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?: emptyList()
                val storedReplayPlayers = (snap.get("replayPlayers") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?: emptyList()
                val replayPlayers = (storedReplayPlayers + currentPlayers + fallbackPlayers)
                    .mapNotNull { player ->
                        val participantId = player["id"] as? String ?: return@mapNotNull null
                        val participantName = player["name"] as? String ?: return@mapNotNull null
                        participantId to mapOf("id" to participantId, "name" to participantName)
                    }
                    .toMap()
                    .values
                    .toList()
                if (replayPlayers.none { it["id"] == replayingPlayerId }) {
                    return@runTransaction null
                }

                val lobbyPlayers = replayPlayers.filter { player ->
                    val participantId = player["id"] as? String ?: return@filter false
                    participantId == replayingPlayerId || participantId !in finishedPlayerIds
                }
                val lobbyPlayerIds = lobbyPlayers.mapNotNull { it["id"] as? String }
                val lobbyStates = lobbyPlayers.map { player ->
                    mapOf(
                        "id" to (player["id"] as? String ?: ""),
                        "name" to (player["name"] as? String ?: ""),
                        "lives" to 3,
                        "bullets" to 2,
                        "protectedLastTurn" to false,
                        "usedDoubleShoot" to false,
                        "usedSuperProtection" to false,
                        "usedBomb" to false,
                        "usedBlock" to false,
                        "eliminatedAtTurn" to null
                    )
                }
                val invitedIds = replayPlayers
                    .mapNotNull { it["id"] as? String }
                    .filter {
                        it.isNotBlank() &&
                            it != replayingPlayerId &&
                            it in finishedPlayerIds
                    }

                val payload = mapOf(
                    "code" to id,
                    "players" to lobbyPlayers,
                    "playersState" to lobbyStates,
                    "started" to false,
                    "choices" to mapOf<String, Any>(),
                    "turn" to 0,
                    "initialPlayers" to lobbyPlayerIds,
                    "replayPlayers" to replayPlayers
                )
                if (snap.exists()) {
                    transaction.update(docRef, payload)
                } else {
                    transaction.set(docRef, payload)
                }
                invitedIds
            }.await()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteGame(idRaw: String) {
        val id = normalizeId(idRaw)
        safeFirestoreCall { games.document(id).delete().await() }
    }

    suspend fun startGame(idRaw: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = safeFirestoreCall { docRef.get().await() } ?: return false
        val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        val players = playersRaw.mapNotNull { (it["id"] as? String) }
        return if (snap.exists() && players.size >= 2) {
            val playersState = playersRaw.map { entry ->
                mapOf(
                    "id" to (entry["id"] as? String ?: ""),
                    "name" to (entry["name"] as? String ?: ""),
                    "lives" to 3,
                    "bullets" to 2,
                    "protectedLastTurn" to false,
                    "usedDoubleShoot" to false,
                    "usedSuperProtection" to false,
                    "usedBomb" to false,
                    "usedBlock" to false
                )
            }
            val payload = mapOf(
                "playersState" to playersState,
                "initialPlayers" to players,
                "replayPlayers" to playersRaw,
                "started" to true,
                "choices" to mapOf<String, Any>(),
                "turn" to 1
            )
            safeFirestoreCall { docRef.update(payload).await(); true } ?: false
        } else false
    }

    private fun generateId(length: Int = 6): String {
        val src = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..length).map { src.random() }.joinToString("")
    }
}
