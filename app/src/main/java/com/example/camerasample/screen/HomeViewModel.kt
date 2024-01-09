package com.example.camerasample.screen

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.camera2.util.getAvailableCameraInfo
import com.example.camerasample.data.FeatureSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _isSupportConcurrentCamera = MutableStateFlow<FeatureSupport?>(null)
    val isSupportConcurrentCameraFeature: StateFlow<FeatureSupport?> =
        _isSupportConcurrentCamera.asStateFlow()

    private val _isSupportMultiCameraApi = MutableStateFlow<FeatureSupport?>(null)
    val isSupportMultiCameraApi: StateFlow<FeatureSupport?> =
        _isSupportMultiCameraApi.asStateFlow()

    private val _cameraInfo = MutableStateFlow<String?>(null)
    val cameraInfo: StateFlow<String?> = _cameraInfo.asStateFlow()

    fun checkConcurrentCameraFeature(context: Context) {
        _isSupportConcurrentCamera.value =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val hasSystemFeature =
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT)
                Log.d(TAG, "hasConcurrentCameraFeature: $hasSystemFeature")

                FeatureSupport(
                    isSupport = hasSystemFeature,
                    featureName = "FEATURE_CAMERA_CONCURRENT",
                    description = if (hasSystemFeature) "" else "The device does not support the FEATURE_CAMERA_CONCURRENT system feature."
                )
            } else {
                FeatureSupport(
                    isSupport = false,
                    featureName = "FEATURE_CAMERA_CONCURRENT",
                    description = "The FEATURE_CAMERA_CONCURRENT feature is only supported on API level 30 and above."
                )
            }
    }

    fun checkMultiCameraApiSupport(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val isPhysicalCameraExist = cameraManager.cameraIdList
                .map {
                    cameraManager.getCameraCharacteristics(it)
                }
                .any {
                    it.physicalCameraIds.isNotEmpty()
                }
            val hasLogicalCameraCapability = cameraManager.cameraIdList
                .map {
                    val characteristic = cameraManager.getCameraCharacteristics(it)
                    characteristic.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                }
                .any { capabilities ->
                    capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
                }

            if (isPhysicalCameraExist && hasLogicalCameraCapability) {
                _isSupportMultiCameraApi.value = FeatureSupport(
                    isSupport = true,
                    featureName = "Multi Camera API",
                    description = ""
                )
            } else {
                _isSupportMultiCameraApi.value = FeatureSupport(
                    isSupport = false,
                    featureName = "Multi Camera API",
                    description = "isPhysicalCameraExist: $isPhysicalCameraExist, hasLogicalCameraCapability: $hasLogicalCameraCapability"
                )
            }

        } else {
            _isSupportMultiCameraApi.value = FeatureSupport(
                isSupport = false,
                featureName = "Multi Camera API",
                description = "The Multi Camera API is only supported on API level 28 and above."
            )
        }
    }

    fun checkCameras(context: Context) {
        _cameraInfo.value = context.getAvailableCameraInfo()
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
