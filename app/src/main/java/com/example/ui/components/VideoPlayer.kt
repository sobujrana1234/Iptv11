package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.ChannelEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    channel: ChannelEntity,
    onBack: () -> Unit,
    onFullScreenToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ExoPlayer State
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentUrl by remember { mutableStateOf(channel.streamUrl) }
    var isBackupInUse by remember { mutableStateOf(false) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var retryCount by remember { mutableStateOf(0) }

    // Custom Control Overlays
    var showControls by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    // Gesture Indicators
    var gestureType by remember { mutableStateOf<String?>(null) } // "volume" or "brightness"
    var gestureValue by remember { mutableStateOf(0f) } // 0f to 1f

    // AudioManager for Volume Gestures
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    fun handlePlayerError(error: PlaybackException) {
        if (retryCount < 3) {
            retryCount++
            errorMessage = "Stream failed. Retrying... ($retryCount/3)"
            coroutineScope.launch {
                delay(2000)
                player?.prepare()
                player?.play()
            }
        } else if (!isBackupInUse && channel.backupUrl1.isNotBlank()) {
            isBackupInUse = true
            retryCount = 0
            errorMessage = "Primary stream failed. Switching to Backup Stream 1..."
            currentUrl = channel.backupUrl1
        } else if (isBackupInUse && currentUrl == channel.backupUrl1 && channel.backupUrl2.isNotBlank()) {
            retryCount = 0
            errorMessage = "Backup 1 failed. Switching to Backup Stream 2..."
            currentUrl = channel.backupUrl2
        } else {
            errorMessage = "Playback Error: Unable to resolve live stream links. Please try another channel."
        }
    }

    // Initialize/Re-initialize Player when stream URL changes
    DisposableEffect(currentUrl) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(currentUrl))
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            playbackParameters = androidx.media3.common.PlaybackParameters(playbackSpeed)
            prepare()
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                isPlaying = exoPlayer.isPlaying
                if (state == Player.STATE_READY) {
                    hasError = false
                    retryCount = 0
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                LogPlayerError(error)
                hasError = true
                isPlaying = false
                handlePlayerError(error)
            }
        })

        player = exoPlayer

        onDispose {
            exoPlayer.release()
            player = null
        }
    }

    // Auto-hide controls handler
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Adjust system brightness helper
    val setWindowBrightness = remember {
        { brightness: Float ->
            (context as? Activity)?.let { activity ->
                val layoutParams = activity.window.attributes
                layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1f)
                activity.window.attributes = layoutParams
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val width = size.width
                        val seekAmount = 10000L // 10s
                        player?.let { p ->
                            if (offset.x < width / 2) {
                                // Seek backward
                                p.seekTo((p.currentPosition - seekAmount).coerceAtLeast(0))
                            } else {
                                // Seek forward
                                p.seekTo((p.currentPosition + seekAmount).coerceAtMost(p.duration))
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
            .pointerInput(Unit) {
                var dragStartY = 0f
                var initialVal = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartY = offset.y
                        if (offset.x < size.width / 2) {
                            // Brightness swipe
                            gestureType = "Brightness"
                            val layoutParams = (context as? Activity)?.window?.attributes
                            initialVal = layoutParams?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f
                        } else {
                            // Volume swipe
                            gestureType = "Volume"
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            initialVal = currentVol / maxVolume
                        }
                        gestureValue = initialVal
                    },
                    onDrag = { change, dragAmount ->
                        val sensitivity = 500f
                        val delta = -dragAmount.y / sensitivity
                        gestureValue = (gestureValue + delta).coerceIn(0f, 1f)
                        if (gestureType == "Brightness") {
                            setWindowBrightness(gestureValue)
                        } else {
                            val newVol = (gestureValue * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(800)
                            gestureType = null
                        }
                    }
                )
            }
    ) {
        // AndroidView binding Media3 PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        if (isBackupInUse) {
                            Text(
                                text = "Backup Feed Connected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showSpeedSheet = true }) {
                            Icon(Icons.Default.Speed, contentDescription = "Playback Speed", tint = Color.White)
                        }
                        IconButton(onClick = { showQualitySheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                // Center Play/Pause & Loader
                Box(modifier = Modifier.align(Alignment.Center)) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(54.dp)
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    player?.let { p ->
                                        p.seekTo((p.currentPosition - 10000L).coerceAtLeast(0L))
                                    }
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    player?.let { p ->
                                        if (p.isPlaying) p.pause() else p.play()
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .size(64.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    player?.let { p ->
                                        p.seekTo((p.currentPosition + 10000L).coerceAtMost(p.duration))
                                    }
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                            }
                        }
                    }
                }

                // Bottom Controls Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LIVE", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }

                    IconButton(onClick = { onFullScreenToggle(true) }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                    }
                }
            }
        }

        // Gesture Overlay Hud Indicator
        gestureType?.let { type ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (type == "Volume") {
                            if (gestureValue == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                        } else {
                            Icons.Default.BrightnessMedium
                        },
                        contentDescription = type,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$type: ${(gestureValue * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { gestureValue },
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }

        // Fallback or Active Error Handler Display
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            hasError = false
                            retryCount = 0
                            player?.prepare()
                            player?.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Retry Stream")
                    }
                }
            }
        }
    }

    // Playback Speed Bottom Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Playback Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playbackSpeed = speed
                                player?.setPlaybackSpeed(speed)
                                showSpeedSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${speed}x", color = Color.White)
                        if (playbackSpeed == speed) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Stream Quality / Tracks Sheet
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Video Feed Options",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Track 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentUrl = channel.streamUrl
                            isBackupInUse = false
                            showQualitySheet = false
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Primary Feed (1080p Auto)", color = Color.White)
                    if (!isBackupInUse) {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Backup 1
                if (channel.backupUrl1.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentUrl = channel.backupUrl1
                                isBackupInUse = true
                                showQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backup Feed 1 (720p Stable)", color = Color.White)
                        if (isBackupInUse && currentUrl == channel.backupUrl1) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Backup 2
                if (channel.backupUrl2.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentUrl = channel.backupUrl2
                                isBackupInUse = true
                                showQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backup Feed 2 (480p Low Latency)", color = Color.White)
                        if (isBackupInUse && currentUrl == channel.backupUrl2) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun LogPlayerError(error: PlaybackException) {
    android.util.Log.e("ExoPlayer", "Playback error encountered: ${error.errorCodeName}", error)
}
