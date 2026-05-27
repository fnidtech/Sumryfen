package com.sumryfen.ui.meeting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumryfen.audio.AudioRecorder
import com.sumryfen.data.local.AppDatabase
import com.sumryfen.data.local.MeetingEntity
import com.sumryfen.data.remote.LlmApiClient
import com.sumryfen.data.remote.NetworkMonitor
import com.sumryfen.data.remote.RateLimitException
import com.sumryfen.data.remote.SttApiClient
import com.sumryfen.data.repository.MeetingRepository
import com.sumryfen.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val MAX_DURATION_SECONDS = 7200 // 2 jam
    }

    private val audioRecorder = AudioRecorder()
    private val sttClient = SttApiClient()
    private val llmClient = LlmApiClient()
    private val settingsRepo = SettingsRepository(application)
    private val meetingRepo = MeetingRepository(
        AppDatabase.getInstance(application).meetingDao()
    )
    private val networkMonitor = NetworkMonitor(application)

    private val _state = MutableStateFlow(MeetingState())
    val state: StateFlow<MeetingState> = _state.asStateFlow()

    // Jobs
    private var audioJob: Job? = null
    private var sttTimerJob: Job? = null
    private var summaryTimerJob: Job? = null
    private var networkJob: Job? = null

    // State akumulasi
    private val sttBuffer = mutableListOf<ByteArray>()
    private var accumulatedChunks = mutableListOf<ByteArray>()
    private var fullTranscript = ""
    private var previousSummary: String? = null
    private var wordCount = 0
    private var isSttInProgress = false
    private var currentMeetingId: Long = -1

    /**
     * Validasi konfigurasi sebelum mulai rekaman.
     * @return null jika valid, String error jika tidak valid
     */
    fun validateConfig(): String? {
        if (settingsRepo.getSttApiKey().isBlank()) {
            return "API Key STT belum diisi. Atur di Pengaturan."
        }
        if (settingsRepo.getSttModel().isBlank()) {
            return "Model STT belum diisi."
        }
        if (settingsRepo.getLlmModel().isBlank()) {
            return "Model LLM belum diisi."
        }
        if (!networkMonitor.isOnline) {
            return "Tidak ada koneksi internet. Periksa koneksi Anda."
        }
        return null
    }

    fun startMeeting() {
        if (_state.value.status != MeetingStatus.Idle) return

        _state.value = MeetingState(status = MeetingStatus.Recording)
        sttBuffer.clear()
        accumulatedChunks.clear()
        fullTranscript = ""
        previousSummary = null
        wordCount = 0
        isSttInProgress = false
        currentMeetingId = -1

        // Pantau koneksi internet
        networkJob = viewModelScope.launch {
            networkMonitor.observeNetworkState().collect { isOnline ->
                _state.value = _state.value.copy(isOffline = !isOnline)
            }
        }

        // Mulai koleksi audio dari mikrofon
        audioJob = viewModelScope.launch {
            try {
                audioRecorder.start().collect { pcmChunk ->
                    sttBuffer.add(pcmChunk)
                    accumulatedChunks.add(pcmChunk)
                }
            } catch (e: Exception) {
                setFatalError("Rekaman gagal: ${e.message}")
            }
        }

        // Timer STT: kirim audio tiap ~1 detik
        sttTimerJob = viewModelScope.launch {
            while (_state.value.status == MeetingStatus.Recording) {
                delay(1000)
                val elapsed = _state.value.elapsedSeconds + 1

                // Cek batas 2 jam
                if (elapsed >= MAX_DURATION_SECONDS) {
                    setTransientError("Batas maksimal 2 jam tercapai. Meeting dihentikan.")
                    forceStop()
                    return@launch
                }

                _state.value = _state.value.copy(elapsedSeconds = elapsed)

                if (sttBuffer.isNotEmpty() && !isSttInProgress && !_state.value.isOffline) {
                    val chunksToSend = sttBuffer.toList()
                    sttBuffer.clear()
                    processSttChunk(chunksToSend)
                }
            }
        }

        // Timer ringkasan: generate setiap 60 detik
        summaryTimerJob = viewModelScope.launch {
            delay(60_000)
            while (_state.value.status == MeetingStatus.Recording) {
                if (!_state.value.isOffline) {
                    runSummary()
                }
                delay(60_000)
            }
        }
    }

    private fun processSttChunk(chunks: List<ByteArray>) {
        isSttInProgress = true
        viewModelScope.launch {
            try {
                val combinedPcm = audioRecorder.combinePcmChunks(chunks)
                val wavData = audioRecorder.encodeToWav(combinedPcm)

                val text = sttClient.transcribe(
                    baseUrl = settingsRepo.getSttBaseUrl(),
                    apiKey = settingsRepo.getSttApiKey(),
                    model = settingsRepo.getSttModel(),
                    audioWav = wavData
                )

                if (text.isNotBlank()) {
                    val current = _state.value.transcript
                    _state.value = _state.value.copy(
                        transcript = current + text.trim() + "\n"
                    )

                    fullTranscript += " " + text.trim()
                    wordCount += text.trim().split(Regex("\\s+")).size

                    if (wordCount >= 200) {
                        runSummary()
                    }
                }
            } catch (e: RateLimitException) {
                setTransientError(
                    "Rate limit STT tercapai. Tunggu ${e.retryAfterSeconds} detik."
                )
            } catch (e: Exception) {
                setTransientError("Transkripsi gagal: ${e.message}")
            } finally {
                isSttInProgress = false
            }
        }
    }

    private fun runSummary() {
        if (fullTranscript.isBlank() || _state.value.isOffline) return

        viewModelScope.launch {
            try {
                val summary = llmClient.summarize(
                    baseUrl = settingsRepo.getLlmBaseUrl(),
                    apiKey = settingsRepo.getLlmApiKey(),
                    model = settingsRepo.getLlmModel(),
                    transcript = fullTranscript.trim(),
                    previousSummary = previousSummary
                )

                if (summary.isNotBlank()) {
                    previousSummary = summary.trim()
                    wordCount = 0
                    _state.value = _state.value.copy(summary = summary.trim())
                }
            } catch (e: RateLimitException) {
                setTransientError(
                    "Rate limit LLM tercapai. Tunggu ${e.retryAfterSeconds} detik."
                )
            } catch (e: Exception) {
                setTransientError("Ringkasan gagal: ${e.message}")
            }
        }
    }

    fun stopMeeting() {
        forceStop()
    }

    private fun forceStop() {
        audioRecorder.stop()
        audioJob?.cancel()
        sttTimerJob?.cancel()
        summaryTimerJob?.cancel()
        networkJob?.cancel()

        _state.value = _state.value.copy(status = MeetingStatus.Stopped)

        // Ringkasan final + simpan
        viewModelScope.launch {
            if (wordCount > 0 && fullTranscript.isNotBlank() && !_state.value.isOffline) {
                try {
                    val finalSummary = llmClient.summarize(
                        baseUrl = settingsRepo.getLlmBaseUrl(),
                        apiKey = settingsRepo.getLlmApiKey(),
                        model = settingsRepo.getLlmModel(),
                        transcript = fullTranscript.trim(),
                        previousSummary = previousSummary
                    )
                    if (finalSummary.isNotBlank()) {
                        previousSummary = finalSummary.trim()
                        _state.value = _state.value.copy(summary = finalSummary.trim())
                    }
                } catch (_: Exception) { }
            }

            saveMeeting()
        }
    }

    private suspend fun saveMeeting() {
        try {
            val state = _state.value
            val app = getApplication<Application>()
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id"))
                .format(Date())

            val title = "Meeting $dateStr"

            val meetingId = meetingRepo.insertMeeting(
                MeetingEntity(
                    title = title,
                    transcript = state.transcript,
                    summary = state.summary,
                    durationSeconds = state.elapsedSeconds
                )
            )
            currentMeetingId = meetingId

            if (settingsRepo.isSaveAudioEnabled() && accumulatedChunks.isNotEmpty()) {
                val audioDir = File(app.filesDir, "audio")
                val audioFile = File(audioDir, "${meetingId}.wav")
                val success = audioRecorder.saveWavFile(accumulatedChunks, audioFile)
                if (success) {
                    meetingRepo.updateMeeting(
                        MeetingEntity(
                            id = meetingId,
                            title = title,
                            transcript = state.transcript,
                            summary = state.summary,
                            audioFilePath = audioFile.absolutePath,
                            durationSeconds = state.elapsedSeconds
                        )
                    )
                }
            }
        } catch (e: Exception) {
            setTransientError("Gagal menyimpan: ${e.message}")
        }
    }

    private fun setTransientError(message: String) {
        _state.value = _state.value.copy(error = message, isTransientError = true)
    }

    private fun setFatalError(message: String) {
        _state.value = _state.value.copy(
            status = MeetingStatus.Error,
            error = message,
            isTransientError = false
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
        audioJob?.cancel()
        sttTimerJob?.cancel()
        summaryTimerJob?.cancel()
        networkJob?.cancel()
    }
}
