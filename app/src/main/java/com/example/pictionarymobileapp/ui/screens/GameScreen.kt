package com.example.pictionarymobileapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pictionarymobileapp.data.LobbySessionManager
import com.example.pictionarymobileapp.data.Player
import com.example.pictionarymobileapp.data.RealtimeDatabaseApi
import com.example.pictionarymobileapp.data.SerializablePoint
import com.example.pictionarymobileapp.data.SerializableStroke
import com.example.pictionarymobileapp.data.WordBankRepository
import com.example.pictionarymobileapp.data.toArgbInt
import com.example.pictionarymobileapp.data.toComposeColor
import kotlinx.coroutines.delay
import java.util.Locale

// Represents a single continuous stroke drawn on the canvas
data class StrokePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
) {
    fun toSerializableStroke(): SerializableStroke = SerializableStroke(
        points = points.map { SerializablePoint.fromOffset(it) },
        colorArgb = color.toArgbInt(),
        strokeWidth = strokeWidth,
        isEraser = isEraser
    )
}

// Represents an action performed on the canvas that can be undone
sealed interface CanvasAction {
    data class DrawStroke(val stroke: StrokePath) : CanvasAction
    data class FillCanvas(
        val previousBackgroundColor: Color,
        val previousStrokes: List<StrokePath>,
        val newBackgroundColor: Color
    ) : CanvasAction
    data class StartOverCanvas(
        val previousBackgroundColor: Color,
        val previousStrokes: List<StrokePath>
    ) : CanvasAction
}

