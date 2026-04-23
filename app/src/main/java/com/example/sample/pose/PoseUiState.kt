package com.example.sample.pose

data class PoseUiState(
    val poseFrame: PoseFrame = PoseFrame(),
    val errorMessage: String? = null,
)
