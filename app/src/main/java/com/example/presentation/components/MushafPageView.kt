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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ArabicNormalizer
import com.example.data.local.AyahEntity
import com.example.data.local.PageEntity
import com.example.data.local.SurahEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ParchmentBorder
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.TextUthmani

private val BASMALAH_UTHMANI_REGEX = Regex("^[\\uFEFF\\s]*ب[\\u064B-\\u065F]*سْمِ\\s+ٱللَّهِ\\s+ٱلرَّحْمَٰنِ\\s+ٱلرَّحِيمِ\\s*")

/**
 * Strips the leading Basmalah from Ayah 1's body text when rendered in the Mushaf,
 * because the decorative Basmalah is already displayed as a centered title header above the first Ayah.
 * Surah Al-Fatihah (Surah 1) retains its Basmalah since it is counted as Ayah 1.
 */
fun cleanAyahUthmaniText(ayah: AyahEntity): String {
    return if (ayah.surahNumber != 1 && ayah.ayahNumber == 1) {
        BASMALAH_UTHMANI_REGEX.replace(ayah.textUthmani, "").trim()
    } else {
        ayah.textUthmani.removePrefix("\uFEFF").trim()
    }
}

/**
 * Formats surah names for headers concisely.
 * When multiple surahs appear on a page (e.g. 3 or 4 surahs), it formats as:
 * "سورة الإخلاص - الناس" to prevent vertical line expansion and layout distortion.
 */
fun formatSurahsForHeader(names: List<String>): String {
    return when {
        names.isEmpty() -> ""
        names.size == 1 -> "سورة ${names[0]}"
        names.size == 2 -> "سورة ${names[0]} • ${names[1]}"
        else -> "سورة ${names.first()} - ${names.last()}"
    }
}

@Composable
fun MushafPageView(
    pageNumber: Int,
    ayahs: List<AyahEntity>,
    pageInfo: PageEntity?,
    highlightedAyah: AyahEntity?,
    surahs: List<SurahEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val surahMap = remember(surahs) { surahs.associateBy { it.id } }

    // Maps to track character offsets, text layouts, and vertical block positions for precise auto-scrolling
    val ayahStartOffsets = remember(ayahs) { mutableMapOf<Pair<Int, Int>, Int>() }
    val textLayoutResults = remember(ayahs) { mutableMapOf<Int, TextLayoutResult>() }
    val blockTopPositions = remember(ayahs) { mutableMapOf<Int, Float>() }

    // Always reset scroll to top when page changes
    LaunchedEffect(pageNumber) {
        scrollState.scrollTo(0)
    }

    // Auto-scroll to highlighted Ayah when highlightedAyah changes
    LaunchedEffect(highlightedAyah) {
        if (highlightedAyah != null) {
            val key = Pair(highlightedAyah.surahNumber, highlightedAyah.ayahNumber)
            val charOffset = ayahStartOffsets[key]
            val layoutResult = textLayoutResults[highlightedAyah.surahNumber]
            val blockTop = blockTopPositions[highlightedAyah.surahNumber] ?: 0f

            if (charOffset != null && layoutResult != null) {
                val line = layoutResult.getLineForOffset(charOffset)
                val lineTopY = layoutResult.getLineTop(line)
                val absoluteY = (blockTop + lineTopY).toInt()

                // Position the highlighted verse near 1/3 from top of scroll viewport
                val viewportHeight = if (scrollState.viewportSize > 0) scrollState.viewportSize else 800
                val targetScroll = (absoluteY - viewportHeight / 3).coerceIn(0, scrollState.maxValue)

                if (kotlin.math.abs(scrollState.value - targetScroll) > 30) {
                    scrollState.animateScrollTo(
                        targetScroll,
                        animationSpec = tween(durationMillis = 400)
                    )
                }
            } else {
                // Fallback: proportional scroll based on Ayah index if precise coordinates aren't ready
                val index = ayahs.indexOfFirst {
                    it.surahNumber == highlightedAyah.surahNumber && it.ayahNumber == highlightedAyah.ayahNumber
                }
                if (index >= 0 && ayahs.isNotEmpty() && scrollState.maxValue > 0) {
                    val fraction = index.toFloat() / ayahs.size.toFloat()
                    val targetScroll = (scrollState.maxValue * fraction).toInt()
                    if (kotlin.math.abs(scrollState.value - targetScroll) > 50) {
                        scrollState.animateScrollTo(
                            targetScroll,
                            animationSpec = tween(durationMillis = 400)
                        )
                    }
                }
            }
        }
    }

    // Aggregate all Surah names appearing on this page
    val surahIdsOnPage = remember(ayahs) { ayahs.map { it.surahNumber }.distinct() }
    val surahHeaderTitle = remember(surahIdsOnPage, surahMap, pageInfo) {
        val names = surahIdsOnPage.mapNotNull { surahMap[it]?.nameArabic }
        if (names.isNotEmpty()) {
            formatSurahsForHeader(names)
        } else {
            pageInfo?.surahNameArabic?.let { "سورة $it" } ?: ""
        }
    }

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
                    // Page Header (Juz & All Surahs on Page)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الجزء ${ArabicNormalizer.toArabicDigits(pageInfo?.juzNumber ?: 1)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Surah Header medallion showing all Surahs on this page
                        Text(
                            text = surahHeaderTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            textAlign = TextAlign.End
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
                                // Surah Banner with the exact surah name
                                val currentSurahName = surahMap[surahId]?.nameArabic ?: pageInfo?.surahNameArabic ?: ""
                                SurahHeaderBanner(surahName = currentSurahName)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Centered Basmalah header (except Surah At-Tawbah 9 and Surah Al-Fatihah 1)
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
                                    val key = Pair(ayah.surahNumber, ayah.ayahNumber)
                                    ayahStartOffsets[key] = length

                                    val isHighlighted = highlightedAyah != null &&
                                            highlightedAyah.surahNumber == ayah.surahNumber &&
                                            highlightedAyah.ayahNumber == ayah.ayahNumber

                                    val displayText = cleanAyahUthmaniText(ayah)
                                    if (isHighlighted) {
                                        withStyle(
                                            style = SpanStyle(
                                                background = GoldPrimary.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            append(displayText)
                                        }
                                    } else {
                                        append(displayText)
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
                                onTextLayout = { layoutResult ->
                                    textLayoutResults[surahId] = layoutResult
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .onGloballyPositioned { coordinates ->
                                        blockTopPositions[surahId] = coordinates.positionInParent().y
                                    }
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
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EmeraldDark)
            .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "۞ ",
                color = GoldPrimary,
                fontSize = 15.sp
            )
            Text(
                text = "سُورَةُ $surahName",
                color = Color.White,
                fontSize = 16.sp,
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
