package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GameEndScreen(
    onPlayAgain: () -> Unit,
    onExitToLanding: () -> Unit
) {
    val finalScores = listOf("Player 2: 350 pts", "Player 1: 320 pts", "Player 3: 210 pts")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Over!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Winner: Player 2 🎉", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(finalScores) { index, score ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "${index + 1}. $score", modifier = Modifier.padding(16.dp))
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