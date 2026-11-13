package com.example.pictionarymobileapp.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

// Serializable 2D point for Realtime Database canvas streaming
data class SerializablePoint(
    val x: Float = 0f,
    val y: Float = 0f
) {
    fun toOffset(): Offset = Offset(x, y)

    companion object {
        fun fromOffset(offset: Offset): SerializablePoint = SerializablePoint(offset.x, offset.y)
    }
}

// Serializable drawing stroke for Realtime Database canvas streaming
data class SerializableStroke(
    val points: List<SerializablePoint> = emptyList(),
    val colorArgb: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 12f,
    val isEraser: Boolean = false
)

// Real-time canvas data payload synced across all session paritcipants
data class CanvasRealtimeData(
    val strokes: List<SerializableStroke> = emptyList(),
    val activeStrokePoints: List<SerializablePoint> = emptyList(),
    val activeStrokeColorArgb: Int = 0xFF000000.toInt(),
    val activeStrokeWidth: Float = 12f,
    val activeStrokeIsEraser: Boolean = false,
    val backgroundColorArgb: Int = 0xFFFFFFFF.toInt()
)

// Realtime Database API for streaming drawing strokes live from the drawer's screen to all guesser's
object RealtimeDatabaseApi {

    private val canvasStreams = MutableStateFlow<Map<String, CanvasRealtimeData>>(emptyMap())

    // Direct StateFlow for observing all real-time canvas streams keyed by lobby code
    val allCanvasData: StateFlow<Map<String, CanvasRealtimeData>> = canvasStreams.asStateFlow()

    // Updates completed strokes and background color for lobby code
    fun updateDrawerStrokes(
        code: String,
        strokes: List<SerializableStroke>,
        backgroundColorArgb: Int
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        canvasStreams.update { current ->
            val existing = current[formattedCode] ?: CanvasRealtimeData()
            current + (formattedCode to existing.copy(
                strokes = strokes,
                activeStrokePoints = emptyList(),
                backgroundColorArgb = backgroundColorArgb
            ))
        }
    }

    // Streams live active stroke points in real-time as the drawer draws on canvas
    fun updateActiveStroke(
        code: String,
        points: List<SerializablePoint>,
        colorArgb: Int,
        strokeWidth: Float,
        isEraser: Boolean
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        canvasStreams.update { current ->
            val existing = current[formattedCode] ?: CanvasRealtimeData()
            current + (formattedCode to existing.copy(
                activeStrokePoints = points,
                activeStrokeColorArgb = colorArgb,
                activeStrokeWidth = strokeWidth,
                activeStrokeIsEraser = isEraser
            ))
        }
    }

    // Resets canvas streaming data for a given lobby code
    fun clearCanvas(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        canvasStreams.update { current ->
            current + (formattedCode to CanvasRealtimeData())
        }
    }
}

// Extension helpers to convert Compose Color to/from 32-bit ARGB Int
fun Color.toArgbInt(): Int = this.toArgb()
fun Int.toComposeColor(): Color = Color(this)
