package com.example.shifumiplus

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class FriendProfile(
    val id: String,
    val name: String,
) {
    fun encode(): String = "$id|$name"

    companion object {
        fun decode(raw: String): FriendProfile? {
            val parts = raw.split("|", limit = 2)
            if (parts.size != 2) return null
            val id = parts[0].trim()
            val name = parts[1].trim()
            if (id.isBlank() || name.isBlank()) return null
            return FriendProfile(id, name)
        }
    }
}

data class GameInvite(
    val id: String,
    val gameId: String,
    val senderName: String,
    val createdAt: Long
)

object FriendsStore {
    private const val PREF_NAME = "friends_prefs"
    private val db by lazy { Firebase.firestore }
    private const val FRIENDS_COLLECTION = "user_friends"
    private const val INVITES_COLLECTION = "game_invites"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun encodeCurrentUser(playerId: String, playerName: String): String =
        FriendProfile(playerId, playerName).encode()

    fun getFriends(context: Context, playerId: String): List<FriendProfile> {
        val set = prefs(context).getStringSet("friends_$playerId", emptySet()) ?: emptySet()
        return set.mapNotNull { FriendProfile.decode(it) }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
    }

    fun addFriend(context: Context, playerId: String, friend: FriendProfile) {
        val updated = getFriends(context, playerId)
            .filter { it.id != friend.id }
            .plus(friend)
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        prefs(context).edit {
            putStringSet(
                "friends_$playerId",
                updated.map { it.encode() }.toSet()
            )
        }
    }

    fun removeFriend(context: Context, playerId: String, friendId: String) {
        val updated = getFriends(context, playerId)
            .filter { it.id != friendId }
            .sortedBy { it.name.lowercase() }

        prefs(context).edit {
            putStringSet(
                "friends_$playerId",
                updated.map { it.encode() }.toSet()
            )
        }
    }

