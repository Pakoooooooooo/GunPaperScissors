package com.example.shifumiplus.domain

data class Player(val name: String)

data class Game(val id: String, val players: List<Player>, val started: Boolean = false)

// Runtime state for a player in a game
data class PlayerState(
    val name: String,
    val lives: Int = 3,
    val bullets: Int = 2,
    val protectedLastTurn: Boolean = false,
    val usedDoubleShoot: Boolean = false,
    val usedSuperProtection: Boolean = false,
    val usedBomb: Boolean = false,
    val usedBlock: Boolean = false,
    val eliminatedAtTurn: Long? = null
)
