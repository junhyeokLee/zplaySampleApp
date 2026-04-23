package com.example.sample.pose

data class PosePoint(
    val x: Float,
    val y: Float,
    val confidence: Float,
)

data class DetectedPose(
    val landmarks: Map<Int, PosePoint>,
)

data class PoseFrame(
    val poses: List<DetectedPose> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val inferenceMs: Long = 0,
)

object HeroPoseLandmarks {
    const val NOSE = 0
    const val LEFT_EYE = 2
    const val RIGHT_EYE = 5
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32

    val visionLike19 = listOf(
        NOSE,
        LEFT_EYE,
        RIGHT_EYE,
        LEFT_EAR,
        RIGHT_EAR,
        LEFT_SHOULDER,
        RIGHT_SHOULDER,
        LEFT_ELBOW,
        RIGHT_ELBOW,
        LEFT_WRIST,
        RIGHT_WRIST,
        LEFT_HIP,
        RIGHT_HIP,
        LEFT_KNEE,
        RIGHT_KNEE,
        LEFT_ANKLE,
        RIGHT_ANKLE,
        LEFT_FOOT_INDEX,
        RIGHT_FOOT_INDEX,
    )

    val skeleton = listOf(
        LEFT_EAR to LEFT_EYE,
        LEFT_EYE to NOSE,
        NOSE to RIGHT_EYE,
        RIGHT_EYE to RIGHT_EAR,
        LEFT_SHOULDER to RIGHT_SHOULDER,
        LEFT_SHOULDER to LEFT_ELBOW,
        LEFT_ELBOW to LEFT_WRIST,
        RIGHT_SHOULDER to RIGHT_ELBOW,
        RIGHT_ELBOW to RIGHT_WRIST,
        LEFT_SHOULDER to LEFT_HIP,
        RIGHT_SHOULDER to RIGHT_HIP,
        LEFT_HIP to RIGHT_HIP,
        LEFT_HIP to LEFT_KNEE,
        LEFT_KNEE to LEFT_ANKLE,
        LEFT_ANKLE to LEFT_FOOT_INDEX,
        RIGHT_HIP to RIGHT_KNEE,
        RIGHT_KNEE to RIGHT_ANKLE,
        RIGHT_ANKLE to RIGHT_FOOT_INDEX,
    )
}
