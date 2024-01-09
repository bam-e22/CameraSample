package com.example.camera2.util

import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image

fun Image.toYuvImage(): YuvImage {
    require(format == ImageFormat.YUV_420_888) { "Only need to YUV_420_888 format" }
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val width: Int = width
    val height: Int = height

    // Order of U/V channel guaranteed, read more:
    // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888

    // Full size Y channel and quarter size U+V channels.
    val numPixels = (width * height * 1.5f).toInt()
    val nv21 = ByteArray(numPixels)
    var index = 0

    // Copy Y channel.
    for (y in 0 until height) {
        for (x in 0 until width) {
            nv21[index++] = yPlane.buffer.get(y * yPlane.rowStride + x * yPlane.pixelStride)
        }
    }

    // Copy VU data; NV21 format is expected to have YYYYVU packaging.
    // The U/V planes are guaranteed to have the same row stride and pixel stride.
    val uvWidth = width / 2
    val uvHeight = height / 2
    for (y in 0 until uvHeight) {
        for (x in 0 until uvWidth) {
            val bufferIndex = y * uPlane.rowStride + x * uPlane.pixelStride
            // V channel.
            nv21[index++] = vPlane.buffer.get(bufferIndex)
            // U channel.
            nv21[index++] = uPlane.buffer.get(bufferIndex)
        }
    }
    return YuvImage(
        nv21, ImageFormat.NV21, width, height,  /* strides= */null
    )
}
