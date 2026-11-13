package com.example.pictionarymobileapp.data

// Word Bank API for themes, word selection, and guess validation
object WordBankRepository {

    // Themes
    val themes: List<String> = listOf("Animals", "Locations")

    // Map of themes to lists of words
    private val wordBank: Map<String, List<String>> = mapOf(
        "Animals" to listOf(
            "Cat", "Dog", "Elephant", "Lizard", "Shark",
            "Tiger", "Penguin", "Dolphin", "Panda", "Giraffe"
        ),
        "Locations" to listOf(
            "Beach", "Airport", "Hospital", "Library", "Park",
            "Museum", "Restaurant", "School", "Supermarket", "Zoo"
        )
    )

    // Retrieves random word options for a given theme
    fun getRandomWordChoices(theme: String, count: Int = 3): List<String> {
        val words = wordBank[theme] ?: wordBank["Animals"]!!
        return words.shuffled().take(count.coerceAtMost(words.size))
    }

    // Validates whether a guess matches the target word (case-insensitive and trimmed)
    fun isCorrectGuess(targetWord: String?, guess: String): Boolean {
        if (targetWord.isNullOrBlank() || guess.isBlank()) return false
        return targetWord.trim().equals(guess.trim(), ignoreCase = true)
    }
}
