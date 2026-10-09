package com.example

import com.example.core.ArabicNormalizer
import com.example.domain.recognition.VerseCandidate
import com.example.domain.recognition.VerseStabilizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testArabicNormalizer_stripsTashkeelAndNormalizes() {
        val input = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val normalized = ArabicNormalizer.normalize(input)
        assertEquals("بسم الله الرحمن الرحيم", normalized)
    }

    @Test
    fun testArabicNormalizer_toArabicDigits() {
        assertEquals("١", ArabicNormalizer.toArabicDigits(1))
        assertEquals("٤٢", ArabicNormalizer.toArabicDigits(42))
        assertEquals("٢٥٥", ArabicNormalizer.toArabicDigits(255))
        assertEquals("٦٠٤", ArabicNormalizer.toArabicDigits(604))
    }

    @Test
    fun testVerseStabilizer_acceptsFirstCandidateImmediately() {
        val stabilizer = VerseStabilizer()
        val firstCandidate = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 2, ayahNumber = 255, confidence = 0.95f)
        )
        assertNotNull(firstCandidate)
        assertEquals(2, firstCandidate?.surahNumber)
        assertEquals(255, firstCandidate?.ayahNumber)
    }

    @Test
    fun testVerseStabilizer_setVerseAnchorsPosition() {
        val stabilizer = VerseStabilizer()
        // Page 1 navigation sets anchor to Al-Fatihah 1:1
        stabilizer.setVerse(surahNumber = 1, ayahNumber = 1)
        val current = stabilizer.getCurrentVerse()
        assertNotNull(current)
        assertEquals(1, current?.surahNumber)
        assertEquals(1, current?.ayahNumber)

        // Attempting erratic low-confidence jump to Surah Saad (38:1) must be rejected!
        val result = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 38, ayahNumber = 1, confidence = 0.80f)
        )
        assertNotNull(result)
        assertEquals(1, result?.surahNumber)
        assertEquals(1, result?.ayahNumber)
    }

    @Test
    fun testVerseStabilizer_sequentialAyahFollowsSmoothly() {
        val stabilizer = VerseStabilizer()

        val ayah1 = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 1, ayahNumber = 1, confidence = 0.90f)
        )
        assertNotNull(ayah1)
        assertEquals(1, ayah1?.ayahNumber)

        val ayah2 = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 1, ayahNumber = 2, confidence = 0.90f)
        )
        assertNotNull(ayah2)
        assertEquals(2, ayah2?.ayahNumber)
    }

    @Test
    fun testVerseStabilizer_staysLockedOnSameAyah() {
        val stabilizer = VerseStabilizer()
        // Lock on Al-Anbiya (21:5)
        stabilizer.processCandidate(VerseCandidate(surahNumber = 21, ayahNumber = 5, confidence = 1.0f))

        // Same ayah candidate arrives again
        val locked = stabilizer.processCandidate(VerseCandidate(surahNumber = 21, ayahNumber = 5, confidence = 0.85f))
        assertNotNull(locked)
        assertEquals(21, locked?.surahNumber)
        assertEquals(5, locked?.ayahNumber)
    }

    @Test
    fun testVerseStabilizer_blocksErraticSurahJump() {
        val stabilizer = VerseStabilizer()
        // Active recitation in Al-Anbiya (21:5)
        stabilizer.processCandidate(VerseCandidate(surahNumber = 21, ayahNumber = 5, confidence = 1.0f))

        // Accidental low-confidence or partial match candidate proposing Surah Qaf (50:2)
        val result = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 50, ayahNumber = 2, confidence = 0.75f)
        )
        assertNotNull(result)
        // Must stay solidly on Al-Anbiya (21:5) and reject jump to Qaf!
        assertEquals(21, result?.surahNumber)
        assertEquals(5, result?.ayahNumber)
    }

    @Test
    fun testVerseStabilizer_allowsFatihahTransition() {
        val stabilizer = VerseStabilizer()
        // Active in another surah
        stabilizer.processCandidate(VerseCandidate(surahNumber = 21, ayahNumber = 10, confidence = 1.0f))

        // Fatihah recited in next rak'ah
        Thread.sleep(700) // Pass debounce
        val fatihah = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 1, ayahNumber = 1, confidence = 0.90f)
        )
        assertNotNull(fatihah)
        assertEquals(1, fatihah?.surahNumber)
        assertEquals(1, fatihah?.ayahNumber)
    }

    @Test
    fun testVerseStabilizer_allowsVerifiedFullVerseTransition() {
        val stabilizer = VerseStabilizer()
        // Active in Al-Anbiya
        stabilizer.processCandidate(VerseCandidate(surahNumber = 21, ayahNumber = 10, confidence = 1.0f))

        // High confidence verified full-verse transition (confidence >= 0.95f)
        Thread.sleep(700) // Pass debounce
        val fullVerse = stabilizer.processCandidate(
            VerseCandidate(surahNumber = 50, ayahNumber = 2, confidence = 1.0f)
        )
        assertNotNull(fullVerse)
        assertEquals(50, fullVerse?.surahNumber)
        assertEquals(2, fullVerse?.ayahNumber)
    }
}
