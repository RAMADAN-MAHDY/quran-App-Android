package com.example.domain.recognition

import android.util.Log
import com.example.core.ArabicNormalizer
import com.example.data.local.SurahEntity
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "RecitationMatcher"

private val STOP_WORDS = setOf(
    "في", "من", "ما", "لا", "ان", "عن", "او", "ثم", "هو", "قد", "بل", "يا",
    "ذا", "ذو", "ذي", "به", "له", "كم", "هم", "هن", "انما", "كان", "كانت"
)

private val FATIHAH_KEYWORDS = listOf(
    "الحمد لله رب العالمين",
    "الرحمن الرحيم مالك يوم الدين",
    "اياك نعبد واياك نستعين",
    "اهدنا الصراط المستقيم"
)

class RecitationMatcher(private val repository: QuranRepository) {

    private data class CachedAyah(
        val surahNumber: Int,
        val ayahNumber: Int,
        val pageNumber: Int,
        val textNormalized: String,
        val words: List<String>,
        val wordSet: Set<String>,
        val contentWords: Set<String>
    )

    private val mutex = Mutex()
    private var isInitialized = false
    private val ayahCache = mutableListOf<CachedAyah>()
    private val surahCache = mutableListOf<SurahEntity>()
    private val ayahMap = mutableMapOf<Pair<Int, Int>, CachedAyah>()

    suspend fun warmUp() {
        if (isInitialized) return
        mutex.withLock {
            if (isInitialized) return
            try {
                val ayahs = repository.getAllAyahsSync()
                val surahs = repository.getAllSurahsSync()

                surahCache.clear()
                surahCache.addAll(surahs)

                ayahCache.clear()
                ayahMap.clear()
                for (a in ayahs) {
                    val norm = ArabicNormalizer.normalize(a.textNormalized)
                    val wordsList = norm.split(" ").filter { it.length >= 2 }
                    val contentList = wordsList.filter { !STOP_WORDS.contains(it) }
                    val cached = CachedAyah(
                        surahNumber = a.surahNumber,
                        ayahNumber = a.ayahNumber,
                        pageNumber = a.pageNumber,
                        textNormalized = norm,
                        words = wordsList,
                        wordSet = wordsList.toSet(),
                        contentWords = contentList.toSet()
                    )
                    ayahCache.add(cached)
                    ayahMap[Pair(a.surahNumber, a.ayahNumber)] = cached
                }
                isInitialized = true
                Log.d(TAG, "Quran in-memory cache initialized with ${ayahCache.size} ayahs and ${surahCache.size} surahs")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ayah cache: ${e.message}")
            }
        }
    }

    private fun getCachedAyah(surahNumber: Int, ayahNumber: Int): CachedAyah? {
        return ayahMap[Pair(surahNumber, ayahNumber)]
    }

    private fun calculateMatchScore(queryNorm: String, target: CachedAyah): Float {
        if (target.textNormalized.contains(queryNorm)) {
            return 1.0f
        }
        val queryWords = queryNorm.split(" ").filter { it.length >= 2 }
        val queryContent = queryWords.filter { !STOP_WORDS.contains(it) }

        if (queryContent.isEmpty()) {
            val commonAll = queryWords.intersect(target.wordSet)
            return if (commonAll.isNotEmpty()) commonAll.size.toFloat() / queryWords.size.toFloat() else 0f
        }

        val common = queryContent.intersect(target.contentWords)
        if (common.isEmpty()) return 0f

        val minRequired = if (queryContent.size == 1) 1 else 2
        if (common.size < minRequired) return 0f

        return common.size.toFloat() / queryContent.size.toFloat()
    }

