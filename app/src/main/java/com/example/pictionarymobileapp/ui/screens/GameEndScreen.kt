package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.data.Player
import com.example.pictionarymobileapp.ui.theme.ThemeColors
import com.example.pictionarymobileapp.ui.utils.getAvatarDrawableResId

@Composable
fun GameEndScreen(
    players: List<Player> = emptyList(),
    isHost: Boolean = true,
    localUsername: String = "",
    onPlayAgain: () -> Unit,
    onExitToLanding: () -> Unit
) {
    val sortedPlayers = remember(players) {
        players.sortedByDescending { it.score }
    }
    val winnerName = sortedPlayers.firstOrNull()?.name ?: "No Winner"
    val winnerScore = sortedPlayers.firstOrNull()?.score ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.PageBackgroundOrange)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Over!", style = MaterialTheme.typography.headlineLarge, color = ThemeColors.TextWhite)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Winner: $winnerName ($winnerScore pts)",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Yellow,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(sortedPlayers) { index, player ->
                val isLocalPlayer = player.name.equals(localUsername, ignoreCase = true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                    border = BorderStroke(1.5.dp, ThemeColors.LightPurpleBorder)
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
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.TextWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Avatar circle container
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = getAvatarDrawableResId(player.avatarId)),
                                    contentDescription = "Avatar ${player.avatarId}",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isLocalPlayer) FontWeight.Bold else FontWeight.Normal,
                                color = ThemeColors.TextWhite
                            )
                        }

                        Text(
                            text = "${player.score} pts",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Yellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isHost) {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Play Again (Return to Lobby)", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onExitToLanding,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Exit to Landing Page", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(
                text = "Waiting for host to play again...",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColors.TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onExitToLanding,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Leave Game", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
