package com.example.shifumiplus.data

import com.example.shifumiplus.domain.PlayerState
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

object FirestoreRepository {
    private val db = Firebase.firestore
    private val games = db.collection("games")

    private fun normalizeId(id: String) = id.trim().uppercase()

    suspend fun createGame(hostName: String): String {
        val id = generateId()
        val docRef = games.document(id)
        val payload = mapOf(
            "players" to listOf(hostName),
            "started" to false,
            "turn" to 0
        )
        docRef.set(payload).await()
        return id
    }

    suspend fun joinGame(idRaw: String, name: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val players = (snap.get("players") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val initialPlayers = (snap.get("initialPlayers") as? List<*>)?.mapNotNull { it as? String } ?: players
        val started = snap.getBoolean("started") ?: false

        if (players.contains(name)) return true

        if (started) {
            if (!initialPlayers.contains(name)) return false

            val defaultState = mapOf(
                "name" to name,
                "lives" to 3,
                "bullets" to 2,
                "protectedLastTurn" to false,
                "usedDoubleShoot" to false,
                "usedSuperProtection" to false,
                "usedBomb" to false,
                "usedBlock" to false
            )
            val currentPlayersState = (snap.get("playersState") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
            val nextPlayersState = currentPlayersState + defaultState
            docRef.update(
                mapOf(
                    "players" to FieldValue.arrayUnion(name),
                    "playersState" to nextPlayersState
                )
            ).await()
            return true
        }

        docRef.update("players", FieldValue.arrayUnion(name)).await()
        return true
    }

    suspend fun leaveGame(idRaw: String, playerName: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val players = (snap.get("players") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val nextPlayers = players.filterNot { it == playerName }

        if (nextPlayers.isEmpty()) {
            docRef.delete().await()
            return true
        }

        val updates = mutableMapOf<String, Any>(
            "players" to nextPlayers
        )

        val playersState = (snap.get("playersState") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
        if (playersState.isNotEmpty()) {
            updates["playersState"] = playersState.filterNot { (it["name"] as? String) == playerName }
        }

        val choices = (snap.get("choices") as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String
            val value = v as? Map<String, Any>
            if (key != null && value != null) key to value else null
        }?.toMap() ?: emptyMap()
        if (choices.isNotEmpty()) {
            val nextChoices = choices.toMutableMap()
            nextChoices.remove(playerName)
            updates["choices"] = nextChoices
        }

        docRef.update(updates).await()
        return true
    }

    // initialize the runtime game state (players with lives/bullets) and mark started
    suspend fun initializeGameState(idRaw: String, playerNames: List<String>) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val playersState = playerNames.map { mapOf("name" to it, "lives" to 3, "bullets" to 2, "protectedLastTurn" to false, "usedDoubleShoot" to false, "usedSuperProtection" to false, "usedBomb" to false, "usedBlock" to false) }
        val payload = mapOf(
            "playersState" to playersState,
            "started" to true,
            "choices" to mapOf<String, Any>(),
            "turn" to 1
        )
        docRef.update(payload).await()
    }

    suspend fun submitChoice(idRaw: String, playerName: String, action: String, target: String?) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val choiceMap = if (target != null) mapOf("action" to action, "target" to target) else mapOf("action" to action)
        // write choice into choices.<playerName>
        docRef.update("choices.${playerName}", choiceMap).await()
    }

    // listen to full game doc and expose playersState, choices, started, turn
    fun listenToGame(idRaw: String, onUpdate: (playersState: List<Map<String, Any>>, choices: Map<String, Map<String, Any>>?, started: Boolean, turn: Long?) -> Unit): ListenerRegistration {
        val id = normalizeId(idRaw)
        return games.document(id).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) return@addSnapshotListener

            val players = (snap.get("players") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (players.isEmpty()) {
                snap.reference.delete()
                return@addSnapshotListener
            }

            var playersState = (snap.get("playersState") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
            if (playersState.isEmpty()) {
                playersState = players.map { name -> mapOf("name" to name) }
            }

            val choices = (snap.get("choices") as? Map<*, *>)?.mapNotNull { (k, v) ->
                val key = k as? String
                val value = v as? Map<String, Any>
                if (key != null && value != null) key to value else null
            }?.toMap()
            val started = snap.getBoolean("started") ?: false
            val turn = snap.getLong("turn")
            onUpdate(playersState, choices, started, turn)
        }
    }

    suspend fun writeGameState(idRaw: String, playersState: List<Map<String, Any?>>, clearChoices: Boolean = true, turn: Long? = null) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val payload = mutableMapOf<String, Any>("playersState" to playersState)
        if (clearChoices) payload["choices"] = mapOf<String, Any>()
        if (turn != null) payload["turn"] = turn
        docRef.update(payload).await()
    }

    suspend fun deleteGame(idRaw: String) {
        val id = normalizeId(idRaw)
        games.document(id).delete().await()
    }

    suspend fun startGame(idRaw: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = docRef.get().await()
        val players = (snap.get("players") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return if (snap.exists() && players.size >= 2) {
            val playersState = players.map { mapOf("name" to it, "lives" to 3, "bullets" to 2, "protectedLastTurn" to false, "usedDoubleShoot" to false, "usedSuperProtection" to false, "usedBomb" to false, "usedBlock" to false) }
            val payload = mapOf(
                "playersState" to playersState,
                "initialPlayers" to players,
                "started" to true,
                "choices" to mapOf<String, Any>(),
                "turn" to 1
            )
            docRef.update(payload).await()
            true
        } else false
    }

    private fun generateId(length: Int = 6): String {
        val src = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..length).map { src.random() }.joinToString("")
    }
}
