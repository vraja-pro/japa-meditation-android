package com.vrajapro.japa

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vrajapro.japa.data.MANTRAS_PER_ROUND
import com.vrajapro.japa.data.Session
import com.vrajapro.japa.data.SessionRepository
import com.vrajapro.japa.speech.MantraCounter
import com.vrajapro.japa.speech.SpeechController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Pace(val millisPerWord: Long) { SLOW(600), MEDIUM(500), FAST(300) }

enum class VoiceStatus { OFF, LISTENING, DETECTED, UNAVAILABLE, PERMISSION_DENIED, NETWORK_ERROR, ERROR }

data class JapaState(
    val count: Int = 0,
    val running: Boolean = false,
    val elapsedSec: Int = 0,
    /** Mantra words heard toward the current mantra (0..15). */
    val streak: Int = 0,
    val voiceEnabled: Boolean = false,
    val voiceStatus: VoiceStatus = VoiceStatus.OFF,
    val guidedPlaying: Boolean = false,
    val pace: Pace = Pace.MEDIUM,
    val lockTap: Boolean = false,
    val showSessionDialog: Boolean = false,
    val history: List<Session> = emptyList(),
    /** Increments on every counted mantra; the UI uses it for animation and haptics. */
    val countTick: Int = 0,
) {
    val mantrasInRound: Int get() = count % MANTRAS_PER_ROUND
    val rounds: Int get() = count / MANTRAS_PER_ROUND
    val roundProgress: Float get() = mantrasInRound / MANTRAS_PER_ROUND.toFloat()
}

class JapaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SessionRepository(app)
    private val counter = MantraCounter()

    private val _state = MutableStateFlow(JapaState(history = repo.todaySessions()))
    val state: StateFlow<JapaState> = _state.asStateFlow()

    private var startTime = 0L
    private var timerJob: Job? = null
    private var paceJob: Job? = null
    private var inactivityJob: Job? = null
    private var dialogAutoSaveJob: Job? = null
    private var detectedFlashJob: Job? = null
    private var endedByInactivity = false
    private var inForeground = true

    private val speech = SpeechController(
        context = app,
        onTranscript = ::onTranscript,
        onStatus = ::onSpeechStatus,
    )

    // ─── Counting ────────────────────────────────────────────────────────

    /** Manual button, volume-up key, or lock-mode tap. */
    fun countManual() {
        startSession()
        increment(1)
    }

    /** Volume-down key: undo one mantra. */
    fun decrement() {
        val s = _state.value
        if (s.running && s.count > 0) _state.update { it.copy(count = it.count - 1) }
    }

    private fun increment(n: Int) {
        if (!_state.value.running || n <= 0) return
        _state.update { it.copy(count = it.count + n, countTick = it.countTick + 1) }
        resetInactivity()
        if (_state.value.voiceEnabled) {
            _state.update { it.copy(voiceStatus = VoiceStatus.DETECTED) }
            detectedFlashJob?.cancel()
            detectedFlashJob = viewModelScope.launch {
                delay(1500)
                if (_state.value.running && _state.value.voiceEnabled) {
                    _state.update { it.copy(voiceStatus = VoiceStatus.LISTENING) }
                }
            }
        }
    }

    // ─── Session lifecycle ───────────────────────────────────────────────

    private fun startSession() {
        if (_state.value.running) return
        _state.update { it.copy(running = true) }
        startTime = System.currentTimeMillis() - _state.value.elapsedSec * 1000L
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                _state.update { it.copy(elapsedSec = elapsed) }
                delay(500)
            }
        }
        resetInactivity()
        if (_state.value.voiceEnabled && inForeground) speech.start()
        if (_state.value.guidedPlaying) startPaceLoop()
    }

    fun endSession() = endSession(byInactivity = false)

    private fun endSession(byInactivity: Boolean) {
        if (!_state.value.running) return
        endedByInactivity = byInactivity
        stopActivity()
        _state.update { it.copy(running = false, showSessionDialog = true) }
        if (byInactivity) scheduleDialogAutoSave()
    }

    fun reset() {
        stopActivity()
        _state.update { it.copy(running = false, voiceEnabled = false, guidedPlaying = false) }
        clearCounters()
    }

    private fun stopActivity() {
        timerJob?.cancel(); timerJob = null
        paceJob?.cancel(); paceJob = null
        inactivityJob?.cancel(); inactivityJob = null
        speech.stop()
    }

    private fun clearCounters() {
        endedByInactivity = false
        dialogAutoSaveJob?.cancel()
        counter.reset()
        _state.update {
            it.copy(count = 0, elapsedSec = 0, streak = 0, voiceStatus = VoiceStatus.OFF, showSessionDialog = false)
        }
    }

    private fun resetInactivity() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(INACTIVITY_MS)
            endSession(byInactivity = true)
        }
    }

    // ─── Session dialog ──────────────────────────────────────────────────

    fun saveSession(notes: String) {
        val s = _state.value
        _state.update {
            it.copy(
                history = repo.save(Session(System.currentTimeMillis(), s.elapsedSec, s.count, notes.trim())),
                voiceEnabled = false,
                guidedPlaying = false,
            )
        }
        clearCounters()
    }

    fun discardSession() {
        _state.update { it.copy(voiceEnabled = false, guidedPlaying = false) }
        clearCounters()
    }

    /** "Back to practice": close the dialog; resume if the session ended by inactivity. */
    fun dismissSessionDialog() {
        dialogAutoSaveJob?.cancel()
        _state.update { it.copy(showSessionDialog = false) }
        if (endedByInactivity) {
            endedByInactivity = false
            startSession()
        }
    }

    /** Any interaction with the dialog postpones the inactivity auto-save. */
    fun onSessionDialogInteraction() {
        if (endedByInactivity && _state.value.showSessionDialog) scheduleDialogAutoSave()
    }

    private fun scheduleDialogAutoSave() {
        dialogAutoSaveJob?.cancel()
        dialogAutoSaveJob = viewModelScope.launch {
            delay(INACTIVITY_MS)
            val s = _state.value
            _state.update {
                it.copy(
                    history = repo.save(
                        Session(System.currentTimeMillis(), s.elapsedSec, s.count, notes = "", autoSaved = true)
                    ),
                    voiceEnabled = false,
                    guidedPlaying = false,
                )
            }
            clearCounters()
        }
    }

    // ─── Lock tap mode ───────────────────────────────────────────────────

    fun setLockTap(enabled: Boolean) = _state.update { it.copy(lockTap = enabled) }

    // ─── Guided chanting ─────────────────────────────────────────────────

    fun setPace(pace: Pace) {
        _state.update { it.copy(pace = pace) }
        if (_state.value.guidedPlaying && _state.value.running) startPaceLoop()
    }

    fun setGuidedPlaying(playing: Boolean) {
        if (playing) {
            setVoiceEnabled(false)
            _state.update { it.copy(guidedPlaying = true) }
            if (_state.value.running) startPaceLoop() else startSession()
        } else {
            _state.update { it.copy(guidedPlaying = false) }
            paceJob?.cancel(); paceJob = null
        }
    }

    private fun startPaceLoop() {
        paceJob?.cancel()
        paceJob = viewModelScope.launch {
            while (isActive) {
                delay(_state.value.pace.millisPerWord)
                if (!_state.value.running) continue
                val completed = counter.advance()
                _state.update { it.copy(streak = counter.streak) }
                if (completed) increment(1)
            }
        }
    }

    // ─── Voice detection ─────────────────────────────────────────────────

    /** Call only after RECORD_AUDIO permission has been granted. */
    fun setVoiceEnabled(enabled: Boolean) {
        if (enabled == _state.value.voiceEnabled) return
        if (enabled) {
            if (!speech.isAvailable) {
                _state.update { it.copy(voiceStatus = VoiceStatus.UNAVAILABLE) }
                return
            }
            if (_state.value.guidedPlaying) setGuidedPlaying(false)
            _state.update { it.copy(voiceEnabled = true) }
            if (_state.value.running) { if (inForeground) speech.start() } else startSession()
        } else {
            _state.update { it.copy(voiceEnabled = false, voiceStatus = VoiceStatus.OFF) }
            speech.stop()
        }
    }

    fun onMicPermissionDenied() {
        _state.update { it.copy(voiceEnabled = false, voiceStatus = VoiceStatus.PERMISSION_DENIED) }
    }

    private fun onTranscript(text: String) {
        val found = counter.feed(text)
        _state.update { it.copy(streak = counter.streak) }
        if (found > 0) increment(found)
    }

    private fun onSpeechStatus(status: SpeechController.Status) {
        when (status) {
            SpeechController.Status.LISTENING ->
                if (_state.value.voiceStatus != VoiceStatus.DETECTED) {
                    _state.update { it.copy(voiceStatus = VoiceStatus.LISTENING) }
                }
            SpeechController.Status.OFF ->
                if (_state.value.voiceStatus == VoiceStatus.LISTENING || _state.value.voiceStatus == VoiceStatus.DETECTED) {
                    _state.update { it.copy(voiceStatus = VoiceStatus.OFF) }
                }
            SpeechController.Status.UNAVAILABLE ->
                _state.update { it.copy(voiceEnabled = false, voiceStatus = VoiceStatus.UNAVAILABLE) }
            SpeechController.Status.PERMISSION_DENIED ->
                _state.update { it.copy(voiceEnabled = false, voiceStatus = VoiceStatus.PERMISSION_DENIED) }
            SpeechController.Status.NETWORK_ERROR ->
                _state.update { it.copy(voiceStatus = VoiceStatus.NETWORK_ERROR) }
            SpeechController.Status.ERROR ->
                _state.update { it.copy(voiceStatus = VoiceStatus.ERROR) }
        }
    }

    // ─── App lifecycle ───────────────────────────────────────────────────

    /** Android blocks background microphone use, so pause listening while the app is hidden. */
    fun onAppBackground() {
        inForeground = false
        speech.stop()
    }

    fun onAppForeground() {
        inForeground = true
        val s = _state.value
        if (s.running && s.voiceEnabled) speech.start()
        _state.update { it.copy(history = repo.todaySessions()) }
    }

    override fun onCleared() {
        speech.stop()
    }

    private companion object {
        const val INACTIVITY_MS = 60_000L
    }
}
