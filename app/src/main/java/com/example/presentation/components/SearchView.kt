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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.local.AyahEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.ParchmentSurface

@Composable
fun SearchView(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    results: List<AyahEntity>,
    onAyahSelected: (pageNumber: Int) -> Unit,
    onVoiceSearchRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ParchmentLight)
            .padding(12.dp)
            .testTag("search_view")
    ) {
        // Search Input with Mic button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_input"),
                placeholder = { Text("ابحث عن أي كلمة أو آية بالقرآن الكريم...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = EmeraldPrimary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = Color.Gray
                                )
                            }
                        }
                        IconButton(
                            onClick = onVoiceSearchRequested,
                            modifier = Modifier.testTag("search_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "بحث صوتي",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = Color(0xFFD6C8A8),
                    focusedContainerColor = ParchmentSurface,
                    unfocusedContainerColor = ParchmentSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isNotBlank()) {
            Text(
                text = "نتائج البحث (${ArabicNormalizer.toArabicDigits(results.size)} آية):",
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        if (results.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم العثور على آيات مطابقة للبحث",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { ayah ->
                    SearchResultCard(
                        ayah = ayah,
                        onClick = { onAyahSelected(ayah.pageNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    ayah: AyahEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_result_item_${ayah.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ParchmentSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "آية ${ArabicNormalizer.toArabicDigits(ayah.ayahNumber)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )

                Text(
                    text = "صفحة ${ArabicNormalizer.toArabicDigits(ayah.pageNumber)} (جزء ${ArabicNormalizer.toArabicDigits(ayah.juzNumber)})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ayah.textUthmani,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = Color(0xFF1E1E1C)
            )
        }
    }
}
