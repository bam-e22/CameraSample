package com.example.camerasample

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camerasample.ui.screen.Camera2Screen
import com.example.camerasample.ui.screen.CameraXScreen
import com.example.camerasample.ui.screen.HomeScreen
import com.example.camerasample.ui.theme.CameraSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            CameraSampleTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                ) {
                    composable("home") {
                        HomeScreen(
                            navigateToCameraX = {
                                navController.navigate("camerax")
                            },
                            navigateToCamera2 = {
                                navController.navigate("camera2")
                            },
                        )
                    }
                    composable("camerax") {
                        CameraXScreen(
                            navigateUp = navController::navigateUp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("camera2") {
                        Camera2Screen(
                            navigateUp = navController::navigateUp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
