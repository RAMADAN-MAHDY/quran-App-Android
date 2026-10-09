package com.example.presentation

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ArabicNormalizer
import com.example.presentation.components.AyahOverlay
import com.example.presentation.components.MushafPageView
import com.example.presentation.components.RecitationSimulatorDialog
import com.example.presentation.components.SearchView
import com.example.presentation.components.SurahIndexView
import com.example.presentation.components.TouchLockOverlay
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ParchmentLight

@Composable
fun MushafScreen(
    viewModel: MushafViewModel,
    onRequestRecordAudioPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val currentAyahs by viewModel.currentAyahs.collectAsStateWithLifecycle()
    val pageInfo by viewModel.pageInfo.collectAsStateWithLifecycle()
    val allSurahs by viewModel.allSurahs.collectAsStateWithLifecycle()
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val quickInputText by viewModel.quickInputText.collectAsStateWithLifecycle()
    val isSimulatorOpen by viewModel.isSimulatorOpen.collectAsStateWithLifecycle()

    var isOverlayCollapsed by rememberSaveable { mutableStateOf(false) }
    var isVoiceForSearch by remember { mutableStateOf(false) }

    val voicePromptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                viewModel.handleVoiceRecitation(text, fromSearch = isVoiceForSearch)
            }
        }
    }

    val launchVoiceRecognizer: (forSearch: Boolean) -> Unit = { forSearch ->
        isVoiceForSearch = forSearch
        onRequestRecordAudioPermission()
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (forSearch) "انطق كلمات البحث..." else "انطق آية أو جزء من تلاوة الإمام...")
            }
            voicePromptLauncher.launch(intent)
        } catch (e: Exception) {
            viewModel.openSimulator()
        }
    }

    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!trackingState.isTracking) {
                NavigationBar(
                    containerColor = EmeraldDark,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(Icons.Default.Book, contentDescription = "المصحف") },
                        label = { Text("المصحف", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("tab_mushaf")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "فهرس السور") },
                        label = { Text("السور", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("tab_surahs")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "البحث") },
                        label = { Text("البحث", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("tab_search")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ParchmentLight)
        ) {
            when (selectedTab) {
                0 -> {
                    // Mushaf View
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Aggregate all Surah names appearing on current page
                        val surahsOnPage = remember(currentAyahs, allSurahs, pageInfo) {
                            val ids = currentAyahs.map { it.surahNumber }.distinct()
                            val surahMap = allSurahs.associateBy { it.id }
                            val names = ids.mapNotNull { surahMap[it]?.nameArabic }
                            if (names.isNotEmpty()) {
                                names.joinToString(" • ") { "سورة $it" }
                            } else {
                                pageInfo?.surahNameArabic?.let { "سورة $it" } ?: ""
                            }
                        }

                        // Top Action & Status Bar
                        MushafTopBar(
                            currentPage = currentPage,
                            surahName = surahsOnPage,
                            juzNumber = pageInfo?.juzNumber ?: 1,
                            isTracking = trackingState.isTracking,
                            onToggleAutoFollow = {
                                if (trackingState.isTracking) {
                                    viewModel.stopAutoFollow()
                                } else {
                                    onRequestRecordAudioPermission()
                                    viewModel.startAutoFollow()
                                }
                            },
                            onJumpToPageClicked = { showJumpDialog = true }
                        )

                        // Main Page Content with horizontal swipe gesture support
                        var totalDragX by remember { mutableFloatStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(currentPage) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (totalDragX < -60f) {
                                                // Swipe left -> advance to next page in Arabic Mushaf
                                                viewModel.nextPage()
                                            } else if (totalDragX > 60f) {
                                                // Swipe right -> return to previous page in Arabic Mushaf
                                                viewModel.previousPage()
                                            }
                                            totalDragX = 0f
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragX += dragAmount
                                        }
                                    )
                                }
                        ) {
                            MushafPageView(
                                pageNumber = currentPage,
                                ayahs = currentAyahs,
                                pageInfo = pageInfo,
                                highlightedAyah = if (trackingState.isTracking) trackingState.currentAyah else null,
                                surahs = allSurahs
                            )
                        }

                        // Bottom Navigation Scrubber (only visible in normal mode)
                        if (!trackingState.isTracking) {
                            MushafBottomControls(
                                currentPage = currentPage,
                                onPageChange = { viewModel.loadPage(it) },
                                onNext = { viewModel.nextPage() },
                                onPrevious = { viewModel.previousPage() }
                            )
                        }
                    }
                }
                1 -> {
                    // Surah Index View
                    SurahIndexView(
                        surahs = allSurahs,
                        onSurahSelected = { page ->
                            viewModel.loadPage(page)
                            viewModel.selectTab(0)
                        }
                    )
                }
                2 -> {
                    // Search View
                    SearchView(
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        results = searchResults,
                        onAyahSelected = { page ->
                            viewModel.loadPage(page)
                            viewModel.selectTab(0)
                        },
                        onVoiceSearchRequested = {
                            launchVoiceRecognizer(true)
                        }
                    )
                }
            }

            // Floating Ayah Overlay in Auto Follow Mode
            AyahOverlay(
                trackingState = trackingState,
                inputText = quickInputText,
                onInputTextChange = { viewModel.setQuickInputText(it) },
                onStopTracking = { viewModel.stopAutoFollow() },
                onOpenSimulator = { viewModel.openSimulator() },
                onDirectRecitation = { text -> viewModel.handleVoiceRecitation(text, fromSearch = false) },
                onToggleTouchLock = { viewModel.toggleTouchLock() },
                isCollapsed = isOverlayCollapsed,
                onToggleCollapse = { isOverlayCollapsed = !isOverlayCollapsed },
                onNextAyah = { viewModel.advanceNextAyah() },
                onPrevAyah = { viewModel.advancePreviousAyah() },
                onVoiceInputRequested = {
                    launchVoiceRecognizer(false)
                },
                onNavigateToPage = { page ->
                    viewModel.loadPage(page)
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Touch Lock Barrier (blocks page swipe/touches during prayer)
            TouchLockOverlay(
                isLocked = trackingState.isTracking && trackingState.isTouchLocked,
                onUnlock = { viewModel.unlockTouch() }
            )

            // Recitation Simulator Dialog
            if (isSimulatorOpen) {
                RecitationSimulatorDialog(
                    onDismiss = { viewModel.closeSimulator() },
                    onSimulateCandidate = { surah, ayah ->
                        viewModel.simulateCandidate(surah, ayah)
                    },
                    onSimulateText = { text ->
                        viewModel.handleVoiceRecitation(text, fromSearch = false)
                    }
                )
            }

            // Jump to Page Dialog
            if (showJumpDialog) {
                AlertDialog(
                    onDismissRequest = { showJumpDialog = false },
                    title = { Text("الانتقال إلى صفحة بالمصحف", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("أدخل رقم الصفحة من ١ إلى ٦٠٤:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = jumpPageInput,
                                onValueChange = { jumpPageInput = it.filter { ch -> ch.isDigit() } },
                                placeholder = { Text("مثال: 42") },
                                singleLine = true,
                                modifier = Modifier.testTag("jump_page_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val p = jumpPageInput.toIntOrNull()
                                if (p != null && p in 1..604) {
                                    viewModel.loadPage(p)
                                    showJumpDialog = false
                                    jumpPageInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("انتقال")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MushafTopBar(
    currentPage: Int,
    surahName: String,
    juzNumber: Int,
    isTracking: Boolean,
    onToggleAutoFollow: () -> Unit,
    onJumpToPageClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmeraldDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = if (surahName.startsWith("سورة")) surahName else "سورة $surahName",
                color = GoldPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "صفحة ${ArabicNormalizer.toArabicDigits(currentPage)} • جزء ${ArabicNormalizer.toArabicDigits(juzNumber)}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Jump to page icon
            IconButton(
                onClick = onJumpToPageClicked,
                modifier = Modifier.testTag("jump_to_page_button")
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = "انتقال إلى صفحة",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Auto Follow Toggle Button
            Button(
                onClick = onToggleAutoFollow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) Color(0xFFD32F2F) else GoldPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("toggle_auto_follow_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isTracking) Color.White else Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTracking) "إيقاف المتابعة" else "متابعة التلاوة",
                    color = if (isTracking) Color.White else Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MushafBottomControls(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0EADB))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // In RTL Arabic reading:
                // Start item (placed on the RIGHT of screen) is PREVIOUS (السابق - العودة لليمين لصفحة أقل)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("prev_page_button")
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = currentPage > 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "الصفحة السابقة",
                            tint = if (currentPage > 1) EmeraldDark else Color.LightGray
                        )
                    }
                    Text(
                        text = "السابق",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentPage > 1) EmeraldDark else Color.LightGray
                    )
                }

                Text(
                    text = "صفحة ${ArabicNormalizer.toArabicDigits(currentPage)} من ٦٠٤",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )

                // End item in RTL Row (placed on the LEFT of screen) is NEXT (التالي - التقدم لليسار لصفحة أكبر)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("next_page_button")
                ) {
                    Text(
                        text = "التالي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentPage < 604) EmeraldDark else Color.LightGray
                    )
                    IconButton(
                        onClick = onNext,
                        enabled = currentPage < 604
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الصفحة التالية",
                            tint = if (currentPage < 604) EmeraldDark else Color.LightGray
                        )
                    }
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onPageChange(sliderValue.toInt()) },
                valueRange = 1f..604f,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldDark,
                    activeTrackColor = EmeraldPrimary,
                    inactiveTrackColor = Color.LightGray
                ),
                modifier = Modifier.testTag("page_slider")
            )
        }
    }
}
