package com.example.common

import android.content.Context
import android.graphics.Rect
import android.graphics.YuvImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

fun YuvImage.toFile(context: Context, fileName: String) {
    val image = ByteArrayOutputStream().use {
        compressToJpeg(Rect(0, 0, width, height), 100, it)
        it.toByteArray()
    }

    ByteArrayInputStream(image).toFile(context, fileName)
}

fun InputStream.toFile(context: Context, fileName: String) {
    val cacheDir = File(context.cacheDir, "facePay")
    if (!cacheDir.exists()) {
        cacheDir.mkdir()
    }

    val file = File(cacheDir, fileName)

    if (!file.exists()) {
        file.createNewFile()
    }

    file.outputStream().use { fos ->
        copyTo(fos)
    }
}
