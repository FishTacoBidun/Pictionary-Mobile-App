package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.data.WordBankRepository
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

    // Lobby deletion/disbanding listener
    LaunchedEffect(lobby) {
        if (lobby != null) {
            hasBeenInLobby = true
        } else if (hasBeenInLobby) {
            onLeaveOrDisband()
        }
    }

    if (lobby == null && !hasBeenInLobby) {
        // Fallback UI if lobby wasn't found initially
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Lobby not found or has ended.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLeaveOrDisband) {
                Text("Return to Landing")
            }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isHost) "Lobby (Host)" else "Lobby",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Code: ${lobby.code}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Players (${lobby.players.size}/${lobby.maxPlayers})",
            style = MaterialTheme.typography.bodyLarge
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(lobby.players, key = { it.id }) { player ->
                val isCurrentPlayer = player.name.equals(username, ignoreCase = true)
                val badgeText = buildString {
                    if (player.isHost) append(" (Host)")
                    if (isCurrentPlayer) append(" (You)")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = if (isCurrentPlayer) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.cardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${player.name}$badgeText",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Settings (host interactive, non-host view only)
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

        // Rounds per player dropdown (1 - 10)
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

        // Time per round (in seconds with options 15, 30, 45, 60, 75, 90)
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

        // Start/Disband buttons
        if (isHost) {
            Button(
                onClick = onStartGame,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Start Game")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    LobbySessionManager.disbandLobby(lobby.code)
                    onLeaveOrDisband()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Disband Lobby")
            }
        } else {
            OutlinedButton(
                onClick = {
                    LobbySessionManager.leaveLobby(lobby.code, username)
                    onLeaveOrDisband()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Leave Lobby")
            }
        }
    }
}
