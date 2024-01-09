package com.example.camera2

import android.graphics.YuvImage

data class CameraImage(
    val timestamp: Long,
    val yuvImage: YuvImage
)
