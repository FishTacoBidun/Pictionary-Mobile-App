package com.example.pictionarymobileapp

import com.example.pictionarymobileapp.data.JoinResult
import com.example.pictionarymobileapp.data.LobbySessionManager
import org.junit.Assert.*
import org.junit.Test

class LobbySessionTest {

    @Test
    fun testLobbyCreationAndJoiningFlow() {
        println("Pictionary lobby test")

        // Host creates a lobby
        val hostName = "Wolfie"
        val lobby = LobbySessionManager.createLobby(hostName)
        val code = lobby.code

        println("Lobby created with code $code")
        assertEquals(1, lobby.players.size)
        assertTrue(lobby.players[0].isHost)
        assertEquals(hostName, lobby.players[0].name)

        // Player joins the lobby
        val guestName = "Wolfie's Friend"
        val joinResult = LobbySessionManager.joinLobby(code, guestName)

        assertTrue("Join should be successful", joinResult is JoinResult.Success)
        val successResult = joinResult as JoinResult.Success
        println("Player '${successResult.assignedUsername}' joined lobby $code")
        assertEquals(2, successResult.lobby.players.size)

        // Host updates game settings
        LobbySessionManager.updateSettings(code, theme = "Wolfie's fav thing", roundsPerPlayer = 3)
        val updatedLobby = LobbySessionManager.getLobby(code)

        assertNotNull(updatedLobby)
        assertEquals("Wolfie's fav thing", updatedLobby?.theme)
        assertEquals(3, updatedLobby?.roundsPerPlayer)
        println("Lobby settings updated successfully")

        // Guest leaves the lobby
        LobbySessionManager.leaveLobby(code, guestName)
        val lobbyAfterLeave = LobbySessionManager.getLobby(code)

        assertEquals(1, lobbyAfterLeave?.players?.size)
        println("Guest left remaining players: ${lobbyAfterLeave?.players?.size}")

        // Host disbands lobby
        LobbySessionManager.disbandLobby(code)
        assertNull(LobbySessionManager.getLobby(code))
        println("Lobby disbanded")
    }
}