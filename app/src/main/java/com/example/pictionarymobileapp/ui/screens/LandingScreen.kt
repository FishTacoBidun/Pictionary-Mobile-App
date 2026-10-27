package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.data.JoinResult
import com.example.pictionarymobileapp.data.LobbySessionManager

@Composable
fun LandingScreen(
    onHostClicked: (String) -> Unit,
    onJoinClicked: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("Username") }
    var joinCode by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinErrorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pictionary", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        // Profile picture placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Avatar", color = Color.DarkGray)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Username input
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Host game button
        Button(
            onClick = {
                val cleanUsername = username.ifBlank { "Username" }
                onHostClicked(cleanUsername)
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Host Game")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Join game button
        OutlinedButton(
            onClick = {
                joinErrorMessage = null
                joinCode = ""
                showJoinDialog = true
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Join Game")
        }
    }

    // Join code dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Lobby") },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = {
                            joinCode = it.uppercase()
                            joinErrorMessage = null
                        },
                        label = { Text("Enter Lobby Code") },
                        singleLine = true,
                        isError = joinErrorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    joinErrorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanUsername = username.ifBlank { "Player" }
                        when (val result = LobbySessionManager.joinLobby(joinCode, cleanUsername)) {
                            is JoinResult.Success -> {
                                showJoinDialog = false
                                onJoinClicked(result.assignedUsername, result.lobby.code)
                            }
                            is JoinResult.Error -> {
                                joinErrorMessage = result.message
                            }
                        }
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
