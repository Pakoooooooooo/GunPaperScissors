package com.example.shifumiplus.data

import android.annotation.SuppressLint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

object FirestoreRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = Firebase.firestore
    private val games = db.collection("games")

    private fun normalizeId(id: String) = id.trim().uppercase()

    suspend fun createGame(hostId: String, hostName: String): String {
        val id = generateId()
        val docRef = games.document(id)
        val payload = mapOf(
            // store players as list of maps {id,name}
            "players" to listOf(mapOf("id" to hostId, "name" to hostName)),
            "started" to false,
            "turn" to 0
        )
        docRef.set(payload).await()
        return id
    }

    suspend fun joinGame(idRaw: String, playerId: String, name: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
        val players = playersRaw.mapNotNull { it["id"] as? String }
        val initialPlayers = (snap.get("initialPlayers") as? List<*>)?.mapNotNull { it as? String } ?: players
        val started = snap.getBoolean("started") ?: false

        // Once a game has started, outsiders are never allowed to enter.
        // The only valid joiners are players who were already part of the initial roster,
        // and they can rejoin as long as the game still exists.
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
            val currentPlayersState = (snap.get("playersState") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
            val nextPlayersState = currentPlayersState + defaultState
            docRef.update(
                mapOf(
                    "players" to FieldValue.arrayUnion(mapOf("id" to playerId, "name" to name)),
                    "playersState" to nextPlayersState
                )
            ).await()
            return true
        }

        if (players.contains(playerId)) return true
        docRef.update("players", FieldValue.arrayUnion(mapOf("id" to playerId, "name" to name))).await()
        return true
    }

    suspend fun leaveGame(idRaw: String, playerId: String): Boolean {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
        val nextPlayersRaw = playersRaw.filterNot { (it["id"] as? String) == playerId }

        if (nextPlayersRaw.isEmpty()) {
            docRef.delete().await()
            return true
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

        docRef.update(updates).await()
        return true
    }

    suspend fun initializeGameState(idRaw: String, playerEntries: List<Map<String, String>>) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val playersState = playerEntries.map { entry ->
            mapOf(
                "id" to (entry["id"] ?: ""),
                "name" to (entry["name"] ?: ""),
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
            "started" to true,
            "choices" to mapOf<String, Any>(),
            "turn" to 1,
            "initialPlayers" to playerEntries.mapNotNull { it["id"] }
        )
        docRef.update(payload).await()
    }

    suspend fun submitChoice(idRaw: String, playerId: String, action: String, target: String?) {
        val id = normalizeId(idRaw)
        val docRef = games.document(id)
        val choiceMap = if (target != null) mapOf("action" to action, "target" to target) else mapOf("action" to action)
        // write choice into choices.<playerId>
        docRef.update("choices.${playerId}", choiceMap).await()
    }

    // listen to full game doc and expose playersState, choices, started, turn
    fun listenToGame(idRaw: String, onUpdate: (playersState: List<Map<String, Any>>, choices: Map<String, Map<String, Any>>?, started: Boolean, turn: Long?) -> Unit): ListenerRegistration {
        val id = normalizeId(idRaw)
        return games.document(id).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) return@addSnapshotListener

            val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
            if (playersRaw.isEmpty()) {
                snap.reference.delete()
                return@addSnapshotListener
            }

            var playersState = (snap.get("playersState") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
            if (playersState.isEmpty()) {
                playersState = playersRaw.map { entry -> mapOf("id" to (entry["id"] as? String ?: ""), "name" to (entry["name"] as? String ?: "")) }
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
        val playersRaw = (snap.get("players") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
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
