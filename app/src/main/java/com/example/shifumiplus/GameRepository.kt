
package com.example.shifumiplus.data

import com.example.shifumiplus.domain.Player
import java.util.UUID
import java.util.Locale
import kotlin.random.Random

object GameRepository {
    private val games = mutableMapOf<String, MutableList<Player>>()
    private val started = mutableSetOf<String>()

    private fun generateId(length: Int = 6): String {
        val src = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..length).map { src[Random.nextInt(src.length)] }.joinToString("")
    }

    private fun generatePlayerId(): String = UUID.randomUUID().toString().replace("-", "").uppercase().take(8)

    fun createGame(hostName: String): String {
        val id = generateId()
        // create a host player with generated id
        games[id] = mutableListOf(Player(generatePlayerId(), hostName))
        return id
    }

    fun joinGame(id: String, name: String): Boolean {
        val normalized = id.uppercase(Locale.getDefault())
        val list = games[normalized] ?: games[id]
        return if (list != null) {
            list.add(Player(generatePlayerId(), name))
            true
        } else false
    }

    fun getPlayers(id: String): List<Player>? {
        return games[id]
    }

    fun startGame(id: String): Boolean {
        val list = games[id]
        return if (list != null && list.size >= 2) {
            started.add(id)
            true
        } else false
    }

    fun gameExists(id: String): Boolean = games.containsKey(id)
}