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
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.data.WordBankRepository
import com.example.pictionarymobileapp.ui.theme.ThemeColors
import com.example.pictionarymobileapp.ui.utils.getAvatarDrawableResId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    code: String,
    username: String,
    onStartGame: () -> Unit,
    onLeaveOrDisband: () -> Unit
) {
    val allLobbies by LobbySessionManager.allLobbies.collectAsState()
    val formattedCode = remember(code) { code.trim().uppercase(Locale.ROOT) }
    val lobby = allLobbies[formattedCode]

    var hasBeenInLobby by remember { mutableStateOf(false) }

    // Lobby deletion or disbanding listener
    LaunchedEffect(lobby) {
        if (lobby != null) {
            hasBeenInLobby = true
        } else if (hasBeenInLobby) {
            onLeaveOrDisband()
        }
    }

    if (lobby == null && !hasBeenInLobby) {
        // Loading indicator while initial Firestore snapshot arrives
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.PageBackgroundOrange),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ThemeColors.TextWhite)
        }
        return
    }

    if (lobby == null) {
        return
    }

    val currentUser = lobby.players.find { it.name.equals(username, ignoreCase = true) }
    val isHost = currentUser?.isHost ?: (lobby.hostUsername.equals(username, ignoreCase = true))

    // Dropdown expansion states
    var themeExpanded by remember { mutableStateOf(false) }
    var roundsExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }

    val themeOptions = WordBankRepository.themes
    val roundsOptions = (1..10).toList()
    val timeOptions = listOf(15, 30, 45, 60, 75, 90)

    val canStartGame = isHost && lobby.players.size >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.PageBackgroundOrange)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lobby",
            style = MaterialTheme.typography.headlineMedium,
            color = ThemeColors.TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Code: ${lobby.code}",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeColors.TextWhite
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Status: ${lobby.availability.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeColors.TextWhite
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Players (${lobby.players.size}/${lobby.maxPlayers})",
            style = MaterialTheme.typography.bodyLarge,
            color = ThemeColors.TextWhite
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(lobby.players, key = { it.id }) { player ->
                val isCurrentPlayer = player.name.equals(username, ignoreCase = true)
                val badgeText = if (player.isHost) " (Host)" else ""

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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White circle avatar container
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = getAvatarDrawableResId(player.avatarId)),
                                contentDescription = "Avatar ${player.avatarId}",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${player.name}$badgeText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                            color = ThemeColors.TextWhite
                        )
                    }
                }
            }
        }

        // Settings for host or view only for non host
        // Game theme dropdown
        ExposedDropdownMenuBox(
            expanded = themeExpanded,
            onExpandedChange = { if (isHost) themeExpanded = !themeExpanded }
        ) {
            OutlinedTextField(
                value = lobby.theme,
                onValueChange = {},
                readOnly = true,
                enabled = isHost,
                label = { Text("Game Theme") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.DarkGray,
                    focusedLabelColor = ThemeColors.TextWhite,
                    unfocusedLabelColor = ThemeColors.TextWhite,
                    disabledLabelColor = ThemeColors.TextWhite
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = themeExpanded && isHost,
                onDismissRequest = { themeExpanded = false }
            ) {
                themeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            themeExpanded = false
                            LobbySessionManager.updateSettings(
                                code = lobby.code,
                                theme = option,
                                roundsPerPlayer = lobby.roundsPerPlayer,
                                timePerRoundSeconds = lobby.timePerRoundSeconds
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Rounds per player dropdown
        ExposedDropdownMenuBox(
            expanded = roundsExpanded,
            onExpandedChange = { if (isHost) roundsExpanded = !roundsExpanded }
        ) {
            OutlinedTextField(
                value = "${lobby.roundsPerPlayer}",
                onValueChange = {},
                readOnly = true,
                enabled = isHost,
                label = { Text("Rounds Per Player") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roundsExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.DarkGray,
                    focusedLabelColor = ThemeColors.TextWhite,
                    unfocusedLabelColor = ThemeColors.TextWhite,
                    disabledLabelColor = ThemeColors.TextWhite
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = roundsExpanded && isHost,
                onDismissRequest = { roundsExpanded = false }
            ) {
                roundsOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option") },
                        onClick = {
                            roundsExpanded = false
                            LobbySessionManager.updateSettings(
                                code = lobby.code,
                                theme = lobby.theme,
                                roundsPerPlayer = option,
                                timePerRoundSeconds = lobby.timePerRoundSeconds
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Time per round dropdown
        ExposedDropdownMenuBox(
            expanded = timeExpanded,
            onExpandedChange = { if (isHost) timeExpanded = !timeExpanded }
        ) {
            OutlinedTextField(
                value = "${lobby.timePerRoundSeconds} seconds",
                onValueChange = {},
                readOnly = true,
                enabled = isHost,
                label = { Text("Time Per Round (Seconds)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.DarkGray,
                    focusedLabelColor = ThemeColors.TextWhite,
                    unfocusedLabelColor = ThemeColors.TextWhite,
                    disabledLabelColor = ThemeColors.TextWhite
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = timeExpanded && isHost,
                onDismissRequest = { timeExpanded = false }
            ) {
                timeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option seconds") },
                        onClick = {
                            timeExpanded = false
                            LobbySessionManager.updateSettings(
                                code = lobby.code,
                                theme = lobby.theme,
                                roundsPerPlayer = lobby.roundsPerPlayer,
                                timePerRoundSeconds = option
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Start or disband buttons
        if (isHost) {
            if (lobby.players.size < 2) {
                Text(
                    text = "Need at least 2 players to start game",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColors.TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Button(
                onClick = {
                    LobbySessionManager.startGame(lobby.code)
                    onStartGame()
                },
                enabled = canStartGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeColors.PrimaryPurple,
                    disabledContainerColor = ThemeColors.PrimaryPurple.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Start Game", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    LobbySessionManager.disbandLobby(lobby.code)
                    onLeaveOrDisband()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Disband Lobby", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = {
                    LobbySessionManager.leaveLobby(lobby.code, username)
                    onLeaveOrDisband()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Leave Lobby", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
