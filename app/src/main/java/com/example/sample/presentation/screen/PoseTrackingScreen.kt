package com.example.sample.presentation.screen

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.presentation.component.CameraPreview
import com.example.sample.presentation.component.ErrorBanner
import com.example.sample.presentation.component.FourPersonLayoutGuide
import com.example.sample.presentation.component.PoseOverlay
import com.example.sample.presentation.viewmodel.PoseUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PoseTrackingScreen(
    uiState: StateFlow<PoseUiState>,
    onFrame: (androidx.camera.core.ImageProxy) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        CameraPreview(
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            onFrame = onFrame,
            modifier = Modifier.fillMaxSize(),
        )

        FourPersonLayoutGuide(
            occupiedLanes = state.poseFrame.poses.mapNotNull { it.laneIndex }.toSet(),
            modifier = Modifier.fillMaxSize(),
        )

        PoseOverlay(
            poseFrame = state.poseFrame,
            mirrorHorizontally = false,
            modifier = Modifier.fillMaxSize(),
        )

        state.errorMessage?.let { message ->
            ErrorBanner(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }
    }
}
