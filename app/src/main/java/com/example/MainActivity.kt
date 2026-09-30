package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.LifeLinkViewModel
import com.example.ui.ScreenRoute
import com.example.ui.components.LifeLinkBottomBar
import com.example.ui.components.LifeLinkTopBar
import com.example.ui.components.NotificationBannerView
import com.example.ui.components.SosEmergencyDialog
import com.example.ui.screens.*
import com.example.ui.theme.LifeLinkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LifeLinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeLinkTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isSosOpen by viewModel.isSosDialogOpen.collectAsState()
                val notification by viewModel.bannerNotification.collectAsState()

                // Back navigation handler: return to Home from any secondary sub-screen
                if (currentScreen != ScreenRoute.HOME) {
                    BackHandler {
                        viewModel.navigateTo(ScreenRoute.HOME)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        LifeLinkTopBar(viewModel = viewModel)
                    },
                    bottomBar = {
                        LifeLinkBottomBar(
                            currentScreen = currentScreen,
                            onNavigate = { route -> viewModel.navigateTo(route) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Screen Destination
                        when (currentScreen) {
                            ScreenRoute.HOME -> HomeScreen(viewModel = viewModel)
                            ScreenRoute.BLOOD_SEARCH -> BloodSearchScreen(viewModel = viewModel)
                            ScreenRoute.EMERGENCY_REQUESTS -> EmergencyRequestScreen(viewModel = viewModel)
                            ScreenRoute.DONOR_FINDER -> DonorFinderScreen(viewModel = viewModel)
                            ScreenRoute.MAP_RADAR -> MapViewScreen(viewModel = viewModel)
                            ScreenRoute.DONOR_DASHBOARD -> DonorDashboardScreen(viewModel = viewModel)
                            ScreenRoute.ADMIN_PANEL -> AdminPanelScreen(viewModel = viewModel)
                            ScreenRoute.EDUCATION -> EducationScreen(viewModel = viewModel)
                            ScreenRoute.AUTH -> AuthScreen(viewModel = viewModel)
                        }

                        // Banner Notification Overlay
                        AnimatedVisibility(
                            visible = notification != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            notification?.let { notif ->
                                NotificationBannerView(
                                    notification = notif,
                                    onDismiss = { viewModel.dismissNotification() }
                                )
                            }
                        }

                        // 1-Tap SOS Emergency Dialog
                        SosEmergencyDialog(
                            isOpen = isSosOpen,
                            onDismiss = { viewModel.closeSosDialog() },
                            onSubmit = { patient, group, units, hospital, city, phone ->
                                viewModel.triggerSosBroadcast(patient, group, units, hospital, city, phone)
                            }
                        )
                    }
                }
            }
        }
    }
}
