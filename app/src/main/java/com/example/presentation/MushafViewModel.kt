package com.example.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ArabicNormalizer
import com.example.data.local.AyahEntity
import com.example.data.local.PageEntity
import com.example.data.local.QuranDatabase
import com.example.data.local.SurahEntity
import com.example.data.repository.QuranRepository
import com.example.domain.recognition.RecitationMatcher
import com.example.service.TrackingController
import com.example.service.TrackingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MushafViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuranRepository
    private val matcher: RecitationMatcher
    private val prefs = application.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    private val _currentPage = MutableStateFlow(prefs.getInt("last_page", 1))
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _currentAyahs = MutableStateFlow<List<AyahEntity>>(emptyList())
    val currentAyahs: StateFlow<List<AyahEntity>> = _currentAyahs.asStateFlow()

    private val _pageInfo = MutableStateFlow<PageEntity?>(null)
    val pageInfo: StateFlow<PageEntity?> = _pageInfo.asStateFlow()

    val allSurahs: StateFlow<List<SurahEntity>>

    val trackingState: StateFlow<TrackingState> = TrackingController.state

    private val _selectedTab = MutableStateFlow(0) // 0 = Mushaf, 1 = Surahs, 2 = Search
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AyahEntity>>(emptyList())
    val searchResults: StateFlow<List<AyahEntity>> = _searchResults.asStateFlow()

    private val _quickInputText = MutableStateFlow("")
    val quickInputText: StateFlow<String> = _quickInputText.asStateFlow()

    private val _isSimulatorOpen = MutableStateFlow(false)
    val isSimulatorOpen: StateFlow<Boolean> = _isSimulatorOpen.asStateFlow()

    init {
        val db = QuranDatabase.getDatabase(application)
        repository = QuranRepository(db.quranDao())
        matcher = RecitationMatcher(repository)
        TrackingController.initialize(application)

        allSurahs = repository.allSurahs.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        loadPage(_currentPage.value)

        // Observe direct navigation events from recognition
        viewModelScope.launch {
            TrackingController.navigationEvents.collectLatest { page ->
                loadPage(page)
                _selectedTab.value = 0 // Return to Mushaf view to see the recognized verse
            }
        }

        // Also sync state updates
        viewModelScope.launch {
            TrackingController.state.collectLatest { state ->
                if (state.currentPage != _currentPage.value) {
                    loadPage(state.currentPage)
                }
            }
        }
    }

    fun loadPage(page: Int) {
        val validPage = page.coerceIn(1, 604)
        _currentPage.value = validPage
        prefs.edit().putInt("last_page", validPage).apply()
        TrackingController.setCurrentPage(validPage)

        viewModelScope.launch(Dispatchers.IO) {
            val ayahs = repository.getAyahsByPageSync(validPage)
            val info = repository.getPageInfo(validPage)
            _currentAyahs.value = ayahs
            _pageInfo.value = info
        }
    }

    fun nextPage() {
        if (_currentPage.value < 604) {
            loadPage(_currentPage.value + 1)
        }
    }

    fun previousPage() {
        if (_currentPage.value > 1) {
            loadPage(_currentPage.value - 1)
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setQuickInputText(text: String) {
        _quickInputText.value = text
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = ArabicNormalizer.normalize(query.trim())
            val results = repository.search(normalized)
            _searchResults.value = results
        }
    }

    /**
     * Handles voice input from Google Voice dialog or mic:
     * 1. Displays recognized words in the quick input field AND search query
     * 2. Immediately matches the Ayah across the entire Quran
     * 3. Navigates directly to the target page and highlights the Ayah
     */
    fun handleVoiceRecitation(recognizedText: String, fromSearch: Boolean = false) {
        val cleanText = recognizedText.trim()
        if (cleanText.isBlank()) return

        _quickInputText.value = cleanText

        if (fromSearch) {
            setSearchQuery(cleanText)
        }

        // Send to TrackingController for recognition with context
        TrackingController.processRecognizedText(cleanText, forceImmediate = true)

        // For search view or direct recitation, navigate directly
        viewModelScope.launch(Dispatchers.IO) {
            val prefSurah = if (!fromSearch) trackingState.value.currentSurah?.id else null
            val prefAyah = if (!fromSearch) trackingState.value.currentAyah?.ayahNumber else null
            val match = matcher.matchText(cleanText, preferredSurah = prefSurah, preferredAyah = prefAyah)
            if (match != null) {
                val ayah = repository.getAyah(match.surahNumber, match.ayahNumber)
                val page = ayah?.pageNumber ?: repository.getPageForAyah(match.surahNumber, match.ayahNumber) ?: 1
                viewModelScope.launch(Dispatchers.Main) {
                    loadPage(page)
                    _selectedTab.value = 0
                }
            } else if (fromSearch) {
                // Fallback: search query for Search tab only
                val searchList = repository.search(ArabicNormalizer.normalize(cleanText))
                if (searchList.isNotEmpty()) {
                    val firstAyah = searchList.first()
                    viewModelScope.launch(Dispatchers.Main) {
                        loadPage(firstAyah.pageNumber)
                        _selectedTab.value = 0
                    }
                }
            }
        }
    }

    fun startAutoFollow() {
        TrackingController.startTracking(getApplication())
    }

    fun stopAutoFollow() {
        TrackingController.stopTracking(getApplication())
    }

    fun toggleTouchLock() {
        TrackingController.toggleTouchLock()
    }

    fun unlockTouch() {
        TrackingController.unlockTouch()
    }

    fun openSimulator() {
        _isSimulatorOpen.value = true
    }

    fun closeSimulator() {
        _isSimulatorOpen.value = false
    }

    fun simulateCandidate(surahNumber: Int, ayahNumber: Int) {
        TrackingController.simulateAyah(surahNumber, ayahNumber)
    }

    fun simulateText(text: String) {
        handleVoiceRecitation(text)
    }

    fun advanceNextAyah() {
        TrackingController.advanceNextAyah()
    }

    fun advancePreviousAyah() {
        TrackingController.advancePreviousAyah()
    }
}