    private fun parseFriends(raw: Any?): List<FriendProfile> =
        (raw as? List<*>)
            ?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val id = map["id"] as? String ?: return@mapNotNull null
                val name = map["name"] as? String ?: return@mapNotNull null
                FriendProfile(id, name)
            }
            ?.distinctBy { it.id }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    suspend fun fetchFriends(context: Context, playerId: String): List<FriendProfile> {
        val localFriends = getFriends(context, playerId)
        return try {
            val snapshot = db.collection(FRIENDS_COLLECTION).document(playerId).get().await()
            if (snapshot.exists() && snapshot.contains("friends")) {
                parseFriends(snapshot.get("friends"))
            } else {
                localFriends
            }
        } catch (_: Exception) {
            localFriends
        }
    }

    fun listenToFriends(
        context: Context,
        playerId: String,
        onUpdate: (List<FriendProfile>) -> Unit
    ): ListenerRegistration {
        return db.collection(FRIENDS_COLLECTION).document(playerId)
            .addSnapshotListener { snapshot, _ ->
                val result = try {
                    if (snapshot != null && snapshot.exists() && snapshot.contains("friends")) {
                        parseFriends(snapshot.get("friends"))
                    } else {
                        getFriends(context, playerId)
                    }
                } catch (_: Exception) {
                    getFriends(context, playerId)
                }
                onUpdate(result)
            }
    }

    private suspend fun fetchRemoteFriends(playerId: String): List<FriendProfile> {
        return try {
            val snapshot = db.collection(FRIENDS_COLLECTION).document(playerId).get().await()
            parseFriends(snapshot.get("friends"))
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addMutualFriend(context: Context, currentUser: FriendProfile, friend: FriendProfile) {
        if (currentUser.id == friend.id) return

        val meExisting = (fetchRemoteFriends(currentUser.id) + getFriends(context, currentUser.id))
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        val myUpdatedList = (meExisting.filter { it.id != friend.id } + friend)
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        val friendExisting = (fetchRemoteFriends(friend.id) + getFriends(context, friend.id))
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        val friendUpdatedList = (friendExisting.filter { it.id != currentUser.id } + currentUser)
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        addFriend(context, currentUser.id, friend)
        addFriend(context, friend.id, currentUser)

        try {
            db.collection(FRIENDS_COLLECTION).document(currentUser.id)
                .set(mapOf("friends" to myUpdatedList.map { mapOf("id" to it.id, "name" to it.name) }), SetOptions.merge())
                .await()
            db.collection(FRIENDS_COLLECTION).document(friend.id)
                .set(mapOf("friends" to friendUpdatedList.map { mapOf("id" to it.id, "name" to it.name) }), SetOptions.merge())
                .await()
        } catch (_: Exception) {
            // Fallback local-only if Firestore is temporarily unavailable.
        }
    }

    suspend fun removeMutualFriend(context: Context, currentUserId: String, otherUserId: String) {
        if (currentUserId == otherUserId) return

        val meExisting = (fetchRemoteFriends(currentUserId) + getFriends(context, currentUserId))
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        val myUpdatedList = meExisting.filter { it.id != otherUserId }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        val otherExisting = (fetchRemoteFriends(otherUserId) + getFriends(context, otherUserId))
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        val otherUpdatedList = otherExisting.filter { it.id != currentUserId }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        removeFriend(context, currentUserId, otherUserId)
        removeFriend(context, otherUserId, currentUserId)

        try {
            db.collection(FRIENDS_COLLECTION).document(currentUserId)
                .set(mapOf("friends" to myUpdatedList.map { mapOf("id" to it.id, "name" to it.name) }), SetOptions.merge())
                .await()
            db.collection(FRIENDS_COLLECTION).document(otherUserId)
                .set(mapOf("friends" to otherUpdatedList.map { mapOf("id" to it.id, "name" to it.name) }), SetOptions.merge())
                .await()
        } catch (_: Exception) {
            // Fallback local-only if Firestore is temporarily unavailable.
        }
    }

    suspend fun createGameInvites(
        senderId: String,
        senderName: String,
        invitedIds: List<String>,
        gameId: String
    ) {
        val recipientIds = invitedIds
            .filter { it.isNotBlank() && it != senderId }
            .distinct()
        if (recipientIds.isEmpty()) return

        val batch = db.batch()
        recipientIds.forEach { recipientId ->
            val inviteRef = db.collection(INVITES_COLLECTION)
                .document(recipientId)
                .collection("inbox")
                .document()
            batch.set(
                inviteRef,
                mapOf(
                    "senderId" to senderId,
                    "senderName" to senderName,
                    "gameId" to gameId,
                    "createdAt" to System.currentTimeMillis()
                )
            )
        }
        batch.commit().await()
    }

    fun listenToGameInvites(
        playerId: String,
        onUpdate: (List<GameInvite>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(INVITES_COLLECTION)
            .document(playerId)
            .collection("inbox")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val invites = snapshot.documents.mapNotNull { document ->
                    val gameId = document.getString("gameId") ?: return@mapNotNull null
                    val senderName = document.getString("senderName") ?: return@mapNotNull null
                    GameInvite(
                        id = document.id,
                        gameId = gameId,
                        senderName = senderName,
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                onUpdate(invites)
            }
    }

    suspend fun fetchGameInvites(playerId: String): List<GameInvite> {
        val snapshot = db.collection(INVITES_COLLECTION)
            .document(playerId)
            .collection("inbox")
            .get(Source.SERVER)
            .await()

        return snapshot.documents.mapNotNull { document ->
            val gameId = document.getString("gameId") ?: return@mapNotNull null
            val senderName = document.getString("senderName") ?: return@mapNotNull null
            GameInvite(
                id = document.id,
                gameId = gameId,
                senderName = senderName,
                createdAt = document.getLong("createdAt") ?: 0L
            )
        }.sortedByDescending { it.createdAt }
    }

    suspend fun deleteGameInvite(playerId: String, inviteId: String) {
        db.collection(INVITES_COLLECTION)
            .document(playerId)
            .collection("inbox")
            .document(inviteId)
            .delete()
            .await()
    }
}
