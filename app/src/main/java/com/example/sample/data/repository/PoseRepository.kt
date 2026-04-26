package com.example.sample.data.repository

import android.content.Context
import androidx.camera.core.ImageProxy
import com.example.sample.data.processor.PoseLandmarkerHelper
import com.example.sample.domain.LaneAssignmentEngine
import com.example.sample.domain.PoseSmoother
import com.example.sample.presentation.viewmodel.PoseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PoseRepository(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(PoseUiState())
    val uiState: StateFlow<PoseUiState> = _uiState
    private val poseSmoother = PoseSmoother()
    private val laneAssignmentEngine = LaneAssignmentEngine()

    private val landmarker = PoseLandmarkerHelper(
        context = appContext,
        onResult = { frame ->
            val smoothedFrame = poseSmoother.smooth(frame)
            val slottedFrame = laneAssignmentEngine.assign(smoothedFrame)
            _uiState.update {
                it.copy(
                    poseFrame = slottedFrame,
                    errorMessage = null,
                )
            }
        },
        onError = { message ->
            _uiState.update { it.copy(errorMessage = message) }
        },
    )

    fun processCameraFrame(imageProxy: ImageProxy) {
        landmarker.detectLiveStream(imageProxy)
    }

    override fun close() {
        poseSmoother.reset()
        laneAssignmentEngine.reset()
        landmarker.close()
    }
}
