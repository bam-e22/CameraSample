package com.example.camerax.ui

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camerax.util.getCameraProvider
import kotlinx.coroutines.launch

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    previewScaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_CENTER,
    imageAnalyzer: ImageAnalysis.Analyzer? = null,
    targetResolution: Size? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    AndroidView(
        factory = { context ->
            val executor = ContextCompat.getMainExecutor(context)
            val usecases = mutableListOf<UseCase>()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // preview
            val previewView = PreviewView(context).apply {
                scaleType = previewScaleType
            }
            Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                    usecases.add(it)
                }

            // imageAnalyzer
            if (imageAnalyzer != null) {
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .apply {
                        targetResolution?.let {
                            setTargetResolution(Size(it.width, it.height))
                        }
                    }
                    .build()
                    .also {
                        it.setAnalyzer(executor, imageAnalyzer)
                        usecases.add(it)
                    }
            }

            coroutineScope.launch {
                runCatching {
                    context.getCameraProvider(executor)
                }.onSuccess { processCameraProvider ->
                    with(processCameraProvider) {
                        unbindAll()
                        bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            *(usecases.toTypedArray())
                        )
                    }
                }.onFailure {
                    Log.e("CameraX", "CameraX initialize fail")
                }
            }
            previewView
        },
        modifier = modifier
    )
}
