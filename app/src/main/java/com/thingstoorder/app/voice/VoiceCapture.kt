package com.thingstoorder.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.widget.DontForgetWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed interface VoiceState {
    data object Idle : VoiceState
    data class Listening(val partial: String) : VoiceState
    data class Heard(val text: String) : VoiceState
    data object NothingHeard : VoiceState
    data object Offline : VoiceState
    data object Failed : VoiceState
    data object Denied : VoiceState
    data object Unavailable : VoiceState
}

/** Wraps Android's built-in recognizer (en-IN). Audio handling is up to the system recognizer service. */
@Stable
class VoiceCapture(private val context: Context) {
    var state: VoiceState by mutableStateOf(VoiceState.Idle)
        private set

    private var recognizer: SpeechRecognizer? = null
    private var lastPartial = ""

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED


    fun markDenied() { state = VoiceState.Denied }

    fun reset() {
        cancel()
        state = VoiceState.Idle
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { state = VoiceState.Unavailable; return }
        cancel()
        lastPartial = ""
        state = VoiceState.Listening("")
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().ifBlank { lastPartial }
                state = if (text.isBlank()) VoiceState.NothingHeard else VoiceState.Heard(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) { lastPartial = text; state = VoiceState.Listening(text) }
            }

            override fun onError(error: Int) {
                if (state !is VoiceState.Listening) return
                state = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        if (lastPartial.isNotBlank()) VoiceState.Heard(lastPartial) else VoiceState.NothingHeard
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> VoiceState.Offline
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceState.Denied
                    else -> VoiceState.Failed
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            },
        )
    }

    /** Ends listening early; the recognizer still delivers what it heard. */
    fun finish() { recognizer?.stopListening() }

    fun cancel() {
        recognizer?.let { it.setRecognitionListener(null); it.cancel(); it.destroy() }
        recognizer = null
    }
}

class VoiceController(val capture: VoiceCapture, val begin: () -> Unit)

/** Remembers a capture plus a `begin` that asks for the mic permission first when needed. */
@Composable
fun rememberVoiceController(): VoiceController {
    val context = LocalContext.current
    val capture = remember { VoiceCapture(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capture.start() else capture.markDenied()
    }
    DisposableEffect(capture) { onDispose { capture.cancel() } }
    // While the sheet is listening, the home-screen widget shows its mint Listening state.
    val listening = capture.state is VoiceState.Listening
    val appContext = context.applicationContext
    LaunchedEffect(listening) { setWidgetListening(appContext, listening) }
    DisposableEffect(Unit) {
        onDispose { CoroutineScope(Dispatchers.Default).launch { setWidgetListening(appContext, false) } }
    }
    return remember(capture, launcher) {
        VoiceController(capture) { if (capture.hasPermission()) capture.start() else launcher.launch(Manifest.permission.RECORD_AUDIO) }
    }
}

private suspend fun setWidgetListening(context: Context, listening: Boolean) {
    // A 90 second expiry keeps the widget from staying mint if the app is killed mid-listen.
    SettingsRepository(context).setWidgetListeningUntil(if (listening) System.currentTimeMillis() + 90_000L else null)
    DontForgetWidget().updateAll(context)
}
