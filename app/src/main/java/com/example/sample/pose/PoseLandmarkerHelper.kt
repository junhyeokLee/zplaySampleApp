package com.example.sample.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class PoseLandmarkerHelper(
    context: Context,
    private val onResult: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit,
) : AutoCloseable {
    private val isProcessing = AtomicBoolean(false)

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
            val bitmap = imageProxy.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees)
            val mpImage = BitmapImageBuilder(bitmap).build()
            landmarker.detectAsync(mpImage, SystemClock.uptimeMillis())
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

    private fun ImageProxy.toBitmap(): Bitmap {
        val nv21 = yuv420888ToNv21()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, output)
        return BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
    }

    private fun ImageProxy.yuv420888ToNv21(): ByteArray {
        val imageWidth = width
        val imageHeight = height
        val frameSize = imageWidth * imageHeight
        val nv21 = ByteArray(frameSize + frameSize / 2)

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        var outputIndex = 0
        for (row in 0 until imageHeight) {
            for (col in 0 until imageWidth) {
                nv21[outputIndex++] = yPlane.buffer.get(
                    row * yPlane.rowStride + col * yPlane.pixelStride
                )
            }
        }

        outputIndex = frameSize
        val chromaHeight = imageHeight / 2
        val chromaWidth = imageWidth / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                nv21[outputIndex++] = vPlane.buffer.get(
                    row * vPlane.rowStride + col * vPlane.pixelStride
                )
                nv21[outputIndex++] = uPlane.buffer.get(
                    row * uPlane.rowStride + col * uPlane.pixelStride
                )
            }
        }
        return nv21
    }

    private fun Bitmap.rotate(rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return this
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private companion object {
        const val MODEL_ASSET = "pose_landmarker_lite.task"
        const val MAX_POSES = 4
        const val JPEG_QUALITY = 85
    }
}
