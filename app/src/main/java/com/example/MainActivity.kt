package com.example

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChannelEntity
import com.example.ui.AdminViewModel
import com.example.ui.IptvViewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.UserHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.VideoPlayer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val iptvViewModel: IptvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("SPLASH") }
                val playingChannel by iptvViewModel.currentPlayingChannel.collectAsState()

                // Intercept back actions smoothly
                BackHandler(enabled = currentScreen != "USER_HOME") {
                    when (currentScreen) {
                        "PLAYER" -> {
                            iptvViewModel.stopPlaying()
                            currentScreen = "USER_HOME"
                        }
                    }
                }

                // If a channel is selected inside the viewmodel, navigate to PLAYER automatically
                LaunchedEffect(playingChannel) {
                    if (playingChannel != null) {
                        currentScreen = "PLAYER"
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F0F0F))
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            "SPLASH" -> SplashScreen(
                                onFinished = { currentScreen = "USER_HOME" }
                            )

                            "USER_HOME" -> UserHomeScreen(
                                viewModel = iptvViewModel,
                                onChannelSelected = { ch ->
                                    iptvViewModel.selectChannel(ch)
                                    currentScreen = "PLAYER"
                                }
                            )

                            "PLAYER" -> {
                                val ch = playingChannel
                                if (ch != null) {
                                    VideoPlayer(
                                        channel = ch,
                                        onBack = {
                                            iptvViewModel.stopPlaying()
                                            currentScreen = "USER_HOME"
                                        },
                                        onFullScreenToggle = { isFull ->
                                            triggerPictureInPicture()
                                        }
                                    )
                                } else {
                                    currentScreen = "USER_HOME"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fully Functional Picture in Picture triggers
    private fun triggerPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Fail-safe fallback if system doesn't support
                android.util.Log.w("PiP", "Picture-in-picture mode failed to initialize.")
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically enter PiP when home button is pressed during active player
        if (iptvViewModel.currentPlayingChannel.value != null) {
            triggerPictureInPicture()
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1800) // Beautiful cinematic duration
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Pulsing Red Logo Card
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFE50914), Color(0xFF800000))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsFootball,
                    contentDescription = "Soccer icon",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "NEKBD IPTV",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                "DUAL COMPLETE SYSTEM",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