@Composable
fun GameScreen(
    code: String = "",
    username: String = "Player",
    players: List<Player> = emptyList(),
    timePerRoundSeconds: Int = 45,
    currentRound: Int = 1,
    totalRounds: Int = 3,
    theme: String = "Animals",
    onLeave: () -> Unit = {},
    onRoundEnd: () -> Unit = {},
    onGameOver: () -> Unit = {}
) {
    // Session key for Realtime Database stream
    val sessionCode = remember(code) { code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" } }
    val allCanvasData by RealtimeDatabaseApi.allCanvasData.collectAsState()
    val remoteCanvasData = allCanvasData[sessionCode]

    // View mode toggle for testing drawer vs guesser views
    var isDrawer by remember { mutableStateOf(true) }
    var guessInput by remember { mutableStateOf("") }

    // Word selection state (3 choices from theme word bank)
    val wordChoices = remember(theme) {
        WordBankRepository.getRandomWordChoices(theme, 3)
    }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var isSelectingWord by remember { mutableStateOf(true) }
    var selectionTimeLeft by remember { mutableIntStateOf(10) }

    // 10-second word prompt selection timer
    LaunchedEffect(isSelectingWord, selectionTimeLeft) {
        if (isSelectingWord) {
            if (selectionTimeLeft > 0) {
                delay(1000L)
                selectionTimeLeft -= 1
            } else {
                if (selectedWord == null && wordChoices.isNotEmpty()) {
                    selectedWord = wordChoices.first()
                }
                isSelectingWord = false
            }
        }
    }

    // Guesser active guess map (3-sec timeout) and correct guessers set
    val activeGuesses = remember { mutableStateMapOf<String, String>() }
    val correctGuessers = remember { mutableStateSetOf<String>() }

    // Countdown timer state
    var timeLeft by remember(timePerRoundSeconds) { mutableIntStateOf(timePerRoundSeconds) }

    // Format timer as M:SS
    val formattedTime = remember(timeLeft) {
        val mins = timeLeft / 60
        val secs = timeLeft % 60
        String.format(Locale.ROOT, "%d:%02d", mins, secs)
    }

    // Active round timer loop (runs once word is selected)
    LaunchedEffect(timeLeft, isSelectingWord) {
        if (!isSelectingWord) {
            if (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            } else {
                if (currentRound >= totalRounds) {
                    onGameOver()
                } else {
                    onRoundEnd()
                }
            }
        }
    }

    // Interactive Canvas State
    val strokes = remember { mutableStateListOf<StrokePath>() }
    val actionHistory = remember { mutableStateListOf<CanvasAction>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var strokeWidth by remember { mutableFloatStateOf(12f) }

    // Color State
    var baseWheelColor by remember { mutableStateOf(Color.Red) }
    var brightnessValue by remember { mutableFloatStateOf(0.5f) } // 0.0 = Black, 0.5 = Base Color, 1.0 = White
    val selectedColor = remember(baseWheelColor, brightnessValue) {
        computeColor(baseWheelColor, brightnessValue)
    }

    var canvasBackgroundColor by remember { mutableStateOf(Color.White) }
    var isEraser by remember { mutableStateOf(false) }
    var isEyeDropper by remember { mutableStateOf(false) }

    // Reset local canvas state & Realtime Database stream whenever a new round starts
    LaunchedEffect(currentRound) {
        strokes.clear()
        actionHistory.clear()
        currentPoints = emptyList()
        canvasBackgroundColor = Color.White
        isEraser = false
        isEyeDropper = false
        RealtimeDatabaseApi.clearCanvas(sessionCode)
        RealtimeDatabaseApi.updateDrawerStrokes(
            code = sessionCode,
            strokes = emptyList(),
            backgroundColorArgb = Color.White.toArgbInt()
        )
    }

    // Helper to sync drawer canvas changes to Realtime Database API
    fun syncDrawerStateToRealtimeDatabase() {
        RealtimeDatabaseApi.updateDrawerStrokes(
            code = sessionCode,
            strokes = strokes.map { it.toSerializableStroke() },
            backgroundColorArgb = canvasBackgroundColor.toArgbInt()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar info (theme, timer, and LEAVE button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Theme: $theme", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (isSelectingWord) "Selecting word ($selectionTimeLeft s)" else "Time: $formattedTime",
                style = MaterialTheme.typography.bodyMedium,
                color = if (timeLeft <= 5 && !isSelectingWord) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onLeave,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("LEAVE", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Players display area box
        PlayersDisplayArea(
            players = players,
            localUsername = username,
            isLocalDrawer = isDrawer,
            activeGuesses = activeGuesses,
            correctGuessers = correctGuessers
        )
        Spacer(modifier = Modifier.height(8.dp))

        // View toggle switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = isDrawer, onCheckedChange = { isDrawer = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDrawer) "View: Drawer" else "View: Guesser",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Interactive drawing canvas (synced live via Realtime Database API)
        val activeCanvasBackground = if (isDrawer) canvasBackgroundColor else (remoteCanvasData?.backgroundColorArgb?.toComposeColor() ?: Color.White)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(
                    width = if (isEyeDropper) 3.dp else 2.dp,
                    color = if (isEyeDropper) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(activeCanvasBackground)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isDrawer, selectedColor, strokeWidth, isEraser, isEyeDropper, canvasBackgroundColor) {
                        if (!isDrawer) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()

                            if (isEyeDropper) {
                                // Eye Dropper mode
                                val picked = pickColorAtPoint(
                                    point = down.position,
                                    strokes = strokes,
                                    backgroundColor = canvasBackgroundColor
                                )
                                baseWheelColor = picked
                                brightnessValue = 0.5f
                                isEyeDropper = false
                            } else {
                                // Drawing mode, stream points live to Realtime Database
                                val points = mutableListOf(down.position)
                                currentPoints = points.toList()

                                val strokeColor = if (isEraser) canvasBackgroundColor else selectedColor
                                RealtimeDatabaseApi.updateActiveStroke(
                                    code = sessionCode,
                                    points = points.map { SerializablePoint.fromOffset(it) },
                                    colorArgb = strokeColor.toArgbInt(),
                                    strokeWidth = strokeWidth,
                                    isEraser = isEraser
                                )

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    change.consume()
                                    points.add(change.position)
                                    currentPoints = points.toList()

                                    // Realtime streaming stroke update
                                    RealtimeDatabaseApi.updateActiveStroke(
                                        code = sessionCode,
                                        points = points.map { SerializablePoint.fromOffset(it) },
                                        colorArgb = strokeColor.toArgbInt(),
                                        strokeWidth = strokeWidth,
                                        isEraser = isEraser
                                    )
                                }

                                if (points.isNotEmpty()) {
                                    val newStroke = StrokePath(
                                        points = points.toList(),
                                        color = strokeColor,
                                        strokeWidth = strokeWidth,
                                        isEraser = isEraser
                                    )
                                    strokes.add(newStroke)
                                    actionHistory.add(CanvasAction.DrawStroke(newStroke))
                                    syncDrawerStateToRealtimeDatabase()
                                }
                                currentPoints = emptyList()
                            }
                        }
                    }
            ) {
                // Background fill
                drawRect(activeCanvasBackground)

                fun renderStroke(
                    pts: List<Offset>,
                    drawColor: Color,
                    width: Float,
                    eraser: Boolean
                ) {
                    if (pts.isEmpty()) return
                    val finalColor = if (eraser) activeCanvasBackground else drawColor

                    if (pts.size == 1) {
                        drawCircle(
                            color = finalColor,
                            radius = width / 2f,
                            center = pts.first()
                        )
                    } else {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                lineTo(pts[i].x, pts[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = finalColor,
                            style = Stroke(
                                width = width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (isDrawer) {
                    // Drawer view, render local strokes and active drawing stroke
                    for (stroke in strokes) {
                        renderStroke(
                            pts = stroke.points,
                            drawColor = stroke.color,
                            width = stroke.strokeWidth,
                            eraser = stroke.isEraser
                        )
                    }
                    if (currentPoints.isNotEmpty()) {
                        renderStroke(
                            pts = currentPoints,
                            drawColor = if (isEraser) canvasBackgroundColor else selectedColor,
                            width = strokeWidth,
                            eraser = isEraser
                        )
                    }
                } else {
                    // Guesser view, stream live from Realtime Database API
                    val remoteStrokes = remoteCanvasData?.strokes ?: emptyList()
                    for (stroke in remoteStrokes) {
                        renderStroke(
                            pts = stroke.points.map { it.toOffset() },
                            drawColor = stroke.colorArgb.toComposeColor(),
                            width = stroke.strokeWidth,
                            eraser = stroke.isEraser
                        )
                    }
                    val activePts = remoteCanvasData?.activeStrokePoints
                    if (!activePts.isNullOrEmpty()) {
                        renderStroke(
                            pts = activePts.map { it.toOffset() },
                            drawColor = (remoteCanvasData?.activeStrokeColorArgb ?: 0xFF000000.toInt()).toComposeColor(),
                            width = remoteCanvasData?.activeStrokeWidth ?: 12f,
                            eraser = remoteCanvasData?.activeStrokeIsEraser ?: false
                        )
                    }
                }
            }

            if (isEyeDropper) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Click anywhere on canvas to pick color",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Drawer controls/guesser view
        if (isDrawer) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top row (stroke Size, color wheel + hue slider, and prompt/word to draw)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stroke size vertical bar
                    VerticalStrokeSlider(
                        strokeWidth = strokeWidth,
                        onStrokeWidthChange = { strokeWidth = it }
                    )

                    // Color wheel with hue slider and current color display
                    RgbColorPickerWithSlider(
                        baseColor = baseWheelColor,
                        selectedColor = if (isEraser) canvasBackgroundColor else selectedColor,
                        brightnessValue = brightnessValue,
                        onBaseColorSelected = { color ->
                            baseWheelColor = color
                            isEraser = false
                            isEyeDropper = false
                        },
                        onBrightnessChanged = { value ->
                            brightnessValue = value
                            isEraser = false
                            isEyeDropper = false
                        }
                    )

                    // Word to draw
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Word to draw:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedWord?.uppercase(Locale.ROOT) ?: "CHOOSING...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom tools row (eraser, fill, eye dropper, undo, start over)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Eraser
                    Button(
                        onClick = {
                            isEraser = !isEraser
                            isEyeDropper = false
                        },
                        colors = if (isEraser) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        border = if (!isEraser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isEraser) "Eraser ON" else "Eraser",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Fill
                    OutlinedButton(
                        onClick = {
                            actionHistory.add(
                                CanvasAction.FillCanvas(
                                    previousBackgroundColor = canvasBackgroundColor,
                                    previousStrokes = strokes.toList(),
                                    newBackgroundColor = selectedColor
                                )
                            )
                            canvasBackgroundColor = selectedColor
                            strokes.clear()
                            isEraser = false
                            isEyeDropper = false
                            syncDrawerStateToRealtimeDatabase()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Fill", style = MaterialTheme.typography.labelSmall)
                    }

                    // Eye Dropper
                    Button(
                        onClick = {
                            isEyeDropper = !isEyeDropper
                            isEraser = false
                        },
                        colors = if (isEyeDropper) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        border = if (!isEyeDropper) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isEyeDropper) "Picker ON" else "Eye Dropper",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Undo
                    OutlinedButton(
                        onClick = {
                            if (actionHistory.isNotEmpty()) {
                                when (val lastAction = actionHistory.removeAt(actionHistory.lastIndex)) {
                                    is CanvasAction.DrawStroke -> {
                                        if (strokes.isNotEmpty()) {
                                            strokes.removeAt(strokes.lastIndex)
                                        }
                                    }
                                    is CanvasAction.FillCanvas -> {
                                        canvasBackgroundColor = lastAction.previousBackgroundColor
                                        strokes.clear()
                                        strokes.addAll(lastAction.previousStrokes)
                                    }
                                    is CanvasAction.StartOverCanvas -> {
                                        canvasBackgroundColor = lastAction.previousBackgroundColor
                                        strokes.clear()
                                        strokes.addAll(lastAction.previousStrokes)
                                    }
                                }
                                syncDrawerStateToRealtimeDatabase()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Undo", style = MaterialTheme.typography.labelSmall)
                    }

                    // Start Over
                    OutlinedButton(
                        onClick = {
                            actionHistory.add(
                                CanvasAction.StartOverCanvas(
                                    previousBackgroundColor = canvasBackgroundColor,
                                    previousStrokes = strokes.toList()
                                )
                            )
                            strokes.clear()
                            canvasBackgroundColor = Color.White
                            isEraser = false
                            isEyeDropper = false
                            syncDrawerStateToRealtimeDatabase()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Start Over", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = guessInput,
                    onValueChange = { guessInput = it },
                    label = { Text("Type your guess...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (guessInput.isNotBlank()) {
                            val trimmedGuess = guessInput.trim()
                            if (WordBankRepository.isCorrectGuess(selectedWord, trimmedGuess)) {
                                if (!correctGuessers.contains(username)) {
                                    correctGuessers.add(username)

                                    // Calculate time fraction and points
                                    val timeFraction = timeLeft.toFloat() / timePerRoundSeconds.toFloat()
                                    val guesserPoints = when {
                                        timeFraction > (2f / 3f) -> 3
                                        timeFraction > (1f / 3f) -> 2
                                        else -> 1
                                    }

                                    // Determine current drawer name
                                    val drawerName = if (players.isNotEmpty()) {
                                        val drawerIndex = (currentRound - 1) % players.size
                                        players.getOrNull(drawerIndex)?.name ?: players.first().name
                                    } else {
                                        "Drawer"
                                    }

                                    if (code.isNotBlank()) {
                                        LobbySessionManager.awardPoints(
                                            code = code,
                                            guesserName = username,
                                            drawerName = drawerName,
                                            guesserPoints = guesserPoints
                                        )
                                    }
                                }
                            } else {
                                activeGuesses[username] = trimmedGuess
                            }
                            guessInput = ""
                        }
                    }
                ) {
                    Text("Guess")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulate Round End Button
        Button(
            onClick = {
                timeLeft = 1
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate Round End")
        }
    }

    // 10-second word prompt selection dialog for drawer
    if (isSelectingWord && isDrawer) {
        AlertDialog(
            onDismissRequest = { /* Require choosing or waiting for timeout */ },
            title = {
                Text(
                    text = "Choose a Word to Draw ($selectionTimeLeft s)",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pick a word from the '$theme' theme:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    wordChoices.forEach { word ->
                        Button(
                            onClick = {
                                selectedWord = word
                                isSelectingWord = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(word, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// Displays current round players in up to 2 rows of 4, centered vertically and horizontally
@Composable
fun PlayersDisplayArea(
    players: List<Player>,
    localUsername: String,
    isLocalDrawer: Boolean,
    activeGuesses: MutableMap<String, String>,
    correctGuessers: Set<String>,
    modifier: Modifier = Modifier
) {
    val totalPlayers = remember(players, localUsername) {
        if (players.isNotEmpty()) players else listOf(Player(name = localUsername, isHost = true))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (totalPlayers.size <= 4) {
                // Single row, centered
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    totalPlayers.forEachIndexed { index, player ->
                        if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                        PlayerItem(
                            player = player,
                            isLocalPlayer = player.name.equals(localUsername, ignoreCase = true),
                            isDrawer = if (player.name.equals(localUsername, ignoreCase = true)) isLocalDrawer else (index == 0 && !isLocalDrawer),
                            activeGuess = activeGuesses[player.name],
                            hasGuessedCorrectly = correctGuessers.contains(player.name),
                            onGuessExpire = { activeGuesses.remove(player.name) }
                        )
                    }
                }
            } else {
                // Two rows, centered
                val row1 = totalPlayers.take(4)
                val row2 = totalPlayers.drop(4)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row1.forEachIndexed { index, player ->
                            if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                            PlayerItem(
                                player = player,
                                isLocalPlayer = player.name.equals(localUsername, ignoreCase = true),
                                isDrawer = if (player.name.equals(localUsername, ignoreCase = true)) isLocalDrawer else false,
                                activeGuess = activeGuesses[player.name],
                                hasGuessedCorrectly = correctGuessers.contains(player.name),
                                onGuessExpire = { activeGuesses.remove(player.name) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row2.forEachIndexed { index, player ->
                            if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                            PlayerItem(
                                player = player,
                                isLocalPlayer = player.name.equals(localUsername, ignoreCase = true),
                                isDrawer = if (player.name.equals(localUsername, ignoreCase = true)) isLocalDrawer else false,
                                activeGuess = activeGuesses[player.name],
                                hasGuessedCorrectly = correctGuessers.contains(player.name),
                                onGuessExpire = { activeGuesses.remove(player.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Individual player display (avatar circle, username, and drawer tag/3-sec guess box)
@Composable
fun PlayerItem(
    player: Player,
    isLocalPlayer: Boolean,
    isDrawer: Boolean,
    activeGuess: String?,
    hasGuessedCorrectly: Boolean,
    onGuessExpire: () -> Unit
) {
    // 3-second guess expiration timer
    LaunchedEffect(activeGuess) {
        if (!activeGuess.isNullOrEmpty() && !hasGuessedCorrectly) {
            delay(3000L)
            onGuessExpire()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(72.dp)
    ) {
        // Gray circle placeholder for avatar (or green circle if they guessed the word)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (hasGuessedCorrectly) Color(0xFF4CAF50) else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = if (hasGuessedCorrectly) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Username
        Text(
            text = player.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            fontWeight = if (isLocalPlayer) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Drawer text or guesser small text box
        if (isDrawer) {
            Text(
                text = "DRAWER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        } else {
            val boxColor = when {
                hasGuessedCorrectly -> Color(0xFF4CAF50)
                !activeGuess.isNullOrEmpty() -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val borderStyle = when {
                hasGuessedCorrectly -> BorderStroke(1.dp, Color(0xFF2E7D32))
                !activeGuess.isNullOrEmpty() -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .border(
                        width = borderStyle.width,
                        brush = borderStyle.brush,
                        shape = RoundedCornerShape(4.dp)
                    ),
                shape = RoundedCornerShape(4.dp),
                color = boxColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (hasGuessedCorrectly) "✓ Correct!" else (activeGuess ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasGuessedCorrectly) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        fontWeight = if (hasGuessedCorrectly) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// Vertical bar slider for adjusting canvas stroke width
@Composable
fun VerticalStrokeSlider(
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    minWidth: Float = 4f,
    maxWidth: Float = 40f,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Size preview indicator
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(strokeWidth.dp.coerceIn(4.dp, 24.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Vertical bar track
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    val updateSize: (Offset) -> Unit = { offset ->
                        val h = size.height.toFloat()
                        val fraction = (1f - (offset.y / h)).coerceIn(0f, 1f)
                        val newWidth = minWidth + fraction * (maxWidth - minWidth)
                        onStrokeWidthChange(newWidth)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        updateSize(down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            change.consume()
                            updateSize(change.position)
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val fraction = ((strokeWidth - minWidth) / (maxWidth - minWidth)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )
        }
    }
}

// Circular RGB color wheel, vertical hue slider, and current color display
@Composable
fun RgbColorPickerWithSlider(
    baseColor: Color,
    selectedColor: Color,
    brightnessValue: Float,
    onBaseColorSelected: (Color) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green,
            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Color wheel canvas
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        val detectColorTouch: (Offset) -> Unit = { offset ->
                            val radius = size.width / 2f
                            val dx = offset.x - radius
                            val dy = offset.y - radius
                            val dist = kotlin.math.hypot(dx, dy)
                            val angle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                            val sat = (dist / radius).coerceIn(0f, 1f)
                            val color = Color.hsv(hue = angle, saturation = sat, value = 1f)
                            onBaseColorSelected(color)
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            detectColorTouch(down.position)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                change.consume()
                                detectColorTouch(change.position)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.sweepGradient(hueColors),
                        radius = radius
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius
                    )
                }
            }

            // Vertical hue slider
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, baseColor, Color.Black)
                        )
                    )
                    .pointerInput(Unit) {
                        val updateBrightness: (Offset) -> Unit = { offset ->
                            val h = size.height.toFloat()
                            val frac = (1f - (offset.y / h)).coerceIn(0f, 1f)
                            onBrightnessChanged(frac)
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            updateBrightness(down.position)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                change.consume()
                                updateBrightness(change.position)
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                val yFraction = (1f - brightnessValue.coerceIn(0f, 1f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .offset(y = (yFraction * 76).dp)
                        .background(Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Current color display
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(selectedColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        )
    }
}

// Computes color with hue adjustment (0.0 = Black, 0.5 = Base Color, and 1.0 = White)
private fun computeColor(baseColor: Color, brightness: Float): Color {
    val b = brightness.coerceIn(0f, 1f)
    return if (b < 0.5f) {
        val factor = b / 0.5f
        Color(
            red = baseColor.red * factor,
            green = baseColor.green * factor,
            blue = baseColor.blue * factor,
            alpha = 1f
        )
    } else {
        val factor = (b - 0.5f) / 0.5f
        Color(
            red = baseColor.red + (1f - baseColor.red) * factor,
            green = baseColor.green + (1f - baseColor.green) * factor,
            blue = baseColor.blue + (1f - baseColor.blue) * factor,
            alpha = 1f
        )
    }
}

// Finds the color at the tapped point on the canvas
private fun pickColorAtPoint(
    point: Offset,
    strokes: List<StrokePath>,
    backgroundColor: Color
): Color {
    for (i in strokes.indices.reversed()) {
        val stroke = strokes[i]
        val hitRadius = stroke.strokeWidth / 2f + 6f
        if (stroke.points.size == 1) {
            if ((point - stroke.points.first()).getDistance() <= hitRadius) {
                return if (stroke.isEraser) backgroundColor else stroke.color
            }
        } else {
            for (j in 0 until stroke.points.size - 1) {
                val p1 = stroke.points[j]
                val p2 = stroke.points[j + 1]
                if (distanceToSegment(point, p1, p2) <= hitRadius) {
                    return if (stroke.isEraser) backgroundColor else stroke.color
                }
            }
        }
    }
    return backgroundColor
}

// Calculates perpendicular distance from point p to line segment ab
private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val v = b - a
    val w = p - a
    val c1 = w.x * v.x + w.y * v.y
    if (c1 <= 0) return (p - a).getDistance()
    val c2 = v.x * v.x + v.y * v.y
    if (c2 <= c1) return (p - b).getDistance()
    val b2 = c1 / c2
    val pb = a + Offset(v.x * b2, v.y * b2)
    return (p - pb).getDistance()
}
