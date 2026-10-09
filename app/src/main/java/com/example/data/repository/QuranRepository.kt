package com.example.data.repository

import com.example.data.local.AyahEntity
import com.example.data.local.PageEntity
import com.example.data.local.QuranDao
import com.example.data.local.SurahEntity
import kotlinx.coroutines.flow.Flow

class QuranRepository(private val quranDao: QuranDao) {
    val allSurahs: Flow<List<SurahEntity>> = quranDao.getAllSurahs()
    val allPages: Flow<List<PageEntity>> = quranDao.getAllPages()

    suspend fun getAllSurahsSync(): List<SurahEntity> =
        quranDao.getAllSurahsSync()

    suspend fun getAllAyahsSync(): List<AyahEntity> =
        quranDao.getAllAyahsSync()

    fun getAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>> =
        quranDao.getAyahsByPage(pageNumber)

    suspend fun getAyahsByPageSync(pageNumber: Int): List<AyahEntity> =
        quranDao.getAyahsByPageSync(pageNumber)

    suspend fun getPageForAyah(surahNumber: Int, ayahNumber: Int): Int? =
        quranDao.getPageForAyah(surahNumber, ayahNumber)

    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): AyahEntity? =
        quranDao.getAyah(surahNumber, ayahNumber)

    suspend fun getSurah(surahId: Int): SurahEntity? =
        quranDao.getSurahById(surahId)

    suspend fun getPageInfo(pageNumber: Int): PageEntity? =
        quranDao.getPageInfo(pageNumber)

    suspend fun search(query: String): List<AyahEntity> =
        quranDao.searchAyahs(query)
}
