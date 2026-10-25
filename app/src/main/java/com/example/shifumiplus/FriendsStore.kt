package com.example.shifumiplus

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

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
}
