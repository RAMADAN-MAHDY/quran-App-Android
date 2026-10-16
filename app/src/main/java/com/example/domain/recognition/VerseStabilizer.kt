package com.example.domain.recognition

data class VerseCandidate(
    val surahNumber: Int,
    val ayahNumber: Int,
    val confidence: Float,
    val isSequenceMatch: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class StabilizedVerse(
    val surahNumber: Int,
    val ayahNumber: Int,
    val confidence: Float,
    val isSequential: Boolean
)

/**
 * Stabilizes verse detections to prevent erratic jumps while allowing immediate initial response
 * and seamless redirection when subsequent verses confirm a different surah (mutashabihat resolution).
 */
class VerseStabilizer {
    private var currentVerse: StabilizedVerse? = null
    private val candidateWindow = mutableListOf<VerseCandidate>()
    private val windowSize = 4
    private var lastTransitionTime: Long = 0
    private val minTransitionIntervalMs = 400L
    private var versesProcessedCount = 0

    // Buffer to track pending surah transition candidates for multi-verse sequence confirmation
    private var pendingSurahCandidate: VerseCandidate? = null
    private var pendingSurahCount = 0

    fun processCandidate(
        candidate: VerseCandidate,
        forceImmediate: Boolean = false,
        isNewRakaah: Boolean = false
    ): StabilizedVerse? {
        val now = System.currentTimeMillis()
        candidateWindow.add(candidate)
        if (candidateWindow.size > windowSize) {
            candidateWindow.removeAt(0)
        }

        val current = currentVerse

        // 1. Initial detection or New Rak'ah (after silence pause >= 12s):
        // Immediately accept the candidate for instantaneous UI responsiveness!
        if (current == null || isNewRakaah) {
            val newVerse = StabilizedVerse(
                surahNumber = candidate.surahNumber,
                ayahNumber = candidate.ayahNumber,
                confidence = candidate.confidence,
                isSequential = false
            )
            currentVerse = newVerse
            lastTransitionTime = now
            versesProcessedCount = 1
            pendingSurahCandidate = null
            pendingSurahCount = 0
            return newVerse
        }

        // 2. Candidate is the exact SAME Ayah: stay locked on it
        if (current.surahNumber == candidate.surahNumber &&
            current.ayahNumber == candidate.ayahNumber
        ) {
            pendingSurahCandidate = null
            pendingSurahCount = 0
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
            versesProcessedCount = 1
            pendingSurahCandidate = null
            pendingSurahCount = 0
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
                versesProcessedCount++
                pendingSurahCandidate = null
                pendingSurahCount = 0
                return newVerse
            }
        }

        // 4. Candidate is in a DIFFERENT Surah:
        // Accumulate pending surah candidates to require 2-3 consecutive verse matches for mid-recitation surah jumps
        val pending = pendingSurahCandidate
        if (pending != null && pending.surahNumber == candidate.surahNumber) {
            pendingSurahCount++
        } else {
            pendingSurahCandidate = candidate
            pendingSurahCount = 1
        }

        // Dynamically allow redirection when:
        // - 2 or more consecutive verse matches accumulated in the new surah (pendingSurahCount >= 2)
        // - Candidate is verified by sequence match (isSequenceMatch = true)
        // - Natural transition at surah boundary into the next surah (candidate.surahNumber == current.surahNumber + 1 && candidate.ayahNumber <= 5)
        // - Early in recitation tracking (versesProcessedCount <= 3) with candidate confidence >= 0.70f
        // - Surah Al-Fatihah transition cue (candidate.surahNumber == 1)
        // - Verified high-confidence transition (candidate.confidence >= 0.85f)
        val isMultiVerseConfirmed = pendingSurahCount >= 2
        val isSequenceMatch = candidate.isSequenceMatch
        val isNextSurahTransition = (candidate.surahNumber == current.surahNumber + 1) && (candidate.ayahNumber <= 5)
        val isEarlyAdjustment = versesProcessedCount <= 3 && candidate.confidence >= 0.70f
        val isFatihahTransition = candidate.surahNumber == 1
        val isHighConfidenceTransition = candidate.confidence >= 0.85f

        val allowSurahRedirect = isMultiVerseConfirmed || isSequenceMatch || isNextSurahTransition || isEarlyAdjustment || isFatihahTransition || isHighConfidenceTransition

        if (allowSurahRedirect && (now - lastTransitionTime >= minTransitionIntervalMs)) {
            val newVerse = StabilizedVerse(
                surahNumber = candidate.surahNumber,
                ayahNumber = candidate.ayahNumber,
                confidence = candidate.confidence,
                isSequential = candidate.surahNumber == current.surahNumber + 1 && candidate.ayahNumber == 1
            )
            currentVerse = newVerse
            lastTransitionTime = now
            versesProcessedCount = 1
            pendingSurahCandidate = null
            pendingSurahCount = 0
            return newVerse
        }

        // Otherwise stay locked on current verse while buffer accumulates confirmation
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
        versesProcessedCount = 1
        pendingSurahCandidate = null
        pendingSurahCount = 0
    }

    fun reset() {
        currentVerse = null
        candidateWindow.clear()
        lastTransitionTime = 0
        versesProcessedCount = 0
        pendingSurahCandidate = null
        pendingSurahCount = 0
    }

    fun getCurrentVerse(): StabilizedVerse? = currentVerse
}
