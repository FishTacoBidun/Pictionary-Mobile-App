package com.example.pictionarymobileapp

import com.example.pictionarymobileapp.data.JoinResult
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.data.WordBankRepository
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

        // Total rounds test: 3 rounds per player * 2 players = 6 total rounds
        LobbySessionManager.updateSettings(code, theme = "Locations", roundsPerPlayer = 3, timePerRoundSeconds = 60)
        val updatedLobby = LobbySessionManager.getLobby(code)

        assertNotNull(updatedLobby)
        assertEquals("Locations", updatedLobby?.theme)
        assertEquals(3, updatedLobby?.roundsPerPlayer)
        assertEquals(60, updatedLobby?.timePerRoundSeconds)
        assertEquals(6, updatedLobby?.totalRounds)
        println("Lobby settings updated successfully: totalRounds = ${updatedLobby?.totalRounds}")

        // Test round advancement
        LobbySessionManager.advanceToNextRound(code)
        val round2Lobby = LobbySessionManager.getLobby(code)
        assertEquals(2, round2Lobby?.currentRound)

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

    @Test
    fun testWordBankRepository() {
        assertTrue(WordBankRepository.themes.contains("Animals"))
        assertTrue(WordBankRepository.themes.contains("Locations"))

        val animalChoices = WordBankRepository.getRandomWordChoices("Animals", 3)
        assertEquals(3, animalChoices.size)

        val locationChoices = WordBankRepository.getRandomWordChoices("Locations", 3)
        assertEquals(3, locationChoices.size)

        assertTrue(WordBankRepository.isCorrectGuess("Elephant", "elephant"))
        assertTrue(WordBankRepository.isCorrectGuess("Airport", " AIRPORT "))
        assertFalse(WordBankRepository.isCorrectGuess("Elephant", "Giraffe"))
    }

    @Test
    fun testAwardingPointsAndCumulativeScoring() {
        val lobby = LobbySessionManager.createLobby("DrawerAlice")
        LobbySessionManager.joinLobby(lobby.code, "GuesserBob")

        // Award GuesserBob 3 points for quick guess (>2/3 time left), DrawerAlice gets 1 point
        LobbySessionManager.awardPoints(
            code = lobby.code,
            guesserName = "GuesserBob",
            drawerName = "DrawerAlice",
            guesserPoints = 3
        )

        val currentLobby = LobbySessionManager.getLobby(lobby.code)
        assertNotNull(currentLobby)

        val alice = currentLobby?.players?.find { it.name == "DrawerAlice" }
        val bob = currentLobby?.players?.find { it.name == "GuesserBob" }

        assertEquals(1, alice?.score)
        assertEquals(3, bob?.score)

        // Test resetGame resets scores back to 0
        LobbySessionManager.resetGame(lobby.code)
        val resetLobby = LobbySessionManager.getLobby(lobby.code)
        assertEquals(0, resetLobby?.players?.find { it.name == "DrawerAlice" }?.score)
        assertEquals(0, resetLobby?.players?.find { it.name == "GuesserBob" }?.score)
    }
}
