package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QuranDatabase
import com.example.data.repository.QuranRepository
import com.example.domain.recognition.RecitationMatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("متابعة التلاوة", appName)
    }

    @Test
    fun `recitation matcher does not redirect isolated word An-Nas to Surah 114 when active in Surah 2`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = QuranDatabase.getDatabase(context)
        val repo = QuranRepository(db.quranDao())
        val matcher = RecitationMatcher(repo)
        matcher.warmUp()

        // When reading in Surah Al-Baqarah (Surah 2, Ayah 8) "ومن الناس من يقول آمنا بالله"
        val candidate = matcher.matchText("ومن الناس من يقول امنا بالله", preferredSurah = 2, preferredAyah = 8)
        assertNotNull(candidate)
        assertEquals(2, candidate?.surahNumber)
        assertEquals(8, candidate?.ayahNumber)
        assertNotEquals(114, candidate?.surahNumber)
    }

    @Test
    fun `recitation matcher does not redirect surah name query to surah 114 as a surah match`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = QuranDatabase.getDatabase(context)
        val repo = QuranRepository(db.quranDao())
        val matcher = RecitationMatcher(repo)
        matcher.warmUp()

        // When active position is anchored at Surah 2 Ayah 8
        val candidate = matcher.matchText("الناس", preferredSurah = 2, preferredAyah = 8)
        assertNotNull(candidate)
        // Must stay in Surah 2 and not divert to Surah 114
        assertEquals(2, candidate?.surahNumber)
        assertEquals(8, candidate?.ayahNumber)
    }
}

