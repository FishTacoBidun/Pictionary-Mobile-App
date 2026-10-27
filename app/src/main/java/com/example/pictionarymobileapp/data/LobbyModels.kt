package com.example.pictionarymobileapp.data

import java.util.UUID

 // Represents a player in a Pictionary lobby
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isHost: Boolean = false
)

// Represents a game lobby and its current state
data class Lobby(
    val code: String,
    val hostUsername: String,
    val players: List<Player> = emptyList(),
    val theme: String = "Animals",
    val roundsPerPlayer: Int = 1,
    val maxPlayers: Int = 6,
    val isGameStarted: Boolean = false
)

// Result of attempting to join a lobby
sealed interface JoinResult {
    data class Success(val lobby: Lobby, val assignedUsername: String) : JoinResult
    data class Error(val message: String) : JoinResult
}
