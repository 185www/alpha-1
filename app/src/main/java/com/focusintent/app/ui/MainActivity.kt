package com.focusintent.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.focusintent.app.service.OverlayService
import java.util.*

/**
 * Main activity for FocusIntent app.
 * Provides UI for inputting focus purpose, selecting timer mode, and starting session.
 */
class MainActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val speechPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startSpeechRecognition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if overlay permission is granted, if not go to guide
        if (!hasOverlayPermission()) {
            startActivity(Intent(this, PermissionGuideActivity::class.java))
            finish()
            return
        }
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onStartFocus = { purpose, duration, countUp ->
                            startOverlayService(purpose, duration, countUp)
                        },
                        onRequestSpeechPermission = { requestSpeechPermission() }
                    )
                }
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestSpeechPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED -> {
                startSpeechRecognition()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请输入专注目的")
        }
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // Handle the recognized text
                    }
                    speechRecognizer?.destroy()
                }

                override fun onError(error: Int) {
                    speechRecognizer?.destroy()
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
            })
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startOverlayService(purpose: String, durationSec: Long, countUp: Boolean) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_PURPOSE, purpose)
            putExtra(OverlayService.EXTRA_DURATION, durationSec)
            putExtra(OverlayService.EXTRA_COUNT_UP, countUp)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // Move task to background
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartFocus: (String, Long, Boolean) -> Unit,
    onRequestSpeechPermission: () -> Unit
) {
    var purpose by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(25) }
    var isCountUp by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "FocusIntent",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "明确意图，专注当下",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Purpose Input
        OutlinedTextField(
            value = purpose,
            onValueChange = { purpose = it },
            label = { Text("本次使用手机的目的") },
            placeholder = { Text("例如：查资料、回复消息...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Voice Input Button
        OutlinedButton(
            onClick = onRequestSpeechPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("语音输入")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Timer Mode Selection
        Text(
            text = "计时模式",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = isCountUp,
                onClick = { isCountUp = true },
                label = { Text("正计时") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !isCountUp,
                onClick = { isCountUp = false },
                label = { Text("倒计时") },
                modifier = Modifier.weight(1f),
                enabled = !isCountUp || durationMinutes <= 120
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Duration Selector (only for countdown)
        if (!isCountUp) {
            Text(
                text = "时长：$durationMinutes 分钟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Slider(
                value = durationMinutes.toFloat(),
                onValueChange = { durationMinutes = it.toInt().coerceIn(1, 120) },
                valueRange = 1f..120f,
                steps = 118,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Start Button
        Button(
            onClick = {
                val durationSec = if (isCountUp) 0 else durationMinutes * 60L
                onStartFocus(purpose.ifBlank { "专注中" }, durationSec, isCountUp)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = purpose.isNotBlank() || isCountUp
        ) {
            Text("开始专注", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
