package com.example.shifumiplus

data class Player(val name: String)

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
