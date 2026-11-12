package com.example.gestionale_personale.presentation.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionale_personale.presentation.AppViewModelProvider
import com.example.gestionale_personale.presentation.components.CiottisTopAppBar
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRegistrationScreen(
    viewModel: VoiceRegistrationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
        }
    )

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recordingDuration by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (true) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    fun startRecording() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val outputFile = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.m4a")
        audioFile = outputFile

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: IOException) {
            Log.e("VoiceScreen", "MediaRecorder prepare() failed", e)
            isRecording = false
        }
    }

    fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                Log.e("VoiceScreen", "Error stopping MediaRecorder", e)
            } finally {
                mediaRecorder = null
                isRecording = false

                audioFile?.let { file ->
                    viewModel.processAudio(file)
                }
            }
        }
    }

    when (uiState) {
        is VoiceUiState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                title = { Text("Registrazione IA completata") },
                text = { Text("Sono stati trascritti e registrati con successo ${(uiState as VoiceUiState.Success).savedCount} turni per la data di oggi.") },
                confirmButton = {
                    Button(onClick = { viewModel.resetState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is VoiceUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                title = { Text("Errore", color = MaterialTheme.colorScheme.error) },
                text = { Text((uiState as VoiceUiState.Error).message) },
                confirmButton = {
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Riprova")
                    }
                }
            )
        }
        else -> Unit
    }

    Scaffold(
        topBar = { CiottisTopAppBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasPermission) {
                Text(
                    text = "L'app necessita dell'accesso al microfono per ascoltare il tuo audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Concedi Permesso")
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            Text(
                text = if (isRecording) "Registrazione in corso..." else "Tieni premuto per parlare",
                style = MaterialTheme.typography.titleLarge,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isRecording) {
                val minutes = recordingDuration / 60
                val seconds = recordingDuration % 60
                Text(
                    text = String.format(Locale.ITALIAN, "%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (uiState is VoiceUiState.Processing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "L'Intelligenza Artificiale sta elaborando il tuo audio...",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Comunica gli orari, i cantieri e lo stato dei lavori. Rilascia il tasto per far trascrivere all'IA.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isRecording) 1.2f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) MaterialTheme.colorScheme.errorContainer
                        else if (uiState is VoiceUiState.Processing) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (hasPermission && uiState !is VoiceUiState.Processing) {
                                    startRecording()
                                    tryAwaitRelease()
                                    stopRecording()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microfono",
                    modifier = Modifier.size(72.dp),
                    tint = if (isRecording) MaterialTheme.colorScheme.error
                           else if (uiState is VoiceUiState.Processing) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
