package com.example.camerasample.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camerasample.data.FeatureSupport
import com.example.camerasample.ui.components.Description
import com.example.camerasample.ui.components.NavButton
import com.example.camerasample.ui.components.Title

@Composable
fun HomeScreen(
    navigateToCameraX: () -> Unit,
    navigateToCamera2: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.checkConcurrentCameraFeature(context)
        viewModel.checkMultiCameraApiSupport(context)
        viewModel.checkCameras(context)
    }
    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        val concurrentCameraFeatureSupport by viewModel.isSupportConcurrentCameraFeature.collectAsStateWithLifecycle()
        val multiCameraApiSupport by viewModel.isSupportMultiCameraApi.collectAsStateWithLifecycle()
        val cameraInfo by viewModel.cameraInfo.collectAsStateWithLifecycle()
        Contents(
            navigateToCameraX = navigateToCameraX,
            navigateToCamera2 = navigateToCamera2,
            concurrentCameraFeatureSupport = concurrentCameraFeatureSupport,
            multiCameraApiSupport = multiCameraApiSupport,
            cameraInfo = cameraInfo,
            modifier = modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(paddingValues)
        )
    }
}

@Composable
private fun Contents(
    navigateToCameraX: () -> Unit,
    navigateToCamera2: () -> Unit,
    concurrentCameraFeatureSupport: FeatureSupport?,
    multiCameraApiSupport: FeatureSupport?,
    cameraInfo: String?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Samples
        Title(
            text = "Samples"
        )
        Spacer(Modifier.height(20.dp))
        NavButton(
            text = "CameraX Basic Sample",
            onClick = navigateToCameraX
        )
        Spacer(Modifier.height(10.dp))
        NavButton(
            text = "Camera2 Basic Sample",
            onClick = navigateToCamera2
        )
        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(20.dp))
        // ConcurrentCamera Feature check
        Title(
            text = "Concurrent Camera Feature"
        )
        Description(
            text = concurrentCameraFeatureSupport?.toString()
        )
        Spacer(Modifier.height(10.dp))
        Title(
            text = "Multi camera API"
        )
        Description(
            text = multiCameraApiSupport?.toString()
        )
        Spacer(Modifier.height(20.dp))
        Title(
            text = "Camera Info"
        )
        Description(
            text = cameraInfo
        )
    }
}
