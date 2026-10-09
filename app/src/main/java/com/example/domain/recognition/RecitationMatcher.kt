package com.example.domain.recognition

import android.util.Log
import com.example.core.ArabicNormalizer
import com.example.data.local.AyahEntity
import com.example.data.local.SurahEntity
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "RecitationMatcher"

private val STOP_WORDS = setOf(
    "في", "من", "ما", "لا", "ان", "عن", "او", "ثم", "هو", "قد", "بل", "يا",
    "ذا", "ذو", "ذي", "به", "له", "كم", "هم", "هن", "انما", "كان", "كانت"
)

private val BASMALAH_CLEAN_REGEX = Regex("^(بسم\\s+الله\\s+الرحمن\\s+الرحيم|بسم\\s+الله)\\s*")

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
                    val norm = ArabicNormalizer.normalize(a.textNormalized).trim()
                    // Remove leading Basmalah for Ayah 1 of all surahs except Surah 1 so text matching matches pure verses
                    val cleanNorm = if (a.surahNumber != 1 && a.ayahNumber == 1) {
                        BASMALAH_CLEAN_REGEX.replace(norm, "").trim()
                    } else {
                        norm
                    }

                    val wordsList = cleanNorm.split(" ").filter { it.length >= 2 }
                    val contentList = wordsList.filter { !STOP_WORDS.contains(it) }
                    val cached = CachedAyah(
                        surahNumber = a.surahNumber,
                        ayahNumber = a.ayahNumber,
                        pageNumber = a.pageNumber,
                        textNormalized = cleanNorm,
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
     * Segments continuous spoken speech into distinct identified Ayahs in real-time.
     * Prevents speech from joining into one monolithic query that disrupts search and page display.
     */
    suspend fun segmentVerses(
        rawText: String,
        preferredSurah: Int? = null,
        preferredAyah: Int? = null
    ): List<AyahEntity> {
        if (!isInitialized) warmUp()
        val normalized = ArabicNormalizer.normalize(rawText).trim()
        if (normalized.length < 3) return emptyList()

        val queryWords = normalized.split(" ").filter { it.length >= 2 }
        if (queryWords.isEmpty()) return emptyList()
        val queryWordSet = queryWords.toSet()
        val paddedQuery = " $normalized "

        // If preferredSurah is active, search within the current Surah first
        if (preferredSurah != null) {
            val inSurahPool = ayahCache.filter { it.surahNumber == preferredSurah }
            val matched = mutableListOf<CachedAyah>()
            for (ayah in inSurahPool) {
                if (matchesAyah(paddedQuery, queryWords, queryWordSet, ayah)) {
                    matched.add(ayah)
                }
            }
            if (matched.isNotEmpty()) {
                return matched
                    .sortedBy { it.ayahNumber }
                    .mapNotNull { repository.getAyah(it.surahNumber, it.ayahNumber) }
            }
        }

        // Global search fallback for segmentation
        val globalMatched = mutableListOf<CachedAyah>()
        for (ayah in ayahCache) {
            if (matchesAyah(paddedQuery, queryWords, queryWordSet, ayah)) {
                globalMatched.add(ayah)
            }
        }

        if (globalMatched.isEmpty()) return emptyList()

        // If matches come from multiple surahs, prefer the surah with the most consecutive matches
        val grouped = globalMatched.groupBy { it.surahNumber }
        val bestSurahEntry = grouped.maxByOrNull { it.value.size }
        val bestAyahs = bestSurahEntry?.value ?: globalMatched

        return bestAyahs
            .sortedBy { it.ayahNumber }
            .mapNotNull { repository.getAyah(it.surahNumber, it.ayahNumber) }
    }

    private fun matchesAyah(
        paddedQuery: String,
        queryWords: List<String>,
        queryWordSet: Set<String>,
        ayah: CachedAyah
    ): Boolean {
        val paddedAyah = " ${ayah.textNormalized} "
        // Condition 1: Ayah is fully contained in query (with word boundaries)
        if (ayah.words.size >= 2 && paddedQuery.contains(paddedAyah)) {
            return true
        }
        // Condition 2: Query is a substantial continuous phrase of the Ayah
        if (queryWords.size >= 3 && paddedAyah.contains(paddedQuery)) {
            return true
        }
        // Condition 3: High recall word coverage of the target Ayah
        val common = queryWordSet.intersect(ayah.wordSet)
        val minReq = if (ayah.words.size <= 3) 2 else 3
        if (common.size >= minReq && common.size.toFloat() / ayah.words.size.toFloat() >= 0.60f) {
            return true
        }
        return false
    }

    /**
     * Matches raw or transcribed speech against the Quran database.
     * Enforces strict surah stability, sequential progression, and verse segmentation.
     */
    suspend fun matchText(
        rawText: String,
        preferredSurah: Int? = null,
        preferredAyah: Int? = null
    ): VerseCandidate? {
        if (!isInitialized) {
            warmUp()
        }

        val normalized = ArabicNormalizer.normalize(rawText).trim()
        if (normalized.length < 2) return null

        val queryWords = normalized.split(" ").filter { it.length >= 2 }
        val queryContent = queryWords.filter { !STOP_WORDS.contains(it) }

        // 1. Direct match for Ayat Al-Kursi (explicit name or key opening phrase)
        if (normalized.contains("اية الكرسي") || normalized.contains("ايه الكرسي") ||
            normalized.contains("الله لا اله الا هو الحي القيوم")
        ) {
            return VerseCandidate(surahNumber = 2, ayahNumber = 255, confidence = 1.0f)
        }

        // 2. Explicit Surah name request (e.g. "سورة الكهف", "سورة ق", "سورة ص", "سورة يس")
        val hasSurahPrefix = normalized.contains("سورة") || normalized.contains("سوره")
        if (hasSurahPrefix) {
            val surahQuery = normalized
                .replace("سوره", "")
                .replace("سورة", "")
                .trim()
            if (surahQuery.isNotBlank()) {
                for (surah in surahCache) {
                    val sNorm = ArabicNormalizer.normalize(surah.nameArabic)
                    if (surahQuery == sNorm || (surahQuery.length >= 3 && sNorm == surahQuery)) {
                        Log.d(TAG, "Explicit surah name recognized: ${surah.nameArabic}")
                        return VerseCandidate(surahNumber = surah.id, ayahNumber = 1, confidence = 1.0f)
                    }
                }
            }
        } else if (queryWords.size <= 2) {
            // User just said the isolated name of a surah in voice search/input (e.g. "الفاتحة", "البقرة")
            val isolatedName = normalized.trim()
            for (surah in surahCache) {
                val sNorm = ArabicNormalizer.normalize(surah.nameArabic)
                if (isolatedName == sNorm) {
                    Log.d(TAG, "Isolated surah name recognized: ${surah.nameArabic}")
                    return VerseCandidate(surahNumber = surah.id, ayahNumber = 1, confidence = 1.0f)
                }
            }
        }

        // 3. Transition Cue: Surah Al-Fatihah (new Rak'ah in prayer)
        if (preferredSurah != 1) {
            if (normalized.contains("الحمد لله رب العالمين")) {
                Log.d(TAG, "Detected Al-Fatihah transition cue (Ayah 2)")
                return VerseCandidate(surahNumber = 1, ayahNumber = 2, confidence = 1.0f)
            } else if (normalized.contains("اياك نعبد واياك نستعين")) {
                Log.d(TAG, "Detected Al-Fatihah transition cue (Ayah 5)")
                return VerseCandidate(surahNumber = 1, ayahNumber = 5, confidence = 1.0f)
            } else if (normalized.contains("اهدنا الصراط المستقيم")) {
                Log.d(TAG, "Detected Al-Fatihah transition cue (Ayah 6)")
                return VerseCandidate(surahNumber = 1, ayahNumber = 6, confidence = 1.0f)
            }
        }

        // 4. Transition Cue: Basmalah ("بسم الله الرحمن الرحيم") starting a new Surah after Al-Fatihah
        if (normalized.contains("بسم الله الرحمن الرحيم") || normalized.contains("بسم الله")) {
            val afterBasmalah = normalized
                .replace("بسم الله الرحمن الرحيم", "")
                .replace("بسم الله", "")
                .trim()
            if (afterBasmalah.length >= 4) {
                for (a in ayahCache) {
                    if (a.ayahNumber == 1 && a.surahNumber != 1 && a.surahNumber != 9) {
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
            // First: Use real-time verse segmentation to find verses present in this speech
            val segmented = segmentVerses(rawText, preferredSurah, preferredAyah)
            val inSurahMatches = segmented.filter { it.surahNumber == preferredSurah }

            if (inSurahMatches.isNotEmpty()) {
                // If verses at or ahead of preferredAyah matched, pick the most advanced one
                val forwardMatches = inSurahMatches.filter { it.ayahNumber >= preferredAyah }
                if (forwardMatches.isNotEmpty()) {
                    val latest = forwardMatches.maxByOrNull { it.ayahNumber }!!
                    Log.d(TAG, "Segmented forward progression in current surah ($preferredSurah:${latest.ayahNumber})")
                    return VerseCandidate(
                        surahNumber = preferredSurah,
                        ayahNumber = latest.ayahNumber,
                        confidence = 1.0f
                    )
                } else {
                    // All matches were earlier verses (e.g. repetition), pick the latest among them
                    val latest = inSurahMatches.maxByOrNull { it.ayahNumber }!!
                    Log.d(TAG, "Segmented verse match in current surah ($preferredSurah:${latest.ayahNumber})")
                    return VerseCandidate(
                        surahNumber = preferredSurah,
                        ayahNumber = latest.ayahNumber,
                        confidence = 1.0f
                    )
                }
            }

            // Step A: CURRENT ACTIVE AYAH RETENTION (For mid-verse recitations)
            val currentAyah = getCachedAyah(preferredSurah, preferredAyah)
            if (currentAyah != null) {
                val currentScore = calculateMatchScore(normalized, currentAyah)
                val hasOverlapWithCurrent = queryContent.isNotEmpty() && queryContent.any { currentAyah.contentWords.contains(it) }

                if (currentScore >= 0.20f || hasOverlapWithCurrent) {
                    Log.d(TAG, "Words match current ayah ($preferredSurah:$preferredAyah). Staying locked on current verse.")
                    return VerseCandidate(
                        surahNumber = preferredSurah,
                        ayahNumber = preferredAyah,
                        confidence = 1.0f
                    )
                }
            }

            // Step B: SEQUENTIAL FORWARD PROGRESSION IN THE SAME SURAH (+1 up to +15 ayahs)
            for (offset in 1..15) {
                val targetAyahNum = preferredAyah + offset
                val targetAyah = getCachedAyah(preferredSurah, targetAyahNum) ?: break
                val score = calculateMatchScore(normalized, targetAyah)
                val minThreshold = if (offset == 1) 0.30f else 0.40f

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
                    if (score >= 0.40f) {
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
            if (bestInSurah != null && bestInSurahScore >= 0.45f) {
                Log.d(TAG, "Matched within current surah ($preferredSurah:${bestInSurah.ayahNumber}) with score $bestInSurahScore.")
                return VerseCandidate(
                    surahNumber = preferredSurah,
                    ayahNumber = bestInSurah.ayahNumber,
                    confidence = bestInSurahScore
                )
            }

            // STRICT RESTRICTION: Retain current surah unless an entire distinct foreign verse was fully recited
            if (queryContent.size < 4) {
                Log.d(TAG, "Retaining current surah ($preferredSurah:$preferredAyah).")
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

                    if (score >= 0.90f && (verseCoverage >= 0.70f || commonWordsCount >= 6)) {
                        if (score > bestOtherScore) {
                            bestOtherScore = score
                            bestOtherAyah = ayah
                        }
                    }
                }
            }

            if (bestOtherAyah != null) {
                Log.d(TAG, "High-confidence external surah transition to: Surah ${bestOtherAyah.surahNumber}:${bestOtherAyah.ayahNumber}")
                return VerseCandidate(
                    surahNumber = bestOtherAyah.surahNumber,
                    ayahNumber = bestOtherAyah.ayahNumber,
                    confidence = bestOtherScore
                )
            }

            return VerseCandidate(
                surahNumber = preferredSurah,
                ayahNumber = preferredAyah,
                confidence = 0.80f
            )
        }

        // =========================================================================
        // GLOBAL INITIAL SEARCH (When no active position is anchored)
        // =========================================================================
        val globalSegmented = segmentVerses(rawText)
        if (globalSegmented.isNotEmpty()) {
            val latest = globalSegmented.last()
            return VerseCandidate(
                surahNumber = latest.surahNumber,
                ayahNumber = latest.ayahNumber,
                confidence = 1.0f
            )
        }

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
            null
        }
    }
}
