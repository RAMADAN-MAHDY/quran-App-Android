package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay

@Composable
fun TouchLockOverlay(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLocked) return

    var isPressing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPressing) {
        if (isPressing) {
            val totalSteps = 40
            val stepTimeMs = 50L // 40 * 50ms = 2000ms (2 seconds)
            for (i in 1..totalSteps) {
                delay(stepTimeMs)
                progress = i.toFloat() / totalSteps.toFloat()
            }
            // Completed 2 seconds!
            onUnlock()
            isPressing = false
            progress = 0f
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Intercept all taps across the screen so nothing gets touched by accident
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { /* Block accidental taps */ },
                    onDoubleTap = { /* Block */ },
                    onLongPress = { /* Block */ }
                )
            }
            .testTag("touch_lock_fullscreen_barrier"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Subtle hint and unlock mechanism at bottom
        Surface(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(0.9f)
                .testTag("touch_lock_card"),
            shape = RoundedCornerShape(24.dp),
            color = EmeraldDark.copy(alpha = 0.95f),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل اللمس",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "الشاشة مقفلة لمنع التقليب",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط مطولاً ٢ ث على الزر للفتح",
                            color = Color(0xFFC8E6C9),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // The 2-second hold-to-unlock button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isPressing) GoldPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressing = true
                                    tryAwaitRelease()
                                    isPressing = false
                                }
                            )
                        }
                        .testTag("hold_to_unlock_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (progress > 0f) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = GoldPrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { 0f },
                            color = Color.Transparent,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isPressing) "${(progress * 100).toInt()}%" else "اضغط\nمطولاً",
                            color = if (isPressing) GoldPrimary else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}
