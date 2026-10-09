package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.audio.AudioEngine
import com.example.data.local.AyahEntity
import com.example.data.local.QuranDatabase
import com.example.data.local.SurahEntity
import com.example.data.repository.QuranRepository
import com.example.domain.recognition.RecitationMatcher
import com.example.domain.recognition.VerseCandidate
import com.example.domain.recognition.VerseStabilizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TrackingController"

data class TrackingState(
    val isTracking: Boolean = false,
    val currentSurah: SurahEntity? = null,
    val currentAyah: AyahEntity? = null,
    val currentPage: Int = 1,
    val audioLevel: Float = 0f,
    val confidence: Float = 0f,
    val isTouchLocked: Boolean = false,
    val statusText: String = "جاهز لبدء المتابعة",
    val lastRecognizedSpeech: String = ""
)

object TrackingController {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var repository: QuranRepository? = null
    private var matcher: RecitationMatcher? = null
    private val stabilizer = VerseStabilizer()
    private var audioEngine: AudioEngine? = null

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    // Event emitted whenever an Ayah is recognized so UI navigates immediately
    private val _navigationEvents = MutableSharedFlow<Int>(extraBufferCapacity = 10)
    val navigationEvents: SharedFlow<Int> = _navigationEvents.asSharedFlow()

    fun initialize(context: Context) {
        if (repository == null) {
            val db = QuranDatabase.getDatabase(context.applicationContext)
            val repo = QuranRepository(db.quranDao())
            repository = repo
            val m = RecitationMatcher(repo)
            matcher = m
            scope.launch(Dispatchers.IO) {
                m.warmUp()
            }
        }
    }

    fun startTracking(context: Context) {
        initialize(context)
        if (_state.value.isTracking) return

        stabilizer.reset()
        _state.update {
            it.copy(
                isTracking = true,
                isTouchLocked = false,
                currentSurah = null,
                currentAyah = null,
                lastRecognizedSpeech = "",
                statusText = "الميكروفون نشط — جاري الاستماع لتلاوة الإمام..."
            )
        }

        // Start Foreground Service
        val serviceIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting TrackingService: ${e.message}")
        }

        // Start Audio Engine
        audioEngine?.stop()
        audioEngine = AudioEngine(context.applicationContext) { recognizedText, isPartial ->
            _state.update { it.copy(lastRecognizedSpeech = recognizedText) }
            processRecognizedText(recognizedText, forceImmediate = !isPartial)
        }.also { engine ->
            engine.start()
            // Observe audio level
            scope.launch {
                engine.audioLevel.collect { level ->
                    if (_state.value.isTracking) {
                        _state.update { it.copy(audioLevel = level) }
                    }
                }
            }
            // Observe status message
            scope.launch {
                engine.statusMessage.collect { msg ->
                    if (_state.value.isTracking && _state.value.currentAyah == null) {
                        _state.update { it.copy(statusText = msg) }
                    }
                }
            }
        }
    }

    fun stopTracking(context: Context) {
        if (!_state.value.isTracking) return

        audioEngine?.stop()
        audioEngine = null
        stabilizer.reset()

        _state.update {
            it.copy(
                isTracking = false,
                isTouchLocked = false,
                audioLevel = 0f,
                statusText = "تم إيقاف المتابعة"
            )
        }

        val serviceIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        try {
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service: ${e.message}")
        }
    }

    fun toggleTouchLock() {
        _state.update { it.copy(isTouchLocked = !it.isTouchLocked) }
    }

    fun lockTouch() {
        _state.update { it.copy(isTouchLocked = true) }
    }

    fun unlockTouch() {
        _state.update { it.copy(isTouchLocked = false) }
    }

    fun setCurrentPage(page: Int) {
        _state.update { it.copy(currentPage = page) }
        val repo = repository ?: return
        if (_state.value.currentSurah == null) {
            scope.launch(Dispatchers.IO) {
                val pageAyahs = repo.getAyahsByPageSync(page)
                val first = pageAyahs.firstOrNull()
                if (first != null && _state.value.currentSurah == null) {
                    val surah = repo.getSurah(first.surahNumber)
                    _state.update {
                        it.copy(currentSurah = surah, currentAyah = first)
                    }
                }
            }
        }
    }

    fun processRecognizedText(text: String, forceImmediate: Boolean = false) {
        val repo = repository ?: return
        val currentSurahNum = _state.value.currentSurah?.id
        val currentAyahNum = _state.value.currentAyah?.ayahNumber

        _state.update { it.copy(lastRecognizedSpeech = text) }

        scope.launch(Dispatchers.IO) {
            val candidate = matcher?.matchText(
                rawText = text,
                preferredSurah = currentSurahNum,
                preferredAyah = currentAyahNum
            )

            if (candidate != null) {
                applyCandidate(candidate, repo, forceImmediate)
            } else {
                Log.d(TAG, "No verse match found for: $text")
                _state.update {
                    it.copy(statusText = "تم التقاط: « $text »")
                }
            }
        }
    }

    fun simulateRecitation(candidateText: String) {
        processRecognizedText(candidateText, forceImmediate = true)
    }

    fun simulateAyah(surahNumber: Int, ayahNumber: Int) {
        val repo = repository ?: return
        scope.launch(Dispatchers.IO) {
            val ayah = repo.getAyah(surahNumber, ayahNumber)
            if (ayah != null) {
                _state.update { it.copy(lastRecognizedSpeech = ayah.textUthmani) }
            }
            applyCandidate(
                VerseCandidate(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    confidence = 1.0f
                ),
                repo,
                forceImmediate = true
            )
        }
    }

    fun advanceNextAyah() {
        val currentSurah = _state.value.currentSurah ?: return
        val currentAyah = _state.value.currentAyah ?: return

        scope.launch(Dispatchers.IO) {
            val nextAyahNum = currentAyah.ayahNumber + 1
            if (nextAyahNum <= currentSurah.ayahsCount) {
                simulateAyah(currentSurah.id, nextAyahNum)
            } else if (currentSurah.id < 114) {
                simulateAyah(currentSurah.id + 1, 1)
            }
        }
    }

    fun advancePreviousAyah() {
        val currentSurah = _state.value.currentSurah ?: return
        val currentAyah = _state.value.currentAyah ?: return

        scope.launch(Dispatchers.IO) {
            val prevAyahNum = currentAyah.ayahNumber - 1
            if (prevAyahNum >= 1) {
                simulateAyah(currentSurah.id, prevAyahNum)
            }
        }
    }

    private suspend fun applyCandidate(
        candidate: VerseCandidate,
        repo: QuranRepository,
        forceImmediate: Boolean = false
    ) {
        val stabilized = stabilizer.processCandidate(candidate, forceImmediate)
        if (stabilized != null) {
            val surah = repo.getSurah(stabilized.surahNumber)
            val ayah = repo.getAyah(stabilized.surahNumber, stabilized.ayahNumber)
            val page = ayah?.pageNumber ?: repo.getPageForAyah(stabilized.surahNumber, stabilized.ayahNumber) ?: 1

            _state.update {
                it.copy(
                    currentSurah = surah,
                    currentAyah = ayah,
                    currentPage = page,
                    confidence = stabilized.confidence,
                    statusText = "تم التعرف: سورة ${surah?.nameArabic ?: ""} — آية ${stabilized.ayahNumber} (صفحة $page)"
                )
            }
            _navigationEvents.tryEmit(page)
        }
    }
}
