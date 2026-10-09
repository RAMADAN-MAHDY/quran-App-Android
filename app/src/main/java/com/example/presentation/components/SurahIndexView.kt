package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ArabicNormalizer
import com.example.data.local.SurahEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.ParchmentSurface

@Composable
fun SurahIndexView(
    surahs: List<SurahEntity>,
    onSurahSelected: (startPage: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ParchmentLight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("surah_index_view")
    ) {
        Text(
            text = "فهرس سور القرآن الكريم (١١٤ سورة)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldDark,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(surahs, key = { it.id }) { surah ->
                SurahItemCard(surah = surah, onClick = { onSurahSelected(surah.startPage) })
            }
        }
    }
}

@Composable
private fun SurahItemCard(
    surah: SurahEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ParchmentSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Surah Number medallion
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ArabicNormalizer.toArabicDigits(surah.id),
                        color = GoldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "سورة ${surah.nameArabic}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${surah.nameEnglish} • ${surah.revelationType}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Page info & Ayah count
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "صفحة ${ArabicNormalizer.toArabicDigits(surah.startPage)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${ArabicNormalizer.toArabicDigits(surah.ayahsCount)} آية",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
