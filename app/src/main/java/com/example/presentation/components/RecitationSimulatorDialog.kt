package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.ArabicNormalizer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldPrimary

data class RecitationSample(
    val title: String,
    val surahName: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val targetPage: Int,
    val textSnippet: String
)

@Composable
fun RecitationSimulatorDialog(
    onDismiss: () -> Unit,
    onSimulateCandidate: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onSimulateText: (String) -> Unit
) {
    var customText by remember { mutableStateOf("") }

    val samples = listOf(
        RecitationSample(
            title = "فاتحة الكتاب",
            surahName = "الفاتحة",
            surahNumber = 1,
            ayahNumber = 1,
            targetPage = 1,
            textSnippet = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ"
        ),
        RecitationSample(
            title = "آية الكرسي",
            surahName = "البقرة",
            surahNumber = 2,
            ayahNumber = 255,
            targetPage = 42,
            textSnippet = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌۭ وَلَا نَوْمٌۭ"
        ),
        RecitationSample(
            title = "فاتحة سورة الكهف",
            surahName = "الكهف",
            surahNumber = 18,
            ayahNumber = 1,
            targetPage = 293,
            textSnippet = "ٱلْحَمْدُ لِلَّهِ ٱلَّذِىٓ أَنزَلَ عَلَىٰ عَبْدِهِ ٱلْكِتَٰبَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَا"
        ),
        RecitationSample(
            title = "فاتحة سورة يس",
            surahName = "يس",
            surahNumber = 36,
            ayahNumber = 1,
            targetPage = 440,
            textSnippet = "يسٓ وَٱلْقُرْءَانِ ٱلْحَكِيمِ إِنَّكَ لَمِنَ ٱلْمُرْسَلِينَ"
        ),
        RecitationSample(
            title = "فاتحة سورة الملك",
            surahName = "الملك",
            surahNumber = 67,
            ayahNumber = 1,
            targetPage = 562,
            textSnippet = "تَبَٰرَكَ ٱلَّذِى بِيَدِهِ ٱلْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَىْءٍۢ قَدِيرٌ"
        ),
        RecitationSample(
            title = "سورة الإخلاص",
            surahName = "الإخلاص",
            surahNumber = 112,
            ayahNumber = 1,
            targetPage = 604,
            textSnippet = "قُلْ هُوَ ٱللَّهُ أَحَدٌ ٱللَّهُ ٱلصَّمَدُ لَمْ يَلِدْ وَلَمْ يُولَدْ"
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("recitation_simulator_dialog"),
            color = Color(0xFF0F1E17),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "محاكاة تلاوة الإمام",
                            color = GoldPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اختبر الانتقال التلقائي للآيات والصفحات",
                            color = Color(0xFFD0EBDD),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom text input
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("اكتب أو الصق آية للتجربة", fontSize = 12.sp) },
                    placeholder = { Text("مثال: الله لا إله إلا هو الحي القيوم", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_recitation_input")
                )

                if (customText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onSimulateText(customText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_custom_recitation_button")
                    ) {
                        Text("مطابقة الآية والانتقال إليها", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "نماذج تلاوات شائعة:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(samples) { sample ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSimulateCandidate(sample.surahNumber, sample.ayahNumber)
                                    onDismiss()
                                }
                                .testTag("sample_${sample.surahNumber}_${sample.ayahNumber}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF162D22)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sample.title,
                                            color = GoldPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(صفحة ${ArabicNormalizer.toArabicDigits(sample.targetPage)})",
                                            color = Color(0xFFA5D6A7),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sample.textSnippet,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
