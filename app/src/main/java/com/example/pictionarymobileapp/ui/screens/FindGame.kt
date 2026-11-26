package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.data.JoinResult
import com.example.pictionarymobileapp.data.Lobby
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.ui.theme.ThemeColors
import com.example.pictionarymobileapp.ui.utils.getAvatarDrawableResId

@Composable
fun FindGameScreen(
    username: String,
    avatarId: Int,
    onJoinSuccess: (code: String, assignedUsername: String) -> Unit,
    onReturnToLanding: () -> Unit
) {
    var publicLobbies by remember { mutableStateOf<List<Lobby>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    // Live subscription to all public waiting lobbies
    DisposableEffect(Unit) {
        val registration = LobbySessionManager.listenToPublicLobbies { list ->
            publicLobbies = list
        }
        onDispose {
            registration?.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.PageBackgroundOrange)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "All Public Lobbies",
            style = MaterialTheme.typography.headlineMedium,
            color = ThemeColors.TextWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (publicLobbies.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No public lobbies available right now.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeColors.TextWhite
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(publicLobbies, key = { it.code }) { lobby ->
                    val hostPlayer = lobby.players.find { it.isHost || it.id == lobby.hostUid }
                        ?: lobby.players.firstOrNull()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThemeColors.DarkOrangeHeader),
                        border = BorderStroke(1.5.dp, ThemeColors.LightPurpleBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // White circle host avatar container
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = getAvatarDrawableResId(hostPlayer?.avatarId ?: 1)),
                                        contentDescription = "Host Avatar",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${lobby.hostUsername}'s Lobby",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeColors.TextWhite
                                    )
                                    Text(
                                        text = "${lobby.players.size}/${lobby.maxPlayers} Players",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ThemeColors.TextWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val cleanUsername = username.ifBlank { "Player" }

                                    // Checks if lobby is full
                                    if (lobby.players.size >= lobby.maxPlayers) {
                                        errorMessage = "This lobby is full (${lobby.players.size}/${lobby.maxPlayers} players)."
                                        return@Button
                                    }

                                    // Username collision check
                                    if (lobby.players.any { it.name.equals(cleanUsername, ignoreCase = true) }) {
                                        errorMessage = "Username '$cleanUsername' is already taken in this lobby. Please choose a different username."
                                        return@Button
                                    }

                                    // Executes join
                                    isJoining = true
                                    LobbySessionManager.joinLobby(lobby.code, cleanUsername, avatarId) { result ->
                                        isJoining = false
                                        when (result) {
                                            is JoinResult.Success -> {
                                                onJoinSuccess(result.lobby.code, result.assignedUsername)
                                            }
                                            is JoinResult.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    }
                                },
                                enabled = !isJoining,
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Join", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Return to landing page button
        Button(
            onClick = onReturnToLanding,
            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Return to Landing Page", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
        }
    }

    // Error pop up dialog with close button
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = ThemeColors.PageBackgroundOrange,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unable to Join", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = { errorMessage = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Text(
                    text = errorMessage!!,
                    color = ThemeColors.TextWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", color = ThemeColors.TextWhite)
                }
            }
        )
    }
}
