package com.example.pictionarymobileapp.data

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.random.Random

object LobbySessionManager {

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Throwable) {
            false
        }

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) FirebaseFirestore.getInstance() else null

    private val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) FirebaseAuth.getInstance() else null

    private val activeLobbies = MutableStateFlow<Map<String, Lobby>>(emptyMap())

    // Observable stream of all active lobbies keyed by lobby join code
    val allLobbies: StateFlow<Map<String, Lobby>> = activeLobbies.asStateFlow()

    private val lobbyListeners = mutableMapOf<String, ListenerRegistration>()
    private val playerListeners = mutableMapOf<String, ListenerRegistration>()

    // Ensures the client is authenticated with Firebase Anonymous Auth
    fun ensureAuthenticated(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val currentAuth = auth
        if (currentAuth == null) {
            onSuccess("LOCAL_TEST_UID")
            return
        }

        val currentUser = currentAuth.currentUser
        if (currentUser != null) {
            onSuccess(currentUser.uid)
        } else {
            currentAuth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        onSuccess(user.uid)
                    } else {
                        onError(IllegalStateException("Firebase Auth user was null."))
                    }
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        }
    }

    // Returns the current authenticated Firebase user UID or local fallback
    fun getCurrentUid(): String {
        return auth?.currentUser?.uid ?: "LOCAL_TEST_UID"
    }

    // Creates a new lobby in Firestore or in-memory fallback
    fun createLobby(
        hostUsername: String,
        avatarId: Int = 1,
        theme: String = "Animals",
        roundsPerPlayer: Int = 3,
        timePerRoundSeconds: Int = 45,
        availability: String = "private",
        onResult: (JoinResult) -> Unit = {}
    ): Lobby {
        val cleanName = hostUsername.trim().ifBlank { "Host" }
        val code = generateUniqueCode()
        val currentUid = getCurrentUid()

        val hostPlayer = Player(
            id = currentUid,
            name = cleanName,
            isHost = true,
            avatarId = avatarId,
            score = 0
        )

        val newLobby = Lobby(
            code = code,
            hostUid = currentUid,
            hostUsername = cleanName,
            players = listOf(hostPlayer),
            theme = theme,
            roundsPerPlayer = roundsPerPlayer,
            timePerRoundSeconds = timePerRoundSeconds,
            availability = availability,
            status = "waiting",
            gameIndex = 1
        )

        activeLobbies.update { current ->
            current + (code to newLobby)
        }

        val currentFirestore = firestore
        if (currentFirestore == null) {
            onResult(JoinResult.Success(newLobby, cleanName))
            return newLobby
        }

        ensureAuthenticated(
            onSuccess = { uid ->
                val lobbyRef = currentFirestore.collection("lobbies").document(code)
                val updatedHostPlayer = hostPlayer.copy(id = uid)

                currentFirestore.runTransaction { transaction ->
                    val snapshot = transaction.get(lobbyRef)
                    if (snapshot.exists()) {
                        throw IllegalStateException("Lobby code collision. Please try again.")
                    }

                    transaction.set(lobbyRef, mapOf(
                        "code" to code,
                        "hostUid" to uid,
                        "hostUsername" to cleanName,
                        "theme" to theme,
                        "roundsPerPlayer" to roundsPerPlayer,
                        "timePerRoundSeconds" to timePerRoundSeconds,
                        "maxPlayers" to newLobby.maxPlayers,
                        "status" to "waiting",
                        "currentRound" to 1,
                        "totalRounds" to newLobby.totalRounds,
                        "currentDrawerUid" to "",
                        "drawerSchedule" to emptyList<String>(),
                        "wordChosen" to false,
                        "availability" to availability,
                        "gameIndex" to 1,
                        "createdAt" to System.currentTimeMillis()
                    ))

                    val playerRef = lobbyRef.collection("players").document(uid)
                    transaction.set(playerRef, mapOf(
                        "uid" to uid,
                        "name" to cleanName,
                        "isHost" to true,
                        "avatarId" to avatarId,
                        "score" to 0,
                        "joinedAt" to System.currentTimeMillis()
                    ))
                }.addOnSuccessListener {
                    listenToLobby(code)
                    onResult(JoinResult.Success(newLobby.copy(hostUid = uid, players = listOf(updatedHostPlayer)), cleanName))
                }.addOnFailureListener { e ->
                    onResult(JoinResult.Error(e.message ?: "Failed to create lobby."))
                }
            },
            onError = { e ->
                onResult(JoinResult.Error("Authentication failed: ${e.message}"))
            }
        )

        return newLobby
    }

    // Joins an existing lobby in Firestore or in-memory fallback
    fun joinLobby(code: String, username: String, avatarId: Int = 1, onResult: (JoinResult) -> Unit = {}): JoinResult {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        if (formattedCode.isBlank()) {
            val err = JoinResult.Error("Please enter a valid lobby code.")
            onResult(err)
            return err
        }

        val cleanName = username.trim().ifBlank { "Player" }
        val currentUid = getCurrentUid()

        val currentFirestore = firestore
        if (currentFirestore == null) {
            val lobby = activeLobbies.value[formattedCode]
                ?: return JoinResult.Error("Lobby '$formattedCode' does not exist.").also { onResult(it) }

            if (lobby.players.any { it.name.equals(cleanName, ignoreCase = true) }) {
                val err = JoinResult.Error("Username '$cleanName' is already taken in this lobby.")
                onResult(err)
                return err
            }

            val newPlayer = Player(id = currentUid + "_" + Random.nextInt(1000, 9999), name = cleanName, isHost = false, avatarId = avatarId)
            val updatedLobby = lobby.copy(players = lobby.players + newPlayer)

            activeLobbies.update { current ->
                current + (formattedCode to updatedLobby)
            }

            val res = JoinResult.Success(updatedLobby, cleanName)
            onResult(res)
            return res
        }

        ensureAuthenticated(
            onSuccess = { uid ->
                val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)

                lobbyRef.get().addOnSuccessListener { lobbySnap ->
                    if (!lobbySnap.exists()) {
                        onResult(JoinResult.Error("Lobby '$formattedCode' does not exist."))
                        return@addOnSuccessListener
                    }

                    val status = lobbySnap.getString("status") ?: "waiting"
                    if (status != "waiting") {
                        onResult(JoinResult.Error("Game has already started in lobby '$formattedCode'."))
                        return@addOnSuccessListener
                    }

                    val playersRef = lobbyRef.collection("players")
                    playersRef.get().addOnSuccessListener { playersSnap ->
                        val isNameTaken = playersSnap.documents.any { pDoc ->
                            val pName = pDoc.getString("name") ?: ""
                            pName.equals(cleanName, ignoreCase = true)
                        }

                        if (isNameTaken) {
                            onResult(JoinResult.Error("Username '$cleanName' is already taken in this lobby. Please choose a different username."))
                            return@addOnSuccessListener
                        }

                        val playerRef = playersRef.document(uid)
                        playerRef.set(mapOf(
                            "uid" to uid,
                            "name" to cleanName,
                            "isHost" to false,
                            "avatarId" to avatarId,
                            "score" to 0,
                            "joinedAt" to System.currentTimeMillis()
                        )).addOnSuccessListener {
                            listenToLobby(formattedCode)

                            lobbyRef.get().addOnSuccessListener { freshSnap ->
                                val theme = freshSnap.getString("theme") ?: "Animals"
                                val hostUid = freshSnap.getString("hostUid") ?: ""
                                val hostUsername = freshSnap.getString("hostUsername") ?: "Host"
                                val roundsPerPlayer = (freshSnap.getLong("roundsPerPlayer") ?: 3L).toInt()
                                val timePerRoundSeconds = (freshSnap.getLong("timePerRoundSeconds") ?: 45L).toInt()
                                val maxPlayers = (freshSnap.getLong("maxPlayers") ?: 8L).toInt()
                                val freshStatus = freshSnap.getString("status") ?: "waiting"
                                val currentRound = (freshSnap.getLong("currentRound") ?: 1L).toInt()
                                val currentDrawerUid = freshSnap.getString("currentDrawerUid") ?: ""
                                val drawerSchedule = (freshSnap.get("drawerSchedule") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                                val wordChosen = freshSnap.getBoolean("wordChosen") ?: false
                                val availability = freshSnap.getString("availability") ?: "private"
                                val gameIndex = (freshSnap.getLong("gameIndex") ?: 1L).toInt()

                                playersRef.get().addOnSuccessListener { freshPlayersSnap ->
                                    val playersList = freshPlayersSnap.documents.mapNotNull { pDoc ->
                                        Player(
                                            id = pDoc.getString("uid") ?: pDoc.id,
                                            name = pDoc.getString("name") ?: "Player",
                                            isHost = pDoc.getBoolean("isHost") ?: false,
                                            avatarId = (pDoc.getLong("avatarId") ?: 1L).toInt(),
                                            score = (pDoc.getLong("score") ?: 0L).toInt()
                                        )
                                    }.sortedBy { it.joinedAt }

                                    val joinedLobby = Lobby(
                                        code = formattedCode,
                                        hostUid = hostUid,
                                        hostUsername = hostUsername,
                                        players = playersList,
                                        theme = theme,
                                        roundsPerPlayer = roundsPerPlayer,
                                        timePerRoundSeconds = timePerRoundSeconds,
                                        maxPlayers = maxPlayers,
                                        status = freshStatus,
                                        currentRound = currentRound,
                                        currentDrawerUid = currentDrawerUid,
                                        drawerSchedule = drawerSchedule,
                                        wordChosen = wordChosen,
                                        availability = availability,
                                        gameIndex = gameIndex
                                    )

                                    activeLobbies.update { current ->
                                        current + (formattedCode to joinedLobby)
                                    }

                                    onResult(JoinResult.Success(joinedLobby, cleanName))
                                }
                            }
                        }.addOnFailureListener { e ->
                            onResult(JoinResult.Error(e.message ?: "Failed to join lobby."))
                        }
                    }.addOnFailureListener { e ->
                        onResult(JoinResult.Error(e.message ?: "Failed to read lobby players."))
                    }
                }.addOnFailureListener { e ->
                    onResult(JoinResult.Error(e.message ?: "Lobby '$formattedCode' does not exist."))
                }
            },
            onError = { e ->
                onResult(JoinResult.Error("Authentication failed: ${e.message}"))
            }
        )

        return JoinResult.Success(getLobby(formattedCode) ?: Lobby(code = formattedCode), cleanName)
    }

    // Listens live to all public lobbies with status waiting
    fun listenToPublicLobbies(onUpdate: (List<Lobby>) -> Unit): ListenerRegistration? {
        val currentFirestore = firestore
        if (currentFirestore == null) {
            // Local memory fallback
            val publicList = activeLobbies.value.values.filter {
                it.availability.equals("public", ignoreCase = true) && it.status == "waiting"
            }
            onUpdate(publicList)
            return null
        }

        return currentFirestore.collection("lobbies")
            .whereEqualTo("availability", "public")
            .whereEqualTo("status", "waiting")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val lobbyDocs = snapshot.documents
                if (lobbyDocs.isEmpty()) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val resultList = mutableListOf<Lobby>()
                var fetchedCount = 0

                for (doc in lobbyDocs) {
                    val code = doc.getString("code") ?: doc.id
                    val hostUid = doc.getString("hostUid") ?: ""
                    val hostUsername = doc.getString("hostUsername") ?: "Host"
                    val theme = doc.getString("theme") ?: "Animals"
                    val roundsPerPlayer = (doc.getLong("roundsPerPlayer") ?: 3L).toInt()
                    val timePerRoundSeconds = (doc.getLong("timePerRoundSeconds") ?: 45L).toInt()
                    val maxPlayers = (doc.getLong("maxPlayers") ?: 8L).toInt()
                    val status = doc.getString("status") ?: "waiting"
                    val availability = doc.getString("availability") ?: "public"

                    doc.reference.collection("players").get().addOnSuccessListener { playersSnap ->
                        val playersList = playersSnap.documents.mapNotNull { pDoc ->
                            Player(
                                id = pDoc.getString("uid") ?: pDoc.id,
                                name = pDoc.getString("name") ?: "Player",
                                isHost = pDoc.getBoolean("isHost") ?: false,
                                avatarId = (pDoc.getLong("avatarId") ?: 1L).toInt(),
                                score = (pDoc.getLong("score") ?: 0L).toInt()
                            )
                        }.sortedBy { it.joinedAt }

                        resultList.add(
                            Lobby(
                                code = code,
                                hostUid = hostUid,
                                hostUsername = hostUsername,
                                players = playersList,
                                theme = theme,
                                roundsPerPlayer = roundsPerPlayer,
                                timePerRoundSeconds = timePerRoundSeconds,
                                maxPlayers = maxPlayers,
                                status = status,
                                availability = availability
                            )
                        )

                        fetchedCount++
                        if (fetchedCount == lobbyDocs.size) {
                            onUpdate(resultList.toList())
                        }
                    }.addOnFailureListener {
                        fetchedCount++
                        if (fetchedCount == lobbyDocs.size) {
                            onUpdate(resultList.toList())
                        }
                    }
                }
            }
    }

    // Synchronizes live Firestore lobby and player documents to activeLobbies StateFlow
    fun listenToLobby(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        if (formattedCode.isBlank() || lobbyListeners.containsKey(formattedCode)) return

        RealtimeDatabaseApi.listenToSession(formattedCode)

        val currentFirestore = firestore ?: return
        val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)

        val lListener = lobbyRef.addSnapshotListener { lobbySnap, error ->
            if (error != null || lobbySnap == null || !lobbySnap.exists()) {
                activeLobbies.update { it - formattedCode }
                return@addSnapshotListener
            }

            // Immediately ensure activeLobbies contains an entry for formattedCode
            val currentEntry = activeLobbies.value[formattedCode]
            if (currentEntry == null) {
                val initialLobby = Lobby(
                    code = formattedCode,
                    hostUid = lobbySnap.getString("hostUid") ?: "",
                    hostUsername = lobbySnap.getString("hostUsername") ?: "Host",
                    theme = lobbySnap.getString("theme") ?: "Animals",
                    roundsPerPlayer = (lobbySnap.getLong("roundsPerPlayer") ?: 3L).toInt(),
                    timePerRoundSeconds = (lobbySnap.getLong("timePerRoundSeconds") ?: 45L).toInt(),
                    maxPlayers = (lobbySnap.getLong("maxPlayers") ?: 8L).toInt(),
                    status = lobbySnap.getString("status") ?: "waiting",
                    currentRound = (lobbySnap.getLong("currentRound") ?: 1L).toInt(),
                    currentDrawerUid = lobbySnap.getString("currentDrawerUid") ?: "",
                    drawerSchedule = (lobbySnap.get("drawerSchedule") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList(),
                    wordChosen = lobbySnap.getBoolean("wordChosen") ?: false,
                    availability = lobbySnap.getString("availability") ?: "private",
                    gameIndex = (lobbySnap.getLong("gameIndex") ?: 1L).toInt()
                )
                activeLobbies.update { current ->
                    current + (formattedCode to initialLobby)
                }
            }

            val playersRef = lobbyRef.collection("players")
            val pListener = playersRef.addSnapshotListener { playersSnap, pError ->
                if (pError != null || playersSnap == null) return@addSnapshotListener

                val playersList = playersSnap.documents.mapNotNull { pDoc ->
                    Player(
                        id = pDoc.getString("uid") ?: pDoc.id,
                        name = pDoc.getString("name") ?: "Player",
                        isHost = pDoc.getBoolean("isHost") ?: false,
                        avatarId = (pDoc.getLong("avatarId") ?: 1L).toInt(),
                        score = (pDoc.getLong("score") ?: 0L).toInt()
                    )
                }.sortedBy { it.joinedAt }

                val updatedLobby = Lobby(
                    code = formattedCode,
                    hostUid = lobbySnap.getString("hostUid") ?: "",
                    hostUsername = lobbySnap.getString("hostUsername") ?: "Host",
                    players = playersList,
                    theme = lobbySnap.getString("theme") ?: "Animals",
                    roundsPerPlayer = (lobbySnap.getLong("roundsPerPlayer") ?: 3L).toInt(),
                    timePerRoundSeconds = (lobbySnap.getLong("timePerRoundSeconds") ?: 45L).toInt(),
                    maxPlayers = (lobbySnap.getLong("maxPlayers") ?: 8L).toInt(),
                    status = lobbySnap.getString("status") ?: "waiting",
                    currentRound = (lobbySnap.getLong("currentRound") ?: 1L).toInt(),
                    currentDrawerUid = lobbySnap.getString("currentDrawerUid") ?: "",
                    drawerSchedule = (lobbySnap.get("drawerSchedule") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList(),
                    wordChosen = lobbySnap.getBoolean("wordChosen") ?: false,
                    availability = lobbySnap.getString("availability") ?: "private",
                    gameIndex = (lobbySnap.getLong("gameIndex") ?: 1L).toInt()
                )

                activeLobbies.update { current ->
                    current + (formattedCode to updatedLobby)
                }
            }
            playerListeners[formattedCode] = pListener
        }

        lobbyListeners[formattedCode] = lListener
    }

    // Signals that the drawer has chosen a word for the current round
    fun onWordChosen(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentFirestore = firestore
        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)
            lobbyRef.update("wordChosen", true)
        } else {
            activeLobbies.update { current ->
                val existing = current[formattedCode] ?: return@update current
                current + (formattedCode to existing.copy(wordChosen = true))
            }
        }
    }

    // Removes player or disbands lobby in Firestore when player leaves
    fun leaveLobby(code: String, username: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentUid = getCurrentUid()
        val lobby = getLobby(formattedCode) ?: return

        val leavingPlayer = lobby.players.find { it.name.equals(username, ignoreCase = true) }
            ?: lobby.players.find { it.id == currentUid }

        val isHostPlayer = leavingPlayer?.isHost == true

        if (isHostPlayer) {
            disbandLobby(formattedCode)
        } else {
            val currentFirestore = firestore
            if (currentFirestore != null) {
                val targetUid = leavingPlayer?.id ?: currentUid
                currentFirestore.collection("lobbies").document(formattedCode)
                    .collection("players").document(targetUid).delete()
            } else {
                val updatedPlayers = lobby.players.filterNot { it.name.equals(username, ignoreCase = true) }
                activeLobbies.update { current ->
                    current + (formattedCode to lobby.copy(players = updatedPlayers))
                }
            }
        }
    }

    // Disbands and deletes the lobby from Firestore and Realtime Database
    fun disbandLobby(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        RealtimeDatabaseApi.clearCanvas(formattedCode)
        RealtimeDatabaseApi.detachSessionListeners(formattedCode)

        lobbyListeners.remove(formattedCode)?.remove()
        playerListeners.remove(formattedCode)?.remove()

        val currentFirestore = firestore
        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)

            // Mark status as disbanded to immediately notify all connected clients
            lobbyRef.update("status", "disbanded").addOnCompleteListener {
                // Delete players
                lobbyRef.collection("players").get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc -> doc.reference.delete() }

                    // Delete rounds and guesses
                    lobbyRef.collection("rounds").get().addOnSuccessListener { roundsSnap ->
                        roundsSnap.documents.forEach { roundDoc ->
                            roundDoc.reference.collection("guesses").get().addOnSuccessListener { guessesSnap ->
                                guessesSnap.documents.forEach { guessDoc -> guessDoc.reference.delete() }
                                roundDoc.reference.delete()
                            }
                        }
                        lobbyRef.delete()
                    }
                }
            }
        }

        activeLobbies.update { it - formattedCode }
    }

    // Retrieves local snapshot of lobby by code
    fun getLobby(code: String): Lobby? {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        return activeLobbies.value[formattedCode]
    }

    // Updates lobby settings in Firestore or in-memory fallback
    fun updateSettings(
        code: String,
        theme: String = "Animals",
        roundsPerPlayer: Int = 3,
        timePerRoundSeconds: Int = 45
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentFirestore = firestore
        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)
            lobbyRef.update(
                mapOf(
                    "theme" to theme,
                    "roundsPerPlayer" to roundsPerPlayer,
                    "timePerRoundSeconds" to timePerRoundSeconds
                )
            )
        } else {
            activeLobbies.update { current ->
                val existing = current[formattedCode] ?: return@update current
                current + (formattedCode to existing.copy(
                    theme = theme,
                    roundsPerPlayer = roundsPerPlayer,
                    timePerRoundSeconds = timePerRoundSeconds
                ))
            }
        }
    }

    // Generates a randomized drawer schedule and starts the game in Firestore
    fun startGame(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val lobby = getLobby(formattedCode) ?: return
        val players = lobby.players
        if (players.isEmpty()) return

        val shuffledUids = players.map { it.id }.shuffled()
        val fullSchedule = mutableListOf<String>()
        for (set in 1..lobby.roundsPerPlayer) {
            fullSchedule.addAll(shuffledUids)
        }

        val firstDrawerUid = fullSchedule.firstOrNull() ?: ""
        val currentFirestore = firestore

        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)
            lobbyRef.update(
                mapOf(
                    "status" to "playing",
                    "currentRound" to 1,
                    "drawerSchedule" to fullSchedule,
                    "currentDrawerUid" to firstDrawerUid,
                    "wordChosen" to false
                )
            ).addOnSuccessListener {
                RealtimeDatabaseApi.clearCanvas(formattedCode)
                RealtimeDatabaseApi.startRoundTimer(formattedCode, lobby.timePerRoundSeconds)
            }
        } else {
            activeLobbies.update { current ->
                current + (formattedCode to lobby.copy(
                    status = "playing",
                    currentRound = 1,
                    drawerSchedule = fullSchedule,
                    currentDrawerUid = firstDrawerUid,
                    wordChosen = false
                ))
            }
        }
    }

    // Updates game status in Firestore
    fun updateGameStatus(code: String, status: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentFirestore = firestore
        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)
            lobbyRef.update("status", status)
        } else {
            activeLobbies.update { current ->
                val existing = current[formattedCode] ?: return@update current
                current + (formattedCode to existing.copy(status = status))
            }
        }
    }

    // Submits a guess to current round's guesses collection in Firestore
    fun submitGuess(
        code: String,
        currentRound: Int,
        guesserUid: String,
        guesserName: String,
        guessText: String
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentFirestore = firestore ?: return
        val gameIndex = getLobby(formattedCode)?.gameIndex ?: 1
        val roundDocKey = "g${gameIndex}_r${currentRound}"

        val guessRef = currentFirestore.collection("lobbies")
            .document(formattedCode)
            .collection("rounds")
            .document(roundDocKey)
            .collection("guesses")
            .document(guesserUid)

        guessRef.set(
            mapOf(
                "playerUid" to guesserUid,
                "playerName" to guesserName,
                "guessText" to guessText,
                "isCorrect" to false,
                "pointsAwarded" to 0,
                "submittedAt" to System.currentTimeMillis()
            )
        )
    }

    // Atomic transaction to mark a guess as correct and award points to guesser and drawer
    fun awardPoints(
        code: String,
        currentRound: Int,
        guesserUid: String,
        drawerUid: String,
        guesserPoints: Int
    ) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentFirestore = firestore
        if (currentFirestore == null) {
            // Local fallback for unit testing
            activeLobbies.update { current ->
                val existing = current[formattedCode] ?: return@update current
                val updatedPlayers = existing.players.map { player ->
                    when {
                        player.name.equals(guesserUid, ignoreCase = true) || player.id == guesserUid -> {
                            player.copy(score = player.score + guesserPoints)
                        }
                        player.name.equals(drawerUid, ignoreCase = true) || player.id == drawerUid -> {
                            player.copy(score = player.score + 1)
                        }
                        else -> player
                    }
                }
                current + (formattedCode to existing.copy(players = updatedPlayers))
            }
            return
        }

        val gameIndex = getLobby(formattedCode)?.gameIndex ?: 1
        val roundDocKey = "g${gameIndex}_r${currentRound}"
        val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)

        lobbyRef.collection("players").get().addOnSuccessListener { playersSnap ->
            val guesserDoc = playersSnap.documents.find {
                it.id == guesserUid ||
                it.getString("uid") == guesserUid ||
                it.getString("name").equals(guesserUid, ignoreCase = true)
            }
            val drawerDoc = playersSnap.documents.find {
                it.id == drawerUid ||
                it.getString("uid") == drawerUid ||
                it.getString("name").equals(drawerUid, ignoreCase = true)
            }

            val targetGuesserDocId = guesserDoc?.id ?: guesserUid
            val targetDrawerDocId = drawerDoc?.id ?: drawerUid

            val guessRef = lobbyRef.collection("rounds").document(roundDocKey)
                .collection("guesses").document(targetGuesserDocId)
            val guesserRef = lobbyRef.collection("players").document(targetGuesserDocId)

            currentFirestore.runTransaction { transaction ->
                val guessSnap = transaction.get(guessRef)
                val alreadyCorrect = guessSnap.getBoolean("isCorrect") ?: false
                if (alreadyCorrect) {
                    return@runTransaction
                }

                transaction.set(
                    guessRef,
                    mapOf("isCorrect" to true, "pointsAwarded" to guesserPoints, "playerUid" to targetGuesserDocId),
                    SetOptions.merge()
                )

                val guesserSnap = transaction.get(guesserRef)
                if (guesserSnap.exists()) {
                    val currentScore = (guesserSnap.getLong("score") ?: 0L).toInt()
                    transaction.update(guesserRef, "score", currentScore + guesserPoints)
                }

                if (targetDrawerDocId.isNotBlank() && targetDrawerDocId != targetGuesserDocId) {
                    val drawerRef = lobbyRef.collection("players").document(targetDrawerDocId)
                    val drawerSnap = transaction.get(drawerRef)
                    if (drawerSnap.exists()) {
                        val currentScore = (drawerSnap.getLong("score") ?: 0L).toInt()
                        transaction.update(drawerRef, "score", currentScore + 1)
                    }
                }
            }.addOnFailureListener {
                guessRef.set(mapOf("isCorrect" to true, "pointsAwarded" to guesserPoints), SetOptions.merge())
                guesserDoc?.reference?.update("score", FieldValue.increment(guesserPoints.toLong()))
                if (drawerDoc != null && targetDrawerDocId != targetGuesserDocId) {
                    drawerDoc.reference.update("score", FieldValue.increment(1L))
                }
            }
        }
    }

    // Advances to the next round in Firestore
    fun advanceToNextRound(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val lobby = getLobby(formattedCode) ?: return

        // Guard to only advance if current lobby status is round transition
        if (lobby.status != "round_transition" && isFirebaseAvailable) {
            return
        }

        val nextRound = lobby.currentRound + 1
        val nextDrawerUid = lobby.drawerSchedule.getOrNull(nextRound - 1) ?: ""

        val currentFirestore = firestore
        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)
            lobbyRef.update(
                mapOf(
                    "currentRound" to nextRound,
                    "currentDrawerUid" to nextDrawerUid,
                    "status" to "playing",
                    "wordChosen" to false
                )
            ).addOnSuccessListener {
                RealtimeDatabaseApi.clearCanvas(formattedCode)
                RealtimeDatabaseApi.startRoundTimer(formattedCode, lobby.timePerRoundSeconds)
            }
        } else {
            activeLobbies.update { current ->
                current + (formattedCode to lobby.copy(
                    currentRound = nextRound,
                    currentDrawerUid = nextDrawerUid,
                    status = "playing",
                    wordChosen = false
                ))
            }
        }
    }

    // Resets game status to waiting, resets current round to 1, resets scores to 0, increments gameIndex, and deletes previous round documents and guesses from Firestore
    fun resetGame(code: String) {
        val formattedCode = code.trim().uppercase(Locale.ROOT)
        val currentLobby = getLobby(formattedCode)
        val nextGameIndex = (currentLobby?.gameIndex ?: 1) + 1

        val currentFirestore = firestore
        if (currentFirestore != null) {
            val lobbyRef = currentFirestore.collection("lobbies").document(formattedCode)

            // Reset all player scores to 0
            lobbyRef.collection("players").get().addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    doc.reference.update("score", 0)
                }
            }

            // Delete all previous rounds documents and guesses subcollections
            lobbyRef.collection("rounds").get().addOnSuccessListener { roundsSnap ->
                roundsSnap.documents.forEach { roundDoc ->
                    roundDoc.reference.collection("guesses").get().addOnSuccessListener { guessesSnap ->
                        guessesSnap.documents.forEach { guessDoc ->
                            guessDoc.reference.delete()
                        }
                        roundDoc.reference.delete()
                    }
                }
            }

            // Reset lobby fields and increment gameIndex for game isolation
            lobbyRef.update(
                mapOf(
                    "status" to "waiting",
                    "currentRound" to 1,
                    "currentDrawerUid" to "",
                    "drawerSchedule" to emptyList<String>(),
                    "wordChosen" to false,
                    "gameIndex" to nextGameIndex
                )
            ).addOnSuccessListener {
                RealtimeDatabaseApi.clearCanvas(formattedCode)
            }
        } else {
            activeLobbies.update { current ->
                val existing = current[formattedCode] ?: return@update current
                val resetPlayers = existing.players.map { it.copy(score = 0) }
                current + (formattedCode to existing.copy(
                    players = resetPlayers,
                    currentRound = 1,
                    status = "waiting",
                    wordChosen = false,
                    gameIndex = nextGameIndex
                ))
            }
        }
    }

    // Generates a unique 6 character uppercase alphanumeric code
    private fun generateUniqueCode(): String {
        val charPool = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        do {
            code = (1..6)
                .map { Random.nextInt(0, charPool.length) }
                .map(charPool::get)
                .joinToString("")
        } while (activeLobbies.value.containsKey(code))
        return code
    }
}
