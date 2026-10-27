package com.example.pictionarymobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                        // Landing s
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

                            LobbyScreen(
                                code = code,
                                username = username,
                                onStartGame = { navController.navigate("game") },
                                onLeaveOrDisband = {
                                    navController.navigate("landing") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Game screen
                        composable("game") {
                            GameScreen(
                                onRoundEnd = {
                                    navController.navigate("roundTransition")
                                }
                            )
                        }

                        // In-between round screen
                        composable("roundTransition") {
                            RoundTransitionScreen(
                                onNextRound = {
                                    navController.popBackStack("game", inclusive = false)
                                }
                            )
                        }

                        // Game end screen
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
