package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ArabicNormalizer
import com.example.data.local.AyahEntity
import com.example.data.local.PageEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ParchmentBorder
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.TextUthmani

@Composable
fun MushafPageView(
    pageNumber: Int,
    ayahs: List<AyahEntity>,
    pageInfo: PageEntity?,
    highlightedAyah: AyahEntity?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("mushaf_page_card_$pageNumber"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ParchmentLight
        ),
        border = BorderStroke(2.dp, ParchmentBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Ornate Inner Double Border (Madinah Mushaf style)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Page Header (Juz & Surah)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الجزء ${ArabicNormalizer.toArabicDigits(pageInfo?.juzNumber ?: 1)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )

                        // Surah Header medallion
                        Text(
                            text = "سورة ${pageInfo?.surahNameArabic ?: ""}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldDark
                        )
                    }

                    Divider(
                        color = GoldPrimary.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Page Content Scroll
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Check if a Surah starts on this page
                        val groupedBySurah = ayahs.groupBy { it.surahNumber }

                        for ((surahId, surahAyahs) in groupedBySurah) {
                            val firstAyah = surahAyahs.firstOrNull()
                            if (firstAyah != null && firstAyah.ayahNumber == 1) {
                                // Surah Banner
                                SurahHeaderBanner(surahName = pageInfo?.surahNameArabic ?: "")
                                Spacer(modifier = Modifier.height(6.dp))

                                // Bismillah (except Surah At-Tawbah 9 and Surah Al-Fatihah 1 which has Bismillah as Ayah 1)
                                if (surahId != 9 && surahId != 1) {
                                    Text(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            // Render Verses
                            val annotatedString = buildAnnotatedString {
                                for (ayah in surahAyahs) {
                                    val isHighlighted = highlightedAyah != null &&
                                            highlightedAyah.surahNumber == ayah.surahNumber &&
                                            highlightedAyah.ayahNumber == ayah.ayahNumber

                                    if (isHighlighted) {
                                        withStyle(
                                            style = SpanStyle(
                                                background = GoldPrimary.copy(alpha = 0.35f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(ayah.textUthmani)
                                        }
                                    } else {
                                        append(ayah.textUthmani)
                                    }

                                    // Verse End Symbol with Arabic digit ﴿١﴾
                                    append(" ")
                                    withStyle(
                                        style = SpanStyle(
                                            color = GoldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    ) {
                                        append(" ﴿${ArabicNormalizer.toArabicDigits(ayah.ayahNumber)}﴾ ")
                                    }
                                }
                            }

                            Text(
                                text = annotatedString,
                                fontSize = 19.sp,
                                lineHeight = 38.sp,
                                textAlign = TextAlign.Center,
                                color = TextUthmani,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Divider(
                        color = GoldPrimary.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // Page Footer (Page number in Arabic)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "— ",
                                fontSize = 13.sp,
                                color = GoldDark
                            )
                            Text(
                                text = ArabicNormalizer.toArabicDigits(pageNumber),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = " —",
                                fontSize = 13.sp,
                                color = GoldDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahHeaderBanner(surahName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EmeraldDark)
            .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "۞ ",
                color = GoldPrimary,
                fontSize = 16.sp
            )
            Text(
                text = "سُورَةُ $surahName",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " ۞",
                color = GoldPrimary,
                fontSize = 16.sp
            )
        }
    }
}
