package com.example.shifumiplus

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.firestore.ListenerRegistration
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

object FriendsStore {
    private const val PREF_NAME = "friends_prefs"
    private val db = Firebase.firestore
    private const val FRIENDS_COLLECTION = "user_friends"

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
            val remoteFriends = parseFriends(snapshot.get("friends"))
            (remoteFriends + localFriends)
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }
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
                val merged = try {
                    val remoteFriends = parseFriends(snapshot?.get("friends"))
                    val localFriends = getFriends(context, playerId)
                    (remoteFriends + localFriends).distinctBy { it.id }.sortedBy { it.name.lowercase() }
                } catch (_: Exception) {
                    getFriends(context, playerId)
                }
                onUpdate(merged)
            }
    }

    suspend fun addMutualFriend(context: Context, currentUser: FriendProfile, friend: FriendProfile) {
        if (currentUser.id == friend.id) return

        val meExisting = fetchFriends(context, currentUser.id)
        val myUpdatedList = (meExisting.filter { it.id != friend.id } + friend)
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        val friendExisting = fetchFriends(context, friend.id)
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
}
