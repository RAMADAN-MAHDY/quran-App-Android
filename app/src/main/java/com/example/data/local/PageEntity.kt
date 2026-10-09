package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val pageNumber: Int,
    val juzNumber: Int,
    val surahNumber: Int,
    val surahNameArabic: String,
    val startAyah: Int,
    val endAyah: Int
)