    /**
     * Matches raw or transcribed speech against the Quran database.
     *
     * In prayer and Quran recitation:
     * 1. While actively reciting in a Surah, we hold strong inertia to the CURRENT Surah & sequential continuation.
     * 2. Overlapping words with the current verse keep the position locked on the current verse.
     * 3. Recitation moves forward sequentially in the current surah.
     * 4. A jump to an entirely different Surah is STRICTLY FORBIDDEN on partial snippets or 1-3 words.
     *    It is only permitted when:
     *    - A complete distinct verse (آية كاملة مختلفة) is recited with high confidence (>= 0.85f and >= 4 content words).
     *    - Or a new Basmalah ("بسم الله الرحمن الرحيم") marks a new Surah start.
     *    - Or Surah Al-Fatihah is recited (start of new Rak'ah).
     *    - Or an explicit Surah name is stated.
     */
    suspend fun matchText(
        rawText: String,
        preferredSurah: Int? = null,
        preferredAyah: Int? = null
    ): VerseCandidate? {
        if (!isInitialized) {
            warmUp()
        }

        val normalized = ArabicNormalizer.normalize(rawText)
        if (normalized.length < 2) return null

        val queryWords = normalized.split(" ").filter { it.length >= 2 }
        val queryContent = queryWords.filter { !STOP_WORDS.contains(it) }

        // 1. Direct match for Ayat Al-Kursi
        if (normalized.contains("الكرسي")) {
            return VerseCandidate(surahNumber = 2, ayahNumber = 255, confidence = 1.0f)
        }

        // 2. Transition Cue: Surah Al-Fatihah (Vital in prayer at the start of each Rak'ah)
        if (FATIHAH_KEYWORDS.any { normalized.contains(it) }) {
            Log.d(TAG, "Detected Al-Fatihah transition cue in recitation")
            return VerseCandidate(surahNumber = 1, ayahNumber = 1, confidence = 1.0f)
        }

        // 3. Transition Cue: Explicit Surah names (e.g. "سورة الكهف", "سورة ق", "سورة يس")
        val cleanQuery = normalized
            .replace("سوره", "")
            .replace("سورة", "")
            .trim()

        if (cleanQuery.isNotBlank() && cleanQuery.length >= 2) {
            for (surah in surahCache) {
                val sNorm = ArabicNormalizer.normalize(surah.nameArabic)
                if (cleanQuery == sNorm ||
                    (cleanQuery.length >= 3 && sNorm.contains(cleanQuery)) ||
                    (cleanQuery.length >= 4 && cleanQuery.contains(sNorm))
                ) {
                    Log.d(TAG, "Explicit surah name recognized: ${surah.nameArabic}")
                    return VerseCandidate(surahNumber = surah.id, ayahNumber = 1, confidence = 1.0f)
                }
            }
        }

        // 4. Transition Cue: Basmalah ("بسم الله الرحمن الرحيم") starting a new Surah
        if (normalized.contains("بسم الله الرحمن الرحيم") || normalized.contains("بسم الله")) {
            val afterBasmalah = normalized
                .replace("بسم الله الرحمن الرحيم", "")
                .replace("بسم الله", "")
                .trim()
            if (afterBasmalah.length >= 3) {
                for (a in ayahCache) {
                    if (a.ayahNumber == 1) {
                        val score = calculateMatchScore(afterBasmalah, a)
                        if (score >= 0.50f) {
                            Log.d(TAG, "Basmalah new surah detected: Surah ${a.surahNumber}")
                            return VerseCandidate(surahNumber = a.surahNumber, ayahNumber = 1, confidence = 1.0f)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ACTIVE SURAH TRACKING LOGIC (When preferredSurah and preferredAyah are set)
        // =========================================================================
        if (preferredSurah != null && preferredAyah != null) {
            val currentAyah = getCachedAyah(preferredSurah, preferredAyah)

            // Step A: CURRENT ACTIVE AYAH RETENTION
            // If the user is currently reciting within the same verse, STAY LOCKED on it!
            if (currentAyah != null) {
                val currentScore = calculateMatchScore(normalized, currentAyah)
                val hasOverlapWithCurrent = queryContent.isNotEmpty() && queryContent.any { currentAyah.contentWords.contains(it) }

                if (currentScore >= 0.25f || hasOverlapWithCurrent) {
                    Log.d(TAG, "Words match current ayah ($preferredSurah:$preferredAyah). Staying locked on current verse.")
                    return VerseCandidate(
                        surahNumber = preferredSurah,
                        ayahNumber = preferredAyah,
                        confidence = 1.0f
                    )
                }
            }

            // Step B: SEQUENTIAL FORWARD PROGRESSION IN THE SAME SURAH (+1 up to +15 ayahs)
            // Recitation in prayer naturally flows forward verse by verse
            for (offset in 1..15) {
                val targetAyahNum = preferredAyah + offset
                val targetAyah = getCachedAyah(preferredSurah, targetAyahNum) ?: break
                val score = calculateMatchScore(normalized, targetAyah)
                val minThreshold = if (offset == 1) 0.35f else 0.45f

                if (score >= minThreshold) {
                    Log.d(TAG, "Advancing sequentially to nearby ayah ($preferredSurah:$targetAyahNum) score $score.")
                    return VerseCandidate(
                        surahNumber = preferredSurah,
                        ayahNumber = targetAyahNum,
                        confidence = 1.0f
                    )
                }
            }

            // Step C: REPETITION IN SAME SURAH (previous 1-2 verses)
            for (offset in 1..2) {
                val prevAyahNum = preferredAyah - offset
                if (prevAyahNum >= 1) {
                    val prevAyah = getCachedAyah(preferredSurah, prevAyahNum) ?: continue
                    val score = calculateMatchScore(normalized, prevAyah)
                    if (score >= 0.45f) {
                        Log.d(TAG, "Repeated previous ayah in same surah ($preferredSurah:$prevAyahNum).")
                        return VerseCandidate(
                            surahNumber = preferredSurah,
                            ayahNumber = prevAyahNum,
                            confidence = 0.95f
                        )
                    }
                }
            }

            // Step D: ANYWHERE ELSE IN CURRENT SURAH
            var bestInSurah: CachedAyah? = null
            var bestInSurahScore = 0f
            for (ayah in ayahCache) {
                if (ayah.surahNumber == preferredSurah) {
                    val score = calculateMatchScore(normalized, ayah)
                    if (score > bestInSurahScore) {
                        bestInSurahScore = score
                        bestInSurah = ayah
                    }
                }
            }
            if (bestInSurah != null && bestInSurahScore >= 0.50f) {
                Log.d(TAG, "Matched within current surah ($preferredSurah:${bestInSurah.ayahNumber}) with score $bestInSurahScore.")
                return VerseCandidate(
                    surahNumber = preferredSurah,
                    ayahNumber = bestInSurah.ayahNumber,
                    confidence = bestInSurahScore
                )
            }

            // Step E: STRICT RESTRICTION FOR JUMPING TO A DIFFERENT SURAH
            // User requirement: "هو ينقل في حالة واحدة بس إن هو لو شاف الآية كاملة مختلفة"
            // To prevent accidental jumps (like jumping from Al-Anbiya to Qaf on a 2-word snippet):
            // We require:
            // 1. At least 4 content words (or if entire target verse has <= 3 words, all of them).
            // 2. High match score >= 0.85f AND significant coverage of the verse words.
            if (queryContent.size < 4) {
                Log.d(TAG, "Insufficient words (${queryContent.size}) for external surah jump. Retaining current surah ($preferredSurah:$preferredAyah).")
                return VerseCandidate(
                    surahNumber = preferredSurah,
                    ayahNumber = preferredAyah,
                    confidence = 0.85f
                )
            }

            var bestOtherAyah: CachedAyah? = null
            var bestOtherScore = 0f
            for (ayah in ayahCache) {
                if (ayah.surahNumber != preferredSurah) {
                    val score = calculateMatchScore(normalized, ayah)
                    val commonWordsCount = queryWords.toSet().intersect(ayah.wordSet).size
                    val verseCoverage = if (ayah.words.isNotEmpty()) commonWordsCount.toFloat() / ayah.words.size.toFloat() else 0f

                    if (score >= 0.85f && (verseCoverage >= 0.60f || commonWordsCount >= 5)) {
                        if (score > bestOtherScore) {
                            bestOtherScore = score
                            bestOtherAyah = ayah
                        }
                    }
                }
            }

            if (bestOtherAyah != null) {
                Log.d(TAG, "Verified FULL-VERSE distant jump to Surah ${bestOtherAyah.surahNumber}:${bestOtherAyah.ayahNumber} with score $bestOtherScore")
                return VerseCandidate(
                    surahNumber = bestOtherAyah.surahNumber,
                    ayahNumber = bestOtherAyah.ayahNumber,
                    confidence = 1.0f
                )
            }

            // If no full verse match found in other surahs, stay on current position!
            Log.d(TAG, "No verified full verse in other surahs. Keeping current surah position.")
            return VerseCandidate(
                surahNumber = preferredSurah,
                ayahNumber = preferredAyah,
                confidence = 0.80f
            )
        }

        // =========================================================================
        // INITIAL MATCHING (When no surah is actively tracked yet)
        // =========================================================================

        // Exact substring match
        val exactMatches = ayahCache.filter { it.textNormalized.contains(normalized) }
        if (exactMatches.isNotEmpty()) {
            val best = exactMatches.first()
            return VerseCandidate(
                surahNumber = best.surahNumber,
                ayahNumber = best.ayahNumber,
                confidence = 1.0f
            )
        }

        // Global Content-Word Matching
        var bestAyah: CachedAyah? = null
        var bestScore = 0f

        for (ayah in ayahCache) {
            val score = calculateMatchScore(normalized, ayah)
            if (score > bestScore) {
                bestScore = score
                bestAyah = ayah
            }
        }

        return if (bestAyah != null && bestScore >= 0.40f) {
            VerseCandidate(
                surahNumber = bestAyah.surahNumber,
                ayahNumber = bestAyah.ayahNumber,
                confidence = bestScore
            )
        } else {
            // Fallback: SQLite search if in-memory found no match
            val dbMatches = repository.search(normalized)
            if (dbMatches.isNotEmpty()) {
                val first = dbMatches.first()
                VerseCandidate(
                    surahNumber = first.surahNumber,
                    ayahNumber = first.ayahNumber,
                    confidence = 0.5f
                )
            } else {
                null
            }
        }
    }
}
