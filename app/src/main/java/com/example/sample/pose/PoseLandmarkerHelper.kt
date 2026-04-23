package com.example.sample.pose

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean

class PoseLandmarkerHelper(
    context: Context,
    private val onResult: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit,
) : AutoCloseable {
    private val isProcessing = AtomicBoolean(false)
    private val framePreprocessor = CameraFramePreprocessor()

    private val landmarker: PoseLandmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .build()
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(MAX_POSES)
            .setMinPoseDetectionConfidence(0.45f)
            .setMinPosePresenceConfidence(0.45f)
            .setMinTrackingConfidence(0.45f)
            .setResultListener { result, input ->
                isProcessing.set(false)
                onResult(result.toPoseFrame(input.width, input.height))
            }
            .setErrorListener { error ->
                isProcessing.set(false)
                onError(error.message ?: "Pose detection failed")
            }
            .build()
    )

    fun detectLiveStream(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            val inputFrame = framePreprocessor.toPoseInput(imageProxy)
            landmarker.detectAsync(inputFrame.mpImage, SystemClock.uptimeMillis())
        } catch (throwable: Throwable) {
            isProcessing.set(false)
            onError(throwable.message ?: "Unable to process camera frame")
        } finally {
            imageProxy.close()
        }
    }

    override fun close() {
        landmarker.close()
    }

    private fun PoseLandmarkerResult.toPoseFrame(width: Int, height: Int): PoseFrame {
        val poses = landmarks().take(MAX_POSES).map { pose ->
            DetectedPose(
                landmarks = HeroPoseLandmarks.visionLike19.associateWith { index ->
                    val landmark = pose[index]
                    PosePoint(
                        x = landmark.x(),
                        y = landmark.y(),
                        confidence = landmark.visibility().orElse(landmark.presence().orElse(1f)),
                    )
                }
            )
        }

        return PoseFrame(
            poses = poses,
            imageWidth = width,
            imageHeight = height,
            inferenceMs = 0,
        )
    }

    private companion object {
        const val MODEL_ASSET = "pose_landmarker_lite.task"
        const val MAX_POSES = 4
    }
}
