package com.example.camerax.util

import android.content.Context
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun Context.getCameraProvider(executor: Executor): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).apply {
            addListener({
                runCatching {
                    continuation.resume(get())
                }.onFailure { exception ->
                    continuation.resumeWithException(exception)
                }
            }, executor)
        }
    }

@ExperimentalCamera2Interop
fun getCameraSelector(cameraId: String): CameraSelector {
    return CameraSelector.Builder()
        .addCameraFilter { cameras ->
            cameras.filter {
                Camera2CameraInfo.from(it).cameraId == cameraId
            }
        }
        .build()
}
