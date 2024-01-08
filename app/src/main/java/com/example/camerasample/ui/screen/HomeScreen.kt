package com.example.camerasample.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.camerasample.ui.components.NavButton

@Composable
fun HomeScreen(
    navigateToCameraX: () -> Unit,
    navigateToCamera2: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(
                    top = paddingValues.calculateTopPadding() + 24.dp,
                    bottom = paddingValues.calculateBottomPadding(),
                    start = 24.dp,
                    end = 24.dp
                )
        ) {
            NavButton(
                text = "CameraX",
                onClick = navigateToCameraX
            )
            Spacer(Modifier.height(20.dp))
            NavButton(
                text = "Camera2",
                onClick = navigateToCamera2
            )
        }
    }
}
