package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val TAG = "AudioEngine"

class AudioEngine(
    private val context: Context,
    private val onTextRecognized: (text: String, isPartial: Boolean) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _statusMessage = MutableStateFlow("الميكروفون نشط — جاري الاستماع لتلاوة الإمام...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRecognizerActive = false
    private var consecutiveErrorCount = 0

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (_isRecording.value) return
        _isRecording.value = true
        _statusMessage.value = "الميكروفون نشط — جاري الاستماع لتلاوة الإمام..."
        consecutiveErrorCount = 0

        mainHandler.post {
            val hasSpeech = SpeechRecognizer.isRecognitionAvailable(context)
            if (hasSpeech) {
                setupSpeechRecognizer()
            } else {
                _statusMessage.value = "التعرف الصوتي المباشر غير متوفر بالجهاز — استخدم الميكروفون المباشر"
                startFallbackAudioRecord()
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (!_isRecording.value) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                        isRecognizerActive = true
                        _statusMessage.value = "الميكروفون نشط — جاري الاستماع لتلاوة الإمام..."
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                        _statusMessage.value = "جاري التقاط صوت التلاوة..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val lvl = ((rmsdB + 2f) / 10f).coerceIn(0.05f, 1f)
                        _audioLevel.value = lvl
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        isRecognizerActive = false
                    }

                    override fun onError(error: Int) {
                        isRecognizerActive = false
                        Log.d(TAG, "SpeechRecognizer onError: $error")

                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                // Natural pauses between Ayahs
                                consecutiveErrorCount = 0
                                _statusMessage.value = "الميكروفون نشط — في انتظار صوت التلاوة..."
                                scheduleRestart(200)
                            }
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                            SpeechRecognizer.ERROR_CLIENT,
                            SpeechRecognizer.ERROR_AUDIO -> {
                                consecutiveErrorCount++
                                if (consecutiveErrorCount > 4) {
                                    _statusMessage.value = "جاري إعادة تهيئة الميكروفون..."
                                    recreateRecognizer(800)
                                } else {
                                    scheduleRestart(400)
                                }
                            }
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                _statusMessage.value = "يرجى منح إذن الميكروفون لتتبع التلاوة"
                            }
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                                _statusMessage.value = "في انتظار الاتصال — انقر الميكروفون المباشر للتعرف الفوري"
                                scheduleRestart(1000)
                            }
                            else -> {
                                scheduleRestart(500)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isRecognizerActive = false
                        consecutiveErrorCount = 0
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            Log.d(TAG, "Speech final result: $recognized")
                            _statusMessage.value = "تم التقاط: $recognized"
                            onTextRecognized(recognized, false)
                        }

                        if (_isRecording.value) {
                            scheduleRestart(250)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partial = matches[0]
                            Log.d(TAG, "Speech partial result: $partial")
                            _statusMessage.value = "جاري الاستماع: $partial"
                            onTextRecognized(partial, true)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            startListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up speech recognizer: ${e.message}")
            startFallbackAudioRecord()
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!_isRecording.value) return
        mainHandler.postDelayed({
            if (_isRecording.value && !isRecognizerActive) {
                startListening()
            }
        }, delayMs)
    }

    private fun recreateRecognizer(delayMs: Long) {
        if (!_isRecording.value) return
        mainHandler.postDelayed({
            if (_isRecording.value) {
                consecutiveErrorCount = 0
                setupSpeechRecognizer()
            }
        }, delayMs)
    }

    private fun startListening() {
        if (!_isRecording.value || isRecognizerActive) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizer?.startListening(intent)
            isRecognizerActive = true
        } catch (e: Exception) {
            Log.e(TAG, "startListening error: ${e.message}")
            isRecognizerActive = false
            scheduleRestart(500)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFallbackAudioRecord() {
        recordJob?.cancel()
        recordJob = scope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    val buffer = ShortArray(1024)
                    while (isActive && _isRecording.value) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                sum += (buffer[i] * buffer[i])
                            }
                            val rms = sqrt(sum / read)
                            val normalized = (rms / 5000.0).toFloat().coerceIn(0f, 1f)
                            _audioLevel.value = normalized
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback AudioRecord error: ${e.message}")
            }
        }
    }

    fun stop() {
        _isRecording.value = false
        _audioLevel.value = 0f
        recordJob?.cancel()
        recordJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord stop: ${e.message}")
        }
        audioRecord = null

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                isRecognizerActive = false
            } catch (e: Exception) {
                Log.e(TAG, "SpeechRecognizer stop: ${e.message}")
            }
        }
    }
}
