package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.service.TrackingState
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldPrimary

@Composable
fun AyahOverlay(
    trackingState: TrackingState,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onStopTracking: () -> Unit,
    onOpenSimulator: () -> Unit,
    onDirectRecitation: (String) -> Unit,
    onToggleTouchLock: () -> Unit,
    isCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    onNextAyah: () -> Unit = {},
    onPrevAyah: () -> Unit = {},
    onVoiceInputRequested: () -> Unit = {},
    onNavigateToPage: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = trackingState.isTracking,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .animateContentSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isCollapsed) {
                // Sleek collapsed pill: Leaves full Quran page completely visible and unobstructed
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onToggleCollapse() }
                        .testTag("collapsed_overlay_pill"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldDark.copy(alpha = 0.96f)
                    ),
                    border = BorderStroke(1.dp, GoldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live pulse indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (trackingState.audioLevel > 0.05f) Color(0xFF4CAF50) else GoldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (trackingState.currentSurah != null && trackingState.currentAyah != null) {
                                "سورة ${trackingState.currentSurah.nameArabic} (${trackingState.currentAyah.ayahNumber})"
                            } else {
                                "متابعة التلاوة نشطة"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "|",
                            color = GoldPrimary.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "إنزال التبويب وعرض النطق",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "عرض التبويب والنطق ▼",
                                color = GoldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Full Expanded Overlay Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ayah_overlay_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldDark.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Top Row: Status, Mic Waveform, Controls & Collapse Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Pulsating Mic indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.25f + (trackingState.audioLevel * 0.75f))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "ميكروفون التتبع",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "متابعة تلاوة الإمام",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Live pulse dot
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (trackingState.audioLevel > 0.05f) Color(0xFF4CAF50) else GoldPrimary)
                                        )
                                    }
                                    Text(
                                        text = trackingState.statusText,
                                        color = Color(0xFFD0EBDD),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Action buttons (Collapse + Lock toggle + Stop)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Collapse button to hide overlay up and reveal full Quran page
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF133E2B),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clickable { onToggleCollapse() }
                                        .testTag("collapse_overlay_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "إخفاء التبويب لرؤية المصحف بالكامل",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "إخفاء ▲",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = onToggleTouchLock,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("toggle_touch_lock_button")
                                ) {
                                    Icon(
                                        imageVector = if (trackingState.isTouchLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "قفل اللمس",
                                        tint = if (trackingState.isTouchLocked) GoldPrimary else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = onStopTracking,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("stop_tracking_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إيقاف المتابعة",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Transcribed Speech Display Box (shows live captured words clearly)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF091C12))
                                .border(
                                    1.dp,
                                    if (trackingState.lastRecognizedSpeech.isNotBlank()) GoldPrimary else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "صوت التلاوة الملتقطة:",
                                        color = GoldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (trackingState.audioLevel > 0.05f) "● جاري التقاط الصوت" else "● استماع الميكروفون",
                                        color = if (trackingState.audioLevel > 0.05f) Color(0xFF4CAF50) else Color(0xFFB0BEC5),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (trackingState.lastRecognizedSpeech.isNotBlank()) {
                                        "« ${trackingState.lastRecognizedSpeech} »"
                                    } else {
                                        "« في انتظار صوت التلاوة عبر الميكروفون أو اضغط زر المايك المباشر بالأسفل... »"
                                    },
                                    color = if (trackingState.lastRecognizedSpeech.isNotBlank()) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = if (trackingState.lastRecognizedSpeech.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        // Recognized Ayah & Page Banner
                        if (trackingState.currentSurah != null && trackingState.currentAyah != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF133E2B))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onPrevAyah,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("◄", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToPage(trackingState.currentPage) }
                                ) {
                                    Text(
                                        text = "سورة ${trackingState.currentSurah.nameArabic} — آية ${ArabicNormalizer.toArabicDigits(trackingState.currentAyah.ayahNumber)}",
                                        color = GoldPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "صفحة المصحف: ${ArabicNormalizer.toArabicDigits(trackingState.currentPage)} (الجزء ${ArabicNormalizer.toArabicDigits(trackingState.currentAyah.juzNumber)})",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = onNextAyah,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("►", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Audio level waveform bar
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(trackingState.audioLevel.coerceIn(0.05f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(GoldPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Recitation Presets Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickReciteChip(label = "الفاتحة (ص١)") { onDirectRecitation("الحمد لله رب العالمين") }
                            QuickReciteChip(label = "آية الكرسي (ص٤٢)") { onDirectRecitation("الله لا اله الا هو الحي القيوم") }
                            QuickReciteChip(label = "الكهف (ص٢٩٣)") { onDirectRecitation("الحمد لله الذي انزل على عبده الكتاب") }
                            QuickReciteChip(label = "يس (ص٤٤٠)") { onDirectRecitation("يس والقران الحكيم") }
                            QuickReciteChip(label = "الملك (ص٥٦٢)") { onDirectRecitation("تبارك الذي بيده الملك") }
                            QuickReciteChip(label = "الإخلاص (ص٦٠٤)") { onDirectRecitation("قل هو الله احد") }

                            FilledTonalButton(
                                onClick = onOpenSimulator,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("مزيد...", fontSize = 11.sp)
                            }
                        }

                        // Input Bar with Direct Voice Trigger
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Direct Voice Button (Mic button that triggers Google Voice Dialog)
                            IconButton(
                                onClick = onVoiceInputRequested,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                                    .testTag("voice_trigger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "انطق الآية بصوتك",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = onInputTextChange,
                                placeholder = { Text("اكتب أو انطق آية للانتقال المباشر...", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0C2016),
                                    unfocusedContainerColor = Color(0xFF0C2016)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_recite_text_field")
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onDirectRecitation(inputText)
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary)
                                    .testTag("quick_recite_submit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "تعرف على الآية",
                                    tint = Color.Black,
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

@Composable
private fun QuickReciteChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF163826))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
