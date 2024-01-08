package com.example.camerax.util

import android.content.Context
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
