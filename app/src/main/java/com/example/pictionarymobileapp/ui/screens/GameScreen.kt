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

// Represents a single continuous stroke drawn on the canvas
data class StrokePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

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
    onRoundEnd: () -> Unit
) {
    // Toggle for testing drawer vs guesser views easily
    var isDrawer by remember { mutableStateOf(true) }
    var guessInput by remember { mutableStateOf("") }

    // Canvas state
    val strokes = remember { mutableStateListOf<StrokePath>() }
    val actionHistory = remember { mutableStateListOf<CanvasAction>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var strokeWidth by remember { mutableFloatStateOf(12f) }

    // Color state
    var baseWheelColor by remember { mutableStateOf(Color.Red) }
    var brightnessValue by remember { mutableFloatStateOf(0.5f) } // 0.0 = Black, 0.5 = Base Color, 1.0 = White
    val selectedColor = remember(baseWheelColor, brightnessValue) {
        computeColor(baseWheelColor, brightnessValue)
    }

    var canvasBackgroundColor by remember { mutableStateOf(Color.White) }
    var isEraser by remember { mutableStateOf(false) }
    var isEyeDropper by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Theme: Animals", style = MaterialTheme.typography.bodyMedium)
            Text("Time: 0:45", style = MaterialTheme.typography.bodyMedium)
            Text("Drawing: Player 2", style = MaterialTheme.typography.bodyMedium)
        }
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

        // Interactive drawing canvas
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
                .background(canvasBackgroundColor)
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
                                // Eye dropper mode
                                val picked = pickColorAtPoint(
                                    point = down.position,
                                    strokes = strokes,
                                    backgroundColor = canvasBackgroundColor
                                )
                                baseWheelColor = picked
                                brightnessValue = 0.5f
                                isEyeDropper = false
                            } else {
                                // Drawing mode
                                val points = mutableListOf(down.position)
                                currentPoints = points.toList()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    change.consume()
                                    points.add(change.position)
                                    currentPoints = points.toList()
                                }

                                if (points.isNotEmpty()) {
                                    val newStroke = StrokePath(
                                        points = points.toList(),
                                        color = if (isEraser) canvasBackgroundColor else selectedColor,
                                        strokeWidth = strokeWidth,
                                        isEraser = isEraser
                                    )
                                    strokes.add(newStroke)
                                    actionHistory.add(CanvasAction.DrawStroke(newStroke))
                                }
                                currentPoints = emptyList()
                            }
                        }
                    }
            ) {
                // Background fill
                drawRect(canvasBackgroundColor)

                // Helper to render stroke path
                fun drawStrokePath(stroke: StrokePath) {
                    if (stroke.points.isEmpty()) return
                    val drawColor = if (stroke.isEraser) canvasBackgroundColor else stroke.color

                    if (stroke.points.size == 1) {
                        drawCircle(
                            color = drawColor,
                            radius = stroke.strokeWidth / 2f,
                            center = stroke.points.first()
                        )
                    } else {
                        val path = Path().apply {
                            moveTo(stroke.points.first().x, stroke.points.first().y)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = drawColor,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Render saved strokes
                for (stroke in strokes) {
                    drawStrokePath(stroke)
                }

                // Render current active stroke
                if (currentPoints.isNotEmpty()) {
                    drawStrokePath(
                        StrokePath(
                            points = currentPoints,
                            color = if (isEraser) canvasBackgroundColor else selectedColor,
                            strokeWidth = strokeWidth,
                            isEraser = isEraser
                        )
                    )
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

        // Drawer controls and guesser view
        if (isDrawer) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top row has stroke size, color wheel, hue value slider, and word to draw
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

                    // Color wheel with hue value slider and current color indicator
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
                        Text(
                            text = "Word to draw:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Cat",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom row has eraser, fill, eye dropper, undo, and start over tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Eraser mode
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

                    // Fill tool
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
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Fill", style = MaterialTheme.typography.labelSmall)
                    }

                    // Eye dropper tool
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

                    // Undo tool
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
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Undo", style = MaterialTheme.typography.labelSmall)
                    }

                    // Start over button
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
                Button(onClick = { guessInput = "" }) {
                    Text("Guess")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulate round end button
        Button(
            onClick = onRoundEnd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate Round End")
        }
    }
}

//Vertical bar slider for adjusting stroke width
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
        // Stroke size indicator
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

        // Vertical bar
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

// Circular RGB color wheel paired with a vertical hue value slider and a current color indicator
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
            // Color wheel
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

            // Vertical hue value slider
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

        // Current color indicator
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

// Computes color with hue value adjustment
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
