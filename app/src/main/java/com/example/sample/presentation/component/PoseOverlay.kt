package com.example.sample.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sample.data.model.HeroPoseLandmarks
import com.example.sample.data.model.PoseFrame
import kotlin.math.max

private const val LANDMARK_THRESHOLD = 0.5f

fun personColors() = listOf(
    Color(0xFF20D3E6),
    Color(0xFF31D07E),
    Color(0xFFFFA000),
    Color(0xFFB36DFF),
)

@Composable
fun FourPersonLayoutGuide(
    occupiedLanes: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val colors = personColors()
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEachIndexed { index, color ->
            PersonLane(
                number = index + 1,
                color = color,
                isDetected = occupiedLanes.contains(index),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PersonLane(
    number: Int,
    color: Color,
    isDetected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = if (isDetected) 2.dp else 1.dp,
                color = if (isDetected) color else Color(0x668EA4FF),
                shape = RoundedCornerShape(4.dp),
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(44.dp)
                .background(color.copy(alpha = if (isDetected) 0.86f else 0.58f), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isDetected) "LIVE" else "READY",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun PoseOverlay(
    poseFrame: PoseFrame,
    mirrorHorizontally: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = personColors()
    Canvas(modifier = modifier) {
        fun PoseFrame.project(x: Float, y: Float): Offset {
            if (imageWidth <= 0 || imageHeight <= 0) {
                val rawX = x * size.width
                return Offset(
                    x = if (mirrorHorizontally) size.width - rawX else rawX,
                    y = y * size.height,
                )
            }

            val scale = max(size.width / imageWidth, size.height / imageHeight)
            val renderedWidth = imageWidth * scale
            val renderedHeight = imageHeight * scale
            val offsetX = (size.width - renderedWidth) / 2f
            val offsetY = (size.height - renderedHeight) / 2f
            val rawX = offsetX + x * renderedWidth

            return Offset(
                x = if (mirrorHorizontally) size.width - rawX else rawX,
                y = offsetY + y * renderedHeight,
            )
        }

        poseFrame.poses.forEachIndexed { index, pose ->
            val color = colors[pose.laneIndex ?: (index % colors.size)]
            HeroPoseLandmarks.skeleton.forEach { (startId, endId) ->
                val start = pose.landmarks[startId]
                val end = pose.landmarks[endId]
                if (start != null && end != null && start.confidence > LANDMARK_THRESHOLD && end.confidence > LANDMARK_THRESHOLD) {
                    drawLine(
                        color = color,
                        start = poseFrame.project(start.x, start.y),
                        end = poseFrame.project(end.x, end.y),
                        strokeWidth = 7f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            pose.landmarks.values.forEach { point ->
                if (point.confidence > LANDMARK_THRESHOLD) {
                    val center = poseFrame.project(point.x, point.y)
                    drawCircle(color = Color.White, radius = 9f, center = center)
                    drawCircle(color = color, radius = 6f, center = center)
                }
            }
        }
    }
}
