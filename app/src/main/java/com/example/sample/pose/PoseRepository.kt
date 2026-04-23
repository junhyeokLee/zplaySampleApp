package com.example.sample.pose

import android.content.Context
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PoseRepository(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(PoseUiState())
    val uiState: StateFlow<PoseUiState> = _uiState

    private val landmarker = PoseLandmarkerHelper(
        context = appContext,
        onResult = { frame ->
            _uiState.update { it.copy(poseFrame = frame, errorMessage = null) }
        },
        onError = { message ->
            _uiState.update { it.copy(errorMessage = message) }
        },
    )

    fun processCameraFrame(imageProxy: ImageProxy) {
        landmarker.detectLiveStream(imageProxy)
    }

    override fun close() {
        landmarker.close()
    }
}
