package com.example.sample.data.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class CameraFramePreprocessor {
    fun toPoseInput(imageProxy: ImageProxy): PoseInputFrame {
        val bitmap = imageProxy
            .toBitmap()
            .rotate(imageProxy.imageInfo.rotationDegrees)

        return PoseInputFrame(
            mpImage = BitmapImageBuilder(bitmap).build(),
            width = bitmap.width,
            height = bitmap.height,
        )
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        if (planes.size == 1 && planes[0].pixelStride == RGBA_PIXEL_STRIDE) {
            return rgba8888ToBitmap()
        }

        val nv21 = yuv420888ToNv21()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, output)
        return BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
    }

    private fun ImageProxy.rgba8888ToBitmap(): Bitmap {
        val plane = planes[0]
        val source = plane.buffer
        val rowStride = plane.rowStride
        val rowBytes = width * RGBA_PIXEL_STRIDE
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (rowStride == rowBytes) {
            source.rewind()
            bitmap.copyPixelsFromBuffer(source)
            return bitmap
        }

        val packed = ByteArray(rowBytes * height)
        for (row in 0 until height) {
            source.position(row * rowStride)
            source.get(packed, row * rowBytes, rowBytes)
        }
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(packed))
        return bitmap
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
        const val JPEG_QUALITY = 85
        const val RGBA_PIXEL_STRIDE = 4
    }
}

data class PoseInputFrame(
    val mpImage: MPImage,
    val width: Int,
    val height: Int,
)
