package com.example.sample.presentation.viewmodel

import com.example.sample.data.model.PoseFrame

data class PoseUiState(
    val poseFrame: PoseFrame = PoseFrame(),
    val errorMessage: String? = null,
)
