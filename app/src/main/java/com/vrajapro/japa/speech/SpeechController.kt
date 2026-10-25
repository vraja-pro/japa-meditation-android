package com.vrajapro.japa.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Continuous speech recognition on top of Android's [SpeechRecognizer].
 *
 * Android's recognizer stops after each utterance or silence, so this class
 * restarts it automatically until [stop] is called — the native equivalent of
 * `recognition.continuous = true` + the `onend` restart in the web app.
 *
 * Recognition language is hi-IN, which gives the best accuracy for the
 * Sanskrit mantra words (same as the web app). Must be used on the main thread.
 */
class SpeechController(
    context: Context,
    private val onTranscript: (String) -> Unit,
    private val onStatus: (Status) -> Unit,
) {
    enum class Status { LISTENING, UNAVAILABLE, PERMISSION_DENIED, NETWORK_ERROR, ERROR, OFF }

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var consecutiveErrors = 0

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        // Ask for longer utterances; some recognizers ignore these hints.
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
    }

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start() {
        if (active) return
        if (!isAvailable) { onStatus(Status.UNAVAILABLE); return }
        active = true
        consecutiveErrors = 0
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
        }
        listen()
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.let {
            it.setRecognitionListener(null)
            it.cancel()
            it.destroy()
        }
        recognizer = null
        onStatus(Status.OFF)
    }

    private fun listen() {
        if (!active) return
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed", e)
            scheduleRestart(500)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!active) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ listen() }, delayMs)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { onStatus(Status.LISTENING) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onResults(results: Bundle?) {
            consecutiveErrors = 0
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (!text.isNullOrEmpty()) {
                Log.d(TAG, "RAW: $text")
                onTranscript(text)
            }
            scheduleRestart(0)
        }

        override fun onError(error: Int) {
            Log.w(TAG, "Speech error $error")
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    onStatus(Status.PERMISSION_DENIED)
                    stopInternal()
                }
                // Hindi (hi-IN) recognition is not supported/installed on this device.
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                    onStatus(Status.UNAVAILABLE)
                    stopInternal()
                }
                // Silence or nothing recognised — normal while chanting softly; just listen again.
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> scheduleRestart(0)

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER -> {
                    onStatus(Status.NETWORK_ERROR)
                    scheduleRestart(backoff())
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    recognizer?.cancel()
                    scheduleRestart(backoff())
                }
                else -> {
                    onStatus(Status.ERROR)
                    scheduleRestart(backoff())
                }
            }
        }
    }

    private fun backoff(): Long {
        consecutiveErrors++
        return (500L * consecutiveErrors).coerceAtMost(5000L)
    }

    private fun stopInternal() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private companion object {
        const val TAG = "Mantra"
        const val LANGUAGE = "hi-IN"
    }
}
