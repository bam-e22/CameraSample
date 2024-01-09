package com.example.camera2.util

import android.graphics.YuvImage
import com.example.camera2.CameraImage
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

class DualCameraImageQueue(
    private val onImagePairAvailable: (YuvImage, YuvImage) -> Unit
) {
    private val leftImageQueue: Queue<CameraImage> = ConcurrentLinkedQueue()
    private val rightImageQueue: Queue<CameraImage> = ConcurrentLinkedQueue()

    fun putLeftCameraImage(image: CameraImage) {
        leftImageQueue.put(image)
        findImagePair(image, rightImageQueue)
    }

    fun putRightCameraImage(image: CameraImage) {
        rightImageQueue.put(image)
        findImagePair(image, leftImageQueue)
    }

    private fun Queue<CameraImage>.put(image: CameraImage) {
        if (size >= MAX_QUEUE_SIZE) {
            poll()
        }
        offer(image)
    }

    private fun findImagePair(image: CameraImage, queue: Queue<CameraImage>) {
        queue.minByOrNull { abs(image.timestamp - it.timestamp) }
            ?.let {
                onImagePairAvailable(image.yuvImage, it.yuvImage)
            }
    }

    fun clear() {
        leftImageQueue.clear()
        rightImageQueue.clear()
    }

    companion object {
        private const val MAX_QUEUE_SIZE = 5
    }
}
