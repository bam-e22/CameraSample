package com.example.camera2.basic

import android.util.Size
import androidx.lifecycle.ViewModel
import com.example.camera2.ui.AutoFitTextureView
import com.example.camera2.util.DualCameraController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class Camera2BasicViewModel @Inject constructor() : ViewModel() {
    private var dualCameraController: DualCameraController? = null
    val targetResolution = Size(640, 480)

    fun onSurfaceTextureAvailable(textureView: AutoFitTextureView) {
        dualCameraController = DualCameraController(
            textureView,
            targetResolution
        ).apply {
            start()
        }
    }

    fun startCamera() {
        dualCameraController?.start()
    }

    fun stopCamera() {
        dualCameraController?.stop()
    }
}
