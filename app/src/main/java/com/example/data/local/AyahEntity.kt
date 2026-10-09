package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ayahs",
    indices = [
        Index(value = ["surahNumber", "ayahNumber"]),
        Index(value = ["pageNumber"]),
        Index(value = ["textNormalized"])
    ]
)
data class AyahEntity(
    @PrimaryKey val id: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val textUthmani: String,
    val textNormalized: String
)
