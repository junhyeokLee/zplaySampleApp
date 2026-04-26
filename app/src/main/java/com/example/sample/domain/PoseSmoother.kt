package com.example.sample.domain

import com.example.sample.data.model.DetectedPose
import com.example.sample.data.model.PoseFrame
import com.example.sample.data.model.PosePoint
import com.example.sample.data.model.torsoCenter
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class PoseSmoother {
    private var previousFrame: PoseFrame = PoseFrame()

    fun smooth(frame: PoseFrame): PoseFrame {
        if (frame.poses.isEmpty()) {
            previousFrame = frame
            return frame
        }

        val availablePrevious = previousFrame.poses.toMutableList()
        val smoothedPoses = frame.poses.map { currentPose ->
            val matchedPrevious = currentPose.findClosestPose(availablePrevious)
            if (matchedPrevious != null) {
                availablePrevious.remove(matchedPrevious)
                currentPose.smoothWith(matchedPrevious)
            } else {
                currentPose
            }
        }

        val smoothedFrame = frame.copy(poses = smoothedPoses)
        previousFrame = smoothedFrame
        return smoothedFrame
    }

    fun reset() {
        previousFrame = PoseFrame()
    }

    private fun DetectedPose.findClosestPose(candidates: List<DetectedPose>): DetectedPose? {
        val currentCenter = torsoCenter() ?: return null
        return candidates
            .mapNotNull { candidate ->
                val candidateCenter = candidate.torsoCenter() ?: return@mapNotNull null
                candidate to currentCenter.distanceTo(candidateCenter)
            }
            .filter { (_, distance) -> distance < MAX_MATCH_DISTANCE }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun DetectedPose.smoothWith(previous: DetectedPose): DetectedPose {
        val smoothedLandmarks = landmarks.mapValues { (id, current) ->
            val old = previous.landmarks[id] ?: return@mapValues current
            current.smoothWith(old)
        }
        return copy(landmarks = smoothedLandmarks)
    }

    private fun PosePoint.smoothWith(previous: PosePoint): PosePoint {
        val dx = abs(x - previous.x)
        val dy = abs(y - previous.y)
        val adaptiveAlpha = when {
            dx + dy < 0.02f -> 0.18f
            dx + dy < 0.06f -> 0.26f
            else -> 0.38f
        }
        val nextX = if (dx < JITTER_DEADBAND) {
            previous.x
        } else {
            previous.x + (x - previous.x).coerceIn(-MAX_STEP, MAX_STEP) * adaptiveAlpha
        }
        val nextY = if (dy < JITTER_DEADBAND) {
            previous.y
        } else {
            previous.y + (y - previous.y).coerceIn(-MAX_STEP, MAX_STEP) * adaptiveAlpha
        }

        return copy(
            x = nextX,
            y = nextY,
            confidence = previous.confidence + (confidence - previous.confidence) * CONFIDENCE_ALPHA,
        )
    }

    private fun PosePoint.distanceTo(other: PosePoint): Float {
        return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
    }

    private companion object {
        const val CONFIDENCE_ALPHA = 0.35f
        const val JITTER_DEADBAND = 0.006f
        const val MAX_MATCH_DISTANCE = 0.22f
        const val MAX_STEP = 0.08f
    }
}
