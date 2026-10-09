package com.example.domain.recognition

data class VerseCandidate(
    val surahNumber: Int,
    val ayahNumber: Int,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class StabilizedVerse(
    val surahNumber: Int,
    val ayahNumber: Int,
    val confidence: Float,
    val isSequential: Boolean
)

/**
 * Stabilizes verse detections to prevent erratic jumps and maintain smooth page transitions.
 * Stays locked on the current Ayah while reciting, prioritizes sequential progression in the current surah,
 * and strictly protects against unintended jumps to other surahs.
 */
class VerseStabilizer {
    private var currentVerse: StabilizedVerse? = null
    private val candidateWindow = mutableListOf<VerseCandidate>()
    private val windowSize = 4
    private var lastTransitionTime: Long = 0
    private val minTransitionIntervalMs = 600L

    fun processCandidate(candidate: VerseCandidate, forceImmediate: Boolean = false): StabilizedVerse? {
        val now = System.currentTimeMillis()
        candidateWindow.add(candidate)
        if (candidateWindow.size > windowSize) {
            candidateWindow.removeAt(0)
        }

        val current = currentVerse

        // 1. Initial detection
        if (current == null) {
            val newVerse = StabilizedVerse(
                surahNumber = candidate.surahNumber,
                ayahNumber = candidate.ayahNumber,
                confidence = candidate.confidence,
                isSequential = false
            )
            currentVerse = newVerse
            lastTransitionTime = now
            return newVerse
        }

        // 2. Candidate is the exact SAME Ayah: stay locked on it
        if (current.surahNumber == candidate.surahNumber &&
            current.ayahNumber == candidate.ayahNumber
        ) {
            return current
        }

        // If forceImmediate is explicitly requested (e.g. manual simulation click or direct page jump)
        if (forceImmediate) {
            val newVerse = StabilizedVerse(
                surahNumber = candidate.surahNumber,
                ayahNumber = candidate.ayahNumber,
                confidence = candidate.confidence,
                isSequential = candidate.surahNumber == current.surahNumber && candidate.ayahNumber == current.ayahNumber + 1
            )
            currentVerse = newVerse
            lastTransitionTime = now
            return newVerse
        }

        // 3. Candidate is in the SAME Surah:
        if (current.surahNumber == candidate.surahNumber) {
            val isForwardProgression = candidate.ayahNumber > current.ayahNumber
            val isNearbyAyah = Math.abs(candidate.ayahNumber - current.ayahNumber) <= 15

            if (isForwardProgression || isNearbyAyah) {
                val newVerse = StabilizedVerse(
                    surahNumber = candidate.surahNumber,
                    ayahNumber = candidate.ayahNumber,
                    confidence = candidate.confidence,
                    isSequential = candidate.ayahNumber == current.ayahNumber + 1
                )
                currentVerse = newVerse
                lastTransitionTime = now
                return newVerse
            }
        }

        // 4. Candidate is in a DIFFERENT Surah:
        // Strictly protect against jumping out of the active surah!
        // Allow transition only for:
        // - Surah Al-Fatihah (new Rak'ah in prayer)
        // - Verified high-confidence full verse / Basmalah transition (confidence >= 0.95f)
        val isFatihahTransition = candidate.surahNumber == 1
        val isVerifiedFullVerse = candidate.confidence >= 0.95f

        if ((isFatihahTransition || isVerifiedFullVerse) && (now - lastTransitionTime >= minTransitionIntervalMs)) {
            val newVerse = StabilizedVerse(
                surahNumber = candidate.surahNumber,
                ayahNumber = candidate.ayahNumber,
                confidence = candidate.confidence,
                isSequential = false
            )
            currentVerse = newVerse
            lastTransitionTime = now
            return newVerse
        }

        // Otherwise stay solidly locked on current verse
        return current
    }

    fun setVerse(surahNumber: Int, ayahNumber: Int) {
        currentVerse = StabilizedVerse(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            confidence = 1.0f,
            isSequential = false
        )
        candidateWindow.clear()
        lastTransitionTime = System.currentTimeMillis()
    }

    fun reset() {
        currentVerse = null
        candidateWindow.clear()
        lastTransitionTime = 0
    }

    fun getCurrentVerse(): StabilizedVerse? = currentVerse
}
