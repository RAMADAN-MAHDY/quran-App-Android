package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val ayahsCount: Int,
    val startPage: Int,
    val revelationType: String
)
