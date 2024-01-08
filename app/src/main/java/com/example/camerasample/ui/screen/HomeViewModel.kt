package com.example.camerasample.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _concurrentCameraInfo = MutableStateFlow("")
    val concurrentCameraInfo: StateFlow<String> = _concurrentCameraInfo.asStateFlow()

    fun checkConcurrentCameraFeature(context: Context) {

    }
}
