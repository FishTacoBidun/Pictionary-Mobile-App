package com.example.pictionarymobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "landing") {
                        // Landing Screen
                        composable("landing") {
                            LandingScreen(
                                onHostClicked = { username ->
                                    val lobby = LobbySessionManager.createLobby(username)
                                    navController.navigate("lobby/${lobby.code}/$username")
                                },
                                onJoinClicked = { username, code ->
                                    navController.navigate("lobby/$code/$username")
                                }
                            )
                        }

                        // Lobby Screen
                        composable(
                            route = "lobby/{code}/{username}",
                            arguments = listOf(
                                navArgument("code") { type = NavType.StringType },
                                navArgument("username") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val code = backStackEntry.arguments?.getString("code") ?: ""
                            val username = backStackEntry.arguments?.getString("username") ?: "Player"

                            LobbyScreen(
                                code = code,
                                username = username,
                                onStartGame = {
                                    navController.navigate("game/$code/$username")
                                },
                                onLeaveOrDisband = {
                                    navController.navigate("landing") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Game Screen (lobby session route)
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
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onRoundEnd = {
                                    navController.navigate("roundTransition/$code/$username")
                                },
                                onGameOver = {
                                    navController.navigate("gameEnd/$code/$username")
                                }
                            )
                        }

                        // Direct Game Screen route (fallback route)
                        composable("game") {
                            GameScreen(
                                onLeave = {
                                    navController.navigate("landing") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onRoundEnd = {
                                    navController.navigate("roundTransition")
                                },
                                onGameOver = {
                                    navController.navigate("gameEnd")
                                }
                            )
                        }

                        // In-between Round Screen (lobby session route)
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

                            val currentRound = lobby?.currentRound ?: 1
                            val totalRounds = lobby?.totalRounds ?: 3
                            val players = lobby?.players ?: emptyList()

                            RoundTransitionScreen(
                                players = players,
                                currentRound = currentRound,
                                totalRounds = totalRounds,
                                onNextRound = {
                                    LobbySessionManager.advanceToNextRound(code)
                                    navController.navigate("game/$code/$username") {
                                        popUpTo("lobby/$code/$username") { inclusive = false }
                                    }
                                }
                            )
                        }

                        // Direct Round Transition route (fallback route)
                        composable("roundTransition") {
                            RoundTransitionScreen(
                                onNextRound = {
                                    navController.popBackStack("game", inclusive = false)
                                }
                            )
                        }

                        // Game End Screen (lobby session route)
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
                            val players = lobby?.players ?: emptyList()

                            GameEndScreen(
                                players = players,
                                onPlayAgain = {
                                    LobbySessionManager.resetGame(code)
                                    navController.navigate("lobby/$code/$username") {
                                        popUpTo("landing") { inclusive = false }
                                    }
                                },
                                onExitToLanding = {
                                    LobbySessionManager.leaveLobby(code, username)
                                    navController.navigate("landing") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Direct Game End route (fallback route)
                        composable("gameEnd") {
                            GameEndScreen(
                                onPlayAgain = {
                                    navController.navigate("landing") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onExitToLanding = {
                                    navController.navigate("landing") {
                                        popUpTo("landing") { inclusive = true }
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
