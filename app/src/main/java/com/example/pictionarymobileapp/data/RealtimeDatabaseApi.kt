package com.example.pictionarymobileapp.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
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

// Realtime canvas data payload synced across all session participants
data class CanvasRealtimeData(
    val strokes: List<SerializableStroke> = emptyList(),
    val activeStrokePoints: List<SerializablePoint> = emptyList(),
    val activeStrokeColorArgb: Int = 0xFF000000.toInt(),
    val activeStrokeWidth: Float = 12f,
    val activeStrokeIsEraser: Boolean = false,
    val backgroundColorArgb: Int = 0xFFFFFFFF.toInt()
)

// Realtime Database API for streaming drawing strokes live from the drawer screen to all guesser screens in real time, and synchronizing shared round timers
object RealtimeDatabaseApi {

    private const val RTDB_URL = "https://draw-and-guess-firebase-default-rtdb.firebaseio.com"

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Throwable) {
            false
        }

    private val database: FirebaseDatabase?
        get() = if (isFirebaseAvailable) FirebaseDatabase.getInstance(RTDB_URL) else null

    private val canvasStreams = MutableStateFlow<Map<String, CanvasRealtimeData>>(emptyMap())
    val allCanvasData: StateFlow<Map<String, CanvasRealtimeData>> = canvasStreams.asStateFlow()

    private val timerStreams = MutableStateFlow<Map<String, Int>>(emptyMap())
    val allTimerData: StateFlow<Map<String, Int>> = timerStreams.asStateFlow()

    private val drawingListeners = mutableMapOf<String, ValueEventListener>()
    private val timerListeners = mutableMapOf<String, ValueEventListener>()

    private var serverTimeOffset: Long = 0L

    init {
        val currentDb = database
        if (currentDb != null) {
            currentDb.getReference(".info/serverTimeOffset").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    // Listens to live drawing and timer data for a given lobby code
    fun listenToSession(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        if (drawingListeners.containsKey(formattedCode)) return

        val currentDb = database ?: return

        val drawingRef = currentDb.getReference("lobbies").child(formattedCode).child("drawing")
        val dListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    canvasStreams.update { current -> current + (formattedCode to CanvasRealtimeData()) }
                    return
                }

                val strokesList = mutableListOf<SerializableStroke>()
                snapshot.child("strokes").children.forEach { strokeSnap ->
                    val stroke = strokeSnap.getValue(SerializableStroke::class.java)
                    if (stroke != null) strokesList.add(stroke)
                }

                val activePts = mutableListOf<SerializablePoint>()
                snapshot.child("activeStrokePoints").children.forEach { ptSnap ->
                    val pt = ptSnap.getValue(SerializablePoint::class.java)
                    if (pt != null) activePts.add(pt)
                }

                val activeColor = snapshot.child("activeStrokeColorArgb").getValue(Int::class.java) ?: 0xFF000000.toInt()
                val activeWidth = snapshot.child("activeStrokeWidth").getValue(Float::class.java) ?: 12f
                val activeEraser = snapshot.child("activeStrokeIsEraser").getValue(Boolean::class.java) ?: false
                val bgArgb = snapshot.child("backgroundColorArgb").getValue(Int::class.java) ?: 0xFFFFFFFF.toInt()

                val data = CanvasRealtimeData(
                    strokes = strokesList,
                    activeStrokePoints = activePts,
                    activeStrokeColorArgb = activeColor,
                    activeStrokeWidth = activeWidth,
                    activeStrokeIsEraser = activeEraser,
                    backgroundColorArgb = bgArgb
                )

                canvasStreams.update { current -> current + (formattedCode to data) }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        drawingRef.addValueEventListener(dListener)
        drawingListeners[formattedCode] = dListener

        // Listen to shared timer node
        val timerRef = currentDb.getReference("lobbies").child(formattedCode).child("timer")
        val tListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val roundStartTime = snapshot.child("roundStartTime").getValue(Long::class.java) ?: 0L
                val durationSeconds = snapshot.child("durationSeconds").getValue(Int::class.java) ?: 45

                if (roundStartTime > 0L) {
                    val estimatedServerTime = System.currentTimeMillis() + serverTimeOffset
                    val elapsedSeconds = ((estimatedServerTime - roundStartTime) / 1000L).toInt()
                    val remaining = (durationSeconds - elapsedSeconds).coerceAtLeast(0)
                    timerStreams.update { current -> current + (formattedCode to remaining) }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        timerRef.addValueEventListener(tListener)
        timerListeners[formattedCode] = tListener
    }

    // Writes round start timestamp and duration to Realtime Database
    fun startRoundTimer(code: String, durationSeconds: Int) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        val currentDb = database ?: return
        val timerRef = currentDb.getReference("lobbies").child(formattedCode).child("timer")
        timerRef.setValue(
            mapOf(
                "roundStartTime" to ServerValue.TIMESTAMP,
                "durationSeconds" to durationSeconds
            )
        )
    }

    // Updates completed strokes and background color for lobby code
    fun updateDrawerStrokes(
        code: String,
        strokes: List<SerializableStroke>,
        backgroundColorArgb: Int
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        val currentDb = database ?: return
        val drawingRef = currentDb.getReference("lobbies").child(formattedCode).child("drawing")

        drawingRef.setValue(
            mapOf(
                "strokes" to strokes,
                "activeStrokePoints" to emptyList<SerializablePoint>(),
                "activeStrokeColorArgb" to 0xFF000000.toInt(),
                "activeStrokeWidth" to 12f,
                "activeStrokeIsEraser" to false,
                "backgroundColorArgb" to backgroundColorArgb
            )
        )
    }

    // Streams live active stroke points in real time as the drawer draws on canvas
    fun updateActiveStroke(
        code: String,
        points: List<SerializablePoint>,
        colorArgb: Int,
        strokeWidth: Float,
        isEraser: Boolean
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        val currentDb = database ?: return
        val drawingRef = currentDb.getReference("lobbies").child(formattedCode).child("drawing")

        drawingRef.child("activeStrokePoints").setValue(points)
        drawingRef.child("activeStrokeColorArgb").setValue(colorArgb)
        drawingRef.child("activeStrokeWidth").setValue(strokeWidth)
        drawingRef.child("activeStrokeIsEraser").setValue(isEraser)
    }

    // Resets canvas streaming data for a given lobby code
    fun clearCanvas(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        val currentDb = database
        if (currentDb != null) {
            val drawingRef = currentDb.getReference("lobbies").child(formattedCode).child("drawing")
            drawingRef.setValue(CanvasRealtimeData())
        }
        canvasStreams.update { current ->
            current + (formattedCode to CanvasRealtimeData())
        }
    }

    // Cleans up listeners for a lobby session
    fun detachSessionListeners(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT).ifBlank { "DEFAULT" }
        val currentDb = database ?: return
        drawingListeners.remove(formattedCode)?.let {
            currentDb.getReference("lobbies").child(formattedCode).child("drawing").removeEventListener(it)
        }
        timerListeners.remove(formattedCode)?.let {
            currentDb.getReference("lobbies").child(formattedCode).child("timer").removeEventListener(it)
        }
    }
}

// Extension helpers to convert Compose Color to or from 32 bit ARGB Int
fun Color.toArgbInt(): Int = this.toArgb()
fun Int.toComposeColor(): Color = Color(this)
