package com.example.pictionarymobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pictionarymobileapp.data.JoinResult
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure user is authenticated anonymously with Firebase Auth on launch
        LobbySessionManager.ensureAuthenticated(onSuccess = {}, onError = {})

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "landing") {
                        // Landing screen
                        composable("landing") {
                            LandingScreen(
                                onHostClicked = { username, avatarId, theme, roundsPerPlayer, timePerRoundSeconds, availability ->
                                    LobbySessionManager.createLobby(
                                        hostUsername = username,
                                        avatarId = avatarId,
                                        theme = theme,
                                        roundsPerPlayer = roundsPerPlayer,
                                        timePerRoundSeconds = timePerRoundSeconds,
                                        availability = availability
                                    ) { result ->
                                        if (result is JoinResult.Success) {
                                            navController.navigate("lobby/${result.lobby.code}/${result.assignedUsername}")
                                        }
                                    }
                                },
                                onJoinClicked = { username, code ->
                                    navController.navigate("lobby/$code/$username")
                                },
                                onFindGameClicked = { username, avatarId ->
                                    navController.navigate("findGame/$username/$avatarId")
                                }
                            )
                        }

                        // Find game screen
                        composable(
                            route = "findGame/{username}/{avatarId}",
                            arguments = listOf(
                                navArgument("username") { type = NavType.StringType },
                                navArgument("avatarId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val username = backStackEntry.arguments?.getString("username") ?: "Player"
                            val avatarId = backStackEntry.arguments?.getInt("avatarId") ?: 1

                            FindGameScreen(
                                username = username,
                                avatarId = avatarId,
                                onJoinSuccess = { code, assignedUsername ->
                                    navController.navigate("lobby/$code/$assignedUsername")
                                },
                                onReturnToLanding = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Lobby screen
                        composable(
                            route = "lobby/{code}/{username}",
                            arguments = listOf(
                                navArgument("code") { type = NavType.StringType },
                                navArgument("username") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val code = backStackEntry.arguments?.getString("code") ?: ""
                            val username = backStackEntry.arguments?.getString("username") ?: "Player"

                            val allLobbies by LobbySessionManager.allLobbies.collectAsState()
                            val lobby = allLobbies[code]
                            var hasLoadedLobby by remember { mutableStateOf(false) }

                            // Transition players when status changes to playing or return to landing if disbanded
                            LaunchedEffect(lobby) {
                                if (lobby != null) {
                                    hasLoadedLobby = true
                                }

                                if (hasLoadedLobby && lobby == null) {
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else if (lobby != null && lobby.status == "playing") {
                                    navController.navigate("game/$code/$username")
                                }
                            }

                            LobbyScreen(
                                code = code,
                                username = username,
                                onStartGame = {
                                    LobbySessionManager.startGame(code)
                                },
                                onLeaveOrDisband = {
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Game screen lobby session route
                        composable(
                            route = "game/{code}/{username}",
                            arguments = listOf(
                                navArgument("code") { type = NavType.StringType },
                                navArgument("username") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val code = backStackEntry.arguments?.getString("code") ?: ""
                            val username = backStackEntry.arguments?.getString("username") ?: "Player"

                            val allLobbies by LobbySessionManager.allLobbies.collectAsState()
                            val lobby = allLobbies[code]
                            val currentUid = LobbySessionManager.getCurrentUid()
                            var hasLoadedLobby by remember { mutableStateOf(false) }

                            // Transition connected players when status changes or return to landing if disbanded
                            LaunchedEffect(lobby) {
                                if (lobby != null) {
                                    hasLoadedLobby = true
                                }

                                if (hasLoadedLobby && lobby == null) {
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else if (lobby != null) {
                                    val isStillInLobby = lobby.players.any { it.id == currentUid || it.name.equals(username, ignoreCase = true) }
                                    if (isStillInLobby) {
                                        when (lobby.status) {
                                            "round_transition" -> navController.navigate("roundTransition/$code/$username")
                                            "finished" -> navController.navigate("gameEnd/$code/$username")
                                        }
                                    }
                                }
                            }

                            val timePerRoundSeconds = lobby?.timePerRoundSeconds ?: 45
                            val currentRound = lobby?.currentRound ?: 1
                            val totalRounds = lobby?.totalRounds ?: 3
                            val theme = lobby?.theme ?: "Animals"
                            val players = lobby?.players ?: emptyList()

                            GameScreen(
                                code = code,
                                username = username,
                                players = players,
                                timePerRoundSeconds = timePerRoundSeconds,
                                currentRound = currentRound,
                                totalRounds = totalRounds,
                                theme = theme,
                                onLeave = {
                                    if (code.isNotBlank()) {
                                        LobbySessionManager.leaveLobby(code, username)
                                    }
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onRoundEnd = {
                                    LobbySessionManager.updateGameStatus(code, "round_transition")
                                },
                                onGameOver = {
                                    LobbySessionManager.updateGameStatus(code, "finished")
                                }
                            )
                        }

                        // In between round screen lobby session route
                        composable(
                            route = "roundTransition/{code}/{username}",
                            arguments = listOf(
                                navArgument("code") { type = NavType.StringType },
                                navArgument("username") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val code = backStackEntry.arguments?.getString("code") ?: ""
                            val username = backStackEntry.arguments?.getString("username") ?: "Player"

                            val allLobbies by LobbySessionManager.allLobbies.collectAsState()
                            val lobby = allLobbies[code]
                            val currentUid = LobbySessionManager.getCurrentUid()
                            var hasLoadedLobby by remember { mutableStateOf(false) }

                            // Automatically transition when status changes back to playing or return to landing if disbanded
                            LaunchedEffect(lobby) {
                                if (lobby != null) {
                                    hasLoadedLobby = true
                                }

                                if (hasLoadedLobby && lobby == null) {
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else if (lobby != null) {
                                    val isStillInLobby = lobby.players.any { it.id == currentUid || it.name.equals(username, ignoreCase = true) }
                                    if (isStillInLobby && lobby.status == "playing") {
                                        navController.navigate("game/$code/$username") {
                                            popUpTo("lobby/$code/$username") { inclusive = false }
                                        }
                                    }
                                }
                            }

                            val currentRound = lobby?.currentRound ?: 1
                            val totalRounds = lobby?.totalRounds ?: 3
                            val players = lobby?.players ?: emptyList()
                            val isHost = lobby?.hostUid == currentUid || (players.isNotEmpty() && players.first().id == currentUid)

                            RoundTransitionScreen(
                                players = players,
                                currentRound = currentRound,
                                totalRounds = totalRounds,
                                isHost = isHost,
                                localUsername = username,
                                onNextRound = {
                                    LobbySessionManager.advanceToNextRound(code)
                                }
                            )
                        }

                        // Game end screen lobby session route
                        composable(
                            route = "gameEnd/{code}/{username}",
                            arguments = listOf(
                                navArgument("code") { type = NavType.StringType },
                                navArgument("username") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val code = backStackEntry.arguments?.getString("code") ?: ""
                            val username = backStackEntry.arguments?.getString("username") ?: "Player"

                            val allLobbies by LobbySessionManager.allLobbies.collectAsState()
                            val lobby = allLobbies[code]
                            val currentUid = LobbySessionManager.getCurrentUid()
                            var hasLoadedLobby by remember { mutableStateOf(false) }

                            // Transition back to lobby when status changes to waiting or return to landing if host exited
                            LaunchedEffect(lobby) {
                                if (lobby != null) {
                                    hasLoadedLobby = true
                                }

                                if (hasLoadedLobby && lobby == null) {
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else if (lobby != null) {
                                    val isStillInLobby = lobby.players.any { it.id == currentUid || it.name.equals(username, ignoreCase = true) }
                                    if (isStillInLobby && lobby.status == "waiting") {
                                        navController.navigate("lobby/$code/$username") {
                                            popUpTo("landing") { inclusive = false }
                                        }
                                    }
                                }
                            }

                            val players = lobby?.players ?: emptyList()
                            val me = lobby?.players?.find { it.id == currentUid || it.name.equals(username, ignoreCase = true) }
                            val isHost = me?.isHost == true || (lobby?.hostUid?.isNotBlank() == true && lobby.hostUid == currentUid) || (lobby?.hostUsername?.equals(username, ignoreCase = true) == true)

                            GameEndScreen(
                                players = players,
                                isHost = isHost,
                                localUsername = username,
                                onPlayAgain = {
                                    LobbySessionManager.resetGame(code)
                                },
                                onExitToLanding = {
                                    LobbySessionManager.leaveLobby(code, username)
                                    navController.navigate("landing") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
