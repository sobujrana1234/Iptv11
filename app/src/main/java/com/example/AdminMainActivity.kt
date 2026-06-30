package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AdminViewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class AdminMainActivity : ComponentActivity() {

    private val adminViewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("SPLASH") }
                val isLoggedIn by adminViewModel.isLoggedIn.collectAsState()

                // Intercept back actions
                BackHandler(enabled = currentScreen != "ADMIN_LOGIN" && currentScreen != "ADMIN_DASHBOARD") {
                    // Do nothing on root screen back button to prevent accidental exit
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F0F0F))
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            "SPLASH" -> AdminSplashScreen(
                                onFinished = {
                                    currentScreen = if (isLoggedIn) "ADMIN_DASHBOARD" else "ADMIN_LOGIN"
                                }
                            )

                            "ADMIN_LOGIN" -> AdminLoginScreen(
                                viewModel = adminViewModel,
                                onLoginSuccess = {
                                    currentScreen = "ADMIN_DASHBOARD"
                                },
                                onBack = {
                                    // Minimize or close activity
                                    finish()
                                }
                            )

                            "ADMIN_DASHBOARD" -> AdminDashboardScreen(
                                viewModel = adminViewModel,
                                onLogout = {
                                    adminViewModel.logout()
                                    currentScreen = "ADMIN_LOGIN"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1500) // Fast, efficient entry
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Elegant Gold/Bronze Admin Shield Card
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFE65100))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin icon",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "NEKBD IPTV ADMIN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "MANAGEMENT CONTROL PANEL",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFB300),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFFFFB300),
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
