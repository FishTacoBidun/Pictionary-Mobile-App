package com.example.pictionarymobileapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.random.Random

object LobbySessionManager {

    private val activeLobbies = MutableStateFlow<Map<String, Lobby>>(emptyMap())

    // Observable stream of all active lobbies keyed by lobby join code
    val allLobbies: StateFlow<Map<String, Lobby>> = activeLobbies.asStateFlow()

    // Creates a new lobby and generates a unique 6-character code and adds the host as the first player
    fun createLobby(hostUsername: String): Lobby {
        val cleanName = hostUsername.trim().ifBlank { "Host" }
        val code = generateUniqueCode()
        val hostPlayer = Player(name = cleanName, isHost = true)
        val lobby = Lobby(
            code = code,
            hostUsername = cleanName,
            players = listOf(hostPlayer)
        )

        activeLobbies.update { current ->
            current + (code to lobby)
        }

        return lobby
    }

    // Joins an existing lobby identified and validates that the lobby exists and is not full
    fun joinLobby(code: String, username: String): JoinResult {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val cleanName = username.trim().ifBlank { "Player" }

        val currentLobbies = activeLobbies.value
        val lobby = currentLobbies[formattedCode]
            ?: return JoinResult.Error("Lobby does not exist. Check the code and try again.")

        if (lobby.players.size >= lobby.maxPlayers) {
            return JoinResult.Error("Lobby '$formattedCode' is full (max ${lobby.maxPlayers} players).")
        }

        // Avoid duplicate exact player names in the same lobby
        val finalUsername = if (lobby.players.any { it.name.equals(cleanName, ignoreCase = true) }) {
            "$cleanName#${Random.nextInt(100, 999)}"
        } else {
            cleanName
        }

        val newPlayer = Player(name = finalUsername, isHost = false)
        val updatedLobby = lobby.copy(players = lobby.players + newPlayer)

        activeLobbies.update { current ->
            current + (formattedCode to updatedLobby)
        }

        return JoinResult.Success(updatedLobby, finalUsername)
    }

    // Removes everyone from lobby if the host leaves or disbands
    fun leaveLobby(code: String, username: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentLobbies = activeLobbies.value
        val lobby = currentLobbies[formattedCode] ?: return

        val player = lobby.players.find { it.name.equals(username, ignoreCase = true) }
        if (player == null || player.isHost || lobby.hostUsername.equals(username, ignoreCase = true)) {
            // Host leaving
            disbandLobby(formattedCode)
        } else {
            // Guest leaving
            val updatedPlayers = lobby.players.filterNot { it.name.equals(username, ignoreCase = true) }
            val updatedLobby = lobby.copy(players = updatedPlayers)
            activeLobbies.update { current ->
                current + (formattedCode to updatedLobby)
            }
        }
    }

    // Disbands and deletes the lobby
    fun disbandLobby(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        activeLobbies.update { current ->
            current - formattedCode
        }
    }

    // Retrieves the current snapshot of a lobby by the code
    fun getLobby(code: String): Lobby? {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        return activeLobbies.value[formattedCode]
    }


    // Updates lobby settings (theme and rounds per player)
    fun updateSettings(code: String, theme: String, roundsPerPlayer: Int) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        activeLobbies.update { current ->
            val existing = current[formattedCode] ?: return@update current
            current + (formattedCode to existing.copy(theme = theme, roundsPerPlayer = roundsPerPlayer))
        }
    }

    // Generates a unique 6-character uppercase alphanumeric code
    private fun generateUniqueCode(): String {
        val charPool = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        do {
            code = (1..6)
                .map { Random.nextInt(0, charPool.length) }
                .map(charPool::get)
                .joinToString("")
        } while (activeLobbies.value.containsKey(code))
        return code
    }
}
