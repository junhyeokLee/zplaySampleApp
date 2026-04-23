package com.example.sample.pose

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel

class PoseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PoseRepository(application)
    val uiState = repository.uiState

    fun processCameraFrame(imageProxy: ImageProxy) {
        repository.processCameraFrame(imageProxy)
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
