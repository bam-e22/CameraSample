package com.example.camerax.basic

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraXBasicViewModel @Inject constructor() : ViewModel() {
    companion object {
        private const val TAG = "CameraXBasicViewModel"
    }
}
