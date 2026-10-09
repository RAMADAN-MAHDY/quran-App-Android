package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY id ASC")
    suspend fun getAllSurahsSync(): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE id = :surahId LIMIT 1")
    suspend fun getSurahById(surahId: Int): SurahEntity?

    @Query("SELECT * FROM ayahs ORDER BY id ASC")
    suspend fun getAllAyahsSync(): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE pageNumber = :pageNumber ORDER BY id ASC")
    fun getAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE pageNumber = :pageNumber ORDER BY id ASC")
    suspend fun getAyahsByPageSync(pageNumber: Int): List<AyahEntity>

    @Query("SELECT pageNumber FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getPageForAyah(surahNumber: Int, ayahNumber: Int): Int?

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE textNormalized LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchAyahs(query: String): List<AyahEntity>

    @Query("SELECT * FROM pages WHERE pageNumber = :pageNumber LIMIT 1")
    suspend fun getPageInfo(pageNumber: Int): PageEntity?

    @Query("SELECT * FROM pages ORDER BY pageNumber ASC")
    fun getAllPages(): Flow<List<PageEntity>>
}
