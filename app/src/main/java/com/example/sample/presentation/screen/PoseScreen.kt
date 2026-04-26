package com.example.sample.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sample.presentation.viewmodel.PoseViewModel

@Composable
fun PoseRoute() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        val viewModel: PoseViewModel = viewModel()
        PoseTrackingScreen(
            uiState = viewModel.uiState,
            onFrame = viewModel::processCameraFrame,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
