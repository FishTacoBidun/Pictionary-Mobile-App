package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.R
import com.example.pictionarymobileapp.data.JoinResult
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.data.WordBankRepository
import com.example.pictionarymobileapp.ui.theme.ThemeColors
import com.example.pictionarymobileapp.ui.utils.getAvatarDrawableResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onHostClicked: (username: String, avatarId: Int, theme: String, roundsPerPlayer: Int, timePerRoundSeconds: Int, availability: String) -> Unit,
    onJoinClicked: (username: String, code: String) -> Unit,
    onFindGameClicked: (username: String, avatarId: Int) -> Unit = { _, _ -> }
) {
    var username by remember { mutableStateOf("Username") }
    var selectedAvatarId by remember { mutableIntStateOf(1) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    // Join dialog states
    var joinCode by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinErrorMessage by remember { mutableStateOf<String?>(null) }
    var isJoining by remember { mutableStateOf(false) }

    // Host dialog states
    var showHostDialog by remember { mutableStateOf(false) }
    var hostTheme by remember { mutableStateOf("Animals") }
    var hostRounds by remember { mutableIntStateOf(3) }
    var hostTime by remember { mutableIntStateOf(45) }
    var hostAvailability by remember { mutableStateOf("private") }

    var themeExpanded by remember { mutableStateOf(false) }
    var roundsExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }

    val themeOptions = WordBankRepository.themes
    val roundsOptions = (1..10).toList()
    val timeOptions = listOf(15, 30, 45, 60, 75, 90)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.PageBackgroundOrange)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo image
        Image(
            painter = painterResource(id = R.drawable.dress_logo),
            contentDescription = "Pictionary Logo",
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth(1f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Profile avatar button
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, ThemeColors.PrimaryPurple, CircleShape)
                .clickable { showAvatarDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = getAvatarDrawableResId(selectedAvatarId)),
                contentDescription = "Avatar $selectedAvatarId",
                modifier = Modifier.size(68.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap avatar to change",
            style = MaterialTheme.typography.labelMedium,
            color = ThemeColors.TextWhite
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Username input
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", color = ThemeColors.TextWhite) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                errorTextColor = Color.Black,
                focusedLabelColor = ThemeColors.TextWhite,
                unfocusedLabelColor = ThemeColors.TextWhite,
                disabledLabelColor = ThemeColors.TextWhite,
                focusedBorderColor = ThemeColors.PrimaryPurple,
                unfocusedBorderColor = Color.LightGray
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Host game button
        Button(
            onClick = { showHostDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp)
        ) {
            Text("Host Game", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Join game button
        Button(
            onClick = {
                joinErrorMessage = null
                joinCode = ""
                showJoinDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp)
        ) {
            Text("Join Game", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Find game button
        Button(
            onClick = {
                val cleanUsername = username.ifBlank { "Username" }
                onFindGameClicked(cleanUsername, selectedAvatarId)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp)
        ) {
            Text("Find Game", color = ThemeColors.TextWhite, style = MaterialTheme.typography.titleMedium)
        }
    }

    // Host settings dialog
    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            containerColor = ThemeColors.PageBackgroundOrange,
            title = { Text("Host Game Settings", color = ThemeColors.TextWhite) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Theme dropdown
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = !themeExpanded }
                    ) {
                        OutlinedTextField(
                            value = hostTheme,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Game Theme") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = ThemeColors.TextWhite,
                                unfocusedLabelColor = ThemeColors.TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            themeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        hostTheme = option
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Rounds dropdown
                    ExposedDropdownMenuBox(
                        expanded = roundsExpanded,
                        onExpandedChange = { roundsExpanded = !roundsExpanded }
                    ) {
                        OutlinedTextField(
                            value = "$hostRounds",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rounds Per Player") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roundsExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = ThemeColors.TextWhite,
                                unfocusedLabelColor = ThemeColors.TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = roundsExpanded,
                            onDismissRequest = { roundsExpanded = false }
                        ) {
                            roundsOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("$option") },
                                    onClick = {
                                        hostRounds = option
                                        roundsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Time dropdown
                    ExposedDropdownMenuBox(
                        expanded = timeExpanded,
                        onExpandedChange = { timeExpanded = !timeExpanded }
                    ) {
                        OutlinedTextField(
                            value = "$hostTime seconds",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time Per Round (Seconds)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = ThemeColors.TextWhite,
                                unfocusedLabelColor = ThemeColors.TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = timeExpanded,
                            onDismissRequest = { timeExpanded = false }
                        ) {
                            timeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("$option seconds") },
                                    onClick = {
                                        hostTime = option
                                        timeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lobby Privacy", style = MaterialTheme.typography.bodyMedium, color = ThemeColors.TextWhite)

                    // Availability toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPrivate = hostAvailability == "private"
                        Button(
                            onClick = { hostAvailability = "private" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPrivate) ThemeColors.PrimaryPurple else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Private", color = if (isPrivate) ThemeColors.TextWhite else Color.Black)
                        }
                        Button(
                            onClick = { hostAvailability = "public" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isPrivate) ThemeColors.PrimaryPurple else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Public", color = if (!isPrivate) ThemeColors.TextWhite else Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        val cleanUsername = username.ifBlank { "Username" }
                        showHostDialog = false
                        onHostClicked(cleanUsername, selectedAvatarId, hostTheme, hostRounds, hostTime, hostAvailability)
                    }
                ) {
                    Text("Host", color = ThemeColors.TextWhite)
                }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showHostDialog = false }
                ) {
                    Text("Cancel", color = ThemeColors.TextWhite)
                }
            }
        )
    }

    // Avatar selection dialog
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            containerColor = ThemeColors.PageBackgroundOrange,
            title = { Text("Choose an Avatar", color = ThemeColors.TextWhite) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarRows = listOf(listOf(1, 2, 3, 4), listOf(5, 6, 7, 8))
                    avatarRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { id ->
                                val isSelected = id == selectedAvatarId
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) ThemeColors.PrimaryPurple.copy(alpha = 0.3f) else Color.White)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) ThemeColors.PrimaryPurple else ThemeColors.LightPurpleBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedAvatarId = id
                                            showAvatarDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = getAvatarDrawableResId(id)),
                                        contentDescription = "Avatar $id",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAvatarDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple)
                ) {
                    Text("Done", color = ThemeColors.TextWhite)
                }
            }
        )
    }

    // Join code dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { if (!isJoining) showJoinDialog = false },
            containerColor = ThemeColors.PageBackgroundOrange,
            title = { Text("Join Lobby", color = ThemeColors.TextWhite) },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = {
                            joinCode = it.uppercase()
                            joinErrorMessage = null
                        },
                        placeholder = { Text("Enter Lobby Code", color = Color.Gray) },
                        singleLine = true,
                        isError = joinErrorMessage != null,
                        enabled = !isJoining,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            errorTextColor = Color.Black,
                            focusedBorderColor = ThemeColors.PrimaryPurple,
                            unfocusedBorderColor = Color.LightGray,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp),
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
                Button(
                    enabled = !isJoining,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.PrimaryPurple),
                    onClick = {
                        val cleanUsername = username.ifBlank { "Player" }
                        isJoining = true
                        LobbySessionManager.joinLobby(joinCode, cleanUsername, selectedAvatarId) { result ->
                            isJoining = false
                            when (result) {
                                is JoinResult.Success -> {
                                    showJoinDialog = false
                                    onJoinClicked(result.assignedUsername, result.lobby.code)
                                }
                                is JoinResult.Error -> {
                                    joinErrorMessage = result.message
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isJoining) "Joining..." else "Join", color = ThemeColors.TextWhite)
                }
            },
            dismissButton = {
                Button(
                    enabled = !isJoining,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.LeaveRed),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showJoinDialog = false }
                ) {
                    Text("Cancel", color = ThemeColors.TextWhite)
                }
            }
        )
    }
}
