package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.data.Player

@Composable
fun GameEndScreen(
    players: List<Player> = emptyList(),
    onPlayAgain: () -> Unit,
    onExitToLanding: () -> Unit
) {
    val sortedPlayers = remember(players) {
        if (players.isNotEmpty()) {
            players.sortedByDescending { it.score }
        } else {
            listOf(
                Player(name = "Player 1", score = 12, isHost = true),
                Player(name = "Player 2", score = 8),
                Player(name = "Player 3", score = 5)
            )
        }
    }
    val winnerName = sortedPlayers.firstOrNull()?.name ?: "Player 1"
    val winnerScore = sortedPlayers.firstOrNull()?.score ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Over!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Winner: $winnerName ($winnerScore pts)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(sortedPlayers) { index, player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}. ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Avatar circle placeholder
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "${player.score} pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Play Again (Return to Lobby)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onExitToLanding,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Exit to Landing Page")
        }
    }
}
