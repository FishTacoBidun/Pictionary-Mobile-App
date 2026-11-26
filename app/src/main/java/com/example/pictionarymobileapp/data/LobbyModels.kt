package com.example.pictionarymobileapp.data

// Represents a player in a Pictionary lobby
data class Player(
    val id: String = "", // Firebase auth UID
    val name: String = "",
    val isHost: Boolean = false,
    val avatarId: Int = 1,
    val score: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)

// Represents a game lobby and its current state in Firestore
data class Lobby(
    val code: String = "",
    val hostUid: String = "",
    val hostUsername: String = "",
    val players: List<Player> = emptyList(),
    val theme: String = "Animals",
    val roundsPerPlayer: Int = 3,
    val timePerRoundSeconds: Int = 45,
    val maxPlayers: Int = 8,
    val status: String = "waiting", // Waiting, playing, round transition, or finished
    val currentRound: Int = 1,
    val currentDrawerUid: String = "",
    val drawerSchedule: List<String> = emptyList(),
    val wordChosen: Boolean = false,
    val availability: String = "private", // Private or public
    val gameIndex: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Total number of rounds in the game
    val totalRounds: Int
        get() = (roundsPerPlayer * players.size).coerceAtLeast(1)
}

// Result of attempting to join or create a lobby
sealed interface JoinResult {
    data class Success(val lobby: Lobby, val assignedUsername: String) : JoinResult
    data class Error(val message: String) : JoinResult
}

// Submitted guess record stored in Firestore for the current round
data class PlayerGuessRecord(
    val playerUid: String = "",
    val playerName: String = "",
    val guessText: String = "",
    val isCorrect: Boolean = false,
    val pointsAwarded: Int = 0,
    val submittedAt: Long = System.currentTimeMillis()
)
