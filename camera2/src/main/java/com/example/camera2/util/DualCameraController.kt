package com.example.camera2.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.example.camera2.CameraImage
import com.example.camera2.ui.AutoFitTextureView
import com.example.common.SimpleMeasurer
import com.example.common.toFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.properties.Delegates

class DualCameraController(
    private val textureView: AutoFitTextureView,
    private val targetResolution: Size
) : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineName("DualCameraController")
    private val cameraManager: CameraManager
    private var leftCamera: CameraDevice? = null
    private var rightCamera: CameraDevice? = null

    private val leftCameraId: String
    private val rightCameraId: String

    private var cameraHandlerThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var leftImageReaderThread: HandlerThread? = null
    private var leftImageReaderHandler: Handler? = null
    private var rightImageReaderThread: HandlerThread? = null
    private var rightImageReaderHandler: Handler? = null

    private var dualImageAvailableListener: DualImageAvailableListener? = null

    private var leftCameraImageReader: ImageReader? = null
    private var rightCameraImageReader: ImageReader? = null
    private var leftCameraCaptureSession: CameraCaptureSession? = null
    private var rightCameraCaptureSession: CameraCaptureSession? = null

    private var isStarted = AtomicBoolean(false)
    private var cameraOpenTime: Long? = null

    private val measurer0 = SimpleMeasurer("0")
    private val measurer1 = SimpleMeasurer("1")

    // TODO: remove. for test
    var imageSaveToFileCnt = 0

    private val dualCameraImageQueue = DualCameraImageQueue { leftImage, rightImage ->
        launch {
            if (imageSaveToFileCnt == 200) {
                Log.i("Measurer", "save image")
                leftImage.toFile(textureView.context, "left_$imageSaveToFileCnt.jpg")
                rightImage.toFile(textureView.context, "right_$imageSaveToFileCnt.jpg")
            }
            imageSaveToFileCnt++
        }

        dualImageAvailableListener?.onDualImageAvailable(leftImage, rightImage)
    }

    init {
        cameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        leftCameraId = cameraManager.cameraIdList.getOrNull(0) ?: "0"
        rightCameraId = cameraManager.cameraIdList.getOrNull(1) ?: "1"
    }

    private suspend fun startBackgroundThread() {
        coroutineScope {
            listOf(
                async {
                    cameraHandlerThread = HandlerThread("DualCameraThread")
                    cameraHandlerThread?.start()
                    cameraHandler = Handler(cameraHandlerThread!!.looper)
                },
                async {
                    leftImageReaderThread = HandlerThread("LeftImageReaderThread")
                    leftImageReaderThread?.start()
                    leftImageReaderHandler = Handler(leftImageReaderThread!!.looper)
                },
                async {
                    rightImageReaderThread = HandlerThread("RightImageReaderThread")
                    rightImageReaderThread?.start()
                    rightImageReaderHandler = Handler(rightImageReaderThread!!.looper)
                }
            ).awaitAll()
        }
    }

    private suspend fun stopBackgroundThread() {
        coroutineScope {
            listOf(
                async {
                    cameraHandlerThread?.quitSafely()
                    try {
                        cameraHandlerThread?.join()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } finally {
                        cameraHandlerThread = null
                    }
                },
                async {
                    leftImageReaderThread?.quitSafely()
                    try {
                        leftImageReaderThread?.join()

                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } finally {
                        leftImageReaderThread = null
                    }
                },
                async {
                    rightImageReaderThread?.quitSafely()
                    try {
                        rightImageReaderThread?.join()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } finally {
                        rightImageReaderThread = null
                    }
                }
            ).awaitAll()
        }
    }

    private suspend fun startPreview(textureView: TextureView) {
        coroutineScope {
            openDualCamera(leftCameraId, rightCameraId, cameraHandler)

            leftCameraImageReader = ImageReader.newInstance(
                targetResolution.width,
                targetResolution.height,
                ImageFormat.YUV_420_888,
                5
            )
            rightCameraImageReader = ImageReader.newInstance(
                targetResolution.width,
                targetResolution.height,
                ImageFormat.YUV_420_888,
                5
            )
            leftCameraCaptureSession = createCameraCaptureSession(
                leftCamera!!,
                leftCameraImageReader!!,
                null,
                targetResolution,
                cameraHandler
            )
            rightCameraCaptureSession = createCameraCaptureSession(
                rightCamera!!,
                rightCameraImageReader!!,
                textureView,
                targetResolution,
                cameraHandler
            )
            setDualImageListener(
                leftCameraImageReader,
                rightCameraImageReader,
                leftImageReaderHandler,
                rightImageReaderHandler
            )
        }
    }

    private suspend fun stopPreview() {
        withContext(Dispatchers.Main) {
            listOf(
                async {
                    try {
                        leftCameraCaptureSession?.stopRepeating()
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    } finally {
                        leftCameraCaptureSession?.close()
                        leftCameraCaptureSession = null
                    }
                    leftCamera?.close()
                    leftCamera = null
                },
                async {
                    try {
                        rightCameraCaptureSession?.stopRepeating()
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    } finally {
                        rightCameraCaptureSession?.close()
                        rightCameraCaptureSession = null
                    }
                    rightCamera?.close()
                    rightCamera = null
                }
            ).awaitAll()
        }
    }

    fun start() {
        if (isStarted.get()) return
        launch {
            measurer0.startTimer()
            measurer1.startTimer()
            isStarted.compareAndSet(false, true)
            startBackgroundThread()
            startPreview(textureView)
        }
    }

    fun stop() {
        launch {
            measurer0.stopTimer()
            measurer1.stopTimer()
            isStarted.compareAndSet(true, false)
            cameraOpenTime = null
            stopPreview()
            stopBackgroundThread()
            dualCameraImageQueue.clear()
        }
    }

    fun setDualImageAvailableListener(listener: DualImageAvailableListener) {
        dualImageAvailableListener = listener
    }

    private suspend fun openDualCamera(
        leftCameraId: String,
        rightCameraId: String,
        handler: Handler?,
    ) {
        coroutineScope {
            listOf(
                async {
                    leftCamera = awaitCameraOpen(cameraManager, leftCameraId, handler)
                },
                async {
                    rightCamera = awaitCameraOpen(cameraManager, rightCameraId, handler)
                }
            ).awaitAll()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitCameraOpen(
        cameraManager: CameraManager,
        cameraId: String,
        handler: Handler?
    ): CameraDevice =
        suspendCancellableCoroutine { continuation ->
            val stateCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (cameraOpenTime != null) {
                        Log.i(
                            "Measurer",
                            "camera open diff= ${SystemClock.elapsedRealtime() - cameraOpenTime!!}ms"
                        )
                    } else {
                        cameraOpenTime = SystemClock.elapsedRealtime()
                    }

                    continuation.resume(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d(TAG, "onDisconnected")
                    continuation.resumeWithException(CancellationException("Camera $cameraId disconnected"))
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    val msg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val errorMsg = "Camera $cameraId error: ($error) $msg"
                    val exception = RuntimeException(errorMsg)
                    Log.d(TAG, "onError $errorMsg")
                    continuation.resumeWithException(exception)
                }
            }
            cameraManager.openCamera(cameraId, stateCallback, handler)
        }

    private suspend fun createCameraCaptureSession(
        camera: CameraDevice,
        cameraImageReader: ImageReader,
        textureView: TextureView?,
        targetResolution: Size,
        cameraHandler: Handler?
    ): CameraCaptureSession {
        if (textureView != null) {
            val surfaceTexture = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(targetResolution.width, targetResolution.height)
        }

        val previewSurface = textureView?.surfaceTexture?.let { Surface(it) }

        val targets = listOfNotNull(previewSurface, cameraImageReader.surface)
        val cameraCaptureSession = createCaptureSession(camera, targets, cameraHandler)
        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30));
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            targets.forEach { addTarget(it) }
        }
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler)
        return cameraCaptureSession
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    private var leftImageIdx: Long = 0L
    private var rightImageIdx: Long by Delegates.observable(0L) { _, oldValue, newValue ->
        if (newValue % 50 == 0L) {
            Log.i(
                "Measurer",
                "image count diff= ${abs(leftImageIdx - rightImageIdx)}, leftIndex=$leftImageIdx, rightIndex=$rightImageIdx"
            )
        }
    }

    private suspend fun setDualImageListener(
        leftCameraImageReader: ImageReader?,
        rightCameraImageReader: ImageReader?,
        leftImageHandler: Handler?,
        rightImageHandler: Handler?
    ) {
        withContext(Dispatchers.IO) {
            listOf(
                async {
                    leftCameraImageReader?.setOnImageAvailableListener({ imageReader ->
                        leftImageIdx++
                        measurer0.addCount()
                        imageReader.acquireLatestImage()?.use { image ->
                            CameraImage(
                                image.timestamp,
                                image.toYuvImage()
                            )
                        }?.let {
                            dualCameraImageQueue.putLeftCameraImage(it)
                        }
                    }, leftImageHandler)
                },
                async {
                    rightCameraImageReader?.setOnImageAvailableListener({ imageReader ->
                        rightImageIdx++
                        measurer1.addCount()
                        imageReader.acquireLatestImage()?.use { image ->
                            CameraImage(
                                image.timestamp,
                                image.toYuvImage()
                            )
                        }?.let {
                            dualCameraImageQueue.putRightCameraImage(it)
                        }
                    }, rightImageHandler)
                }
            ).awaitAll()
        }
    }

    companion object {
        private const val TAG = "DualCameraController"
    }
}

interface DualImageAvailableListener {
    fun onDualImageAvailable(leftImage: YuvImage, rightImage: YuvImage)
}
