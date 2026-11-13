package com.aventaro.nandan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun VoiceCommandButton(onCommand: (String) -> Unit, onStatus: (String) -> Unit) {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startListening() {
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            onStatus("Offline speech recognition is unavailable. Install an offline speech language pack on this tablet, then try again.")
            return
        }
        val speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer?.destroy()
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { listening = true; onStatus("Offline listening… speak your command.") }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { listening = false; onStatus("Processing voice command…") }
            override fun onError(error: Int) {
                listening = false
                onStatus("Voice input stopped (code $error). Tap the microphone and try again.")
                recognizer?.destroy()
                recognizer = null
            }
            override fun onResults(results: android.os.Bundle?) {
                listening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) onCommand(spoken) else onStatus("I could not understand that command. Try again.")
                recognizer?.destroy()
                recognizer = null
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) { }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) { }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speech.startListening(intent)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else onStatus("Microphone permission is required for voice commands.")
    }
    DisposableEffect(Unit) {
        onDispose { recognizer?.destroy(); recognizer = null }
    }
    FilledTonalButton(
        onClick = {
            if (listening) {
                recognizer?.stopListening()
                listening = false
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                permission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    ) {
        Icon(if (listening) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = null)
        Text(if (listening) "Stop" else "Speak to Nandan", modifier = Modifier.padding(start = 8.dp))
    }
}
