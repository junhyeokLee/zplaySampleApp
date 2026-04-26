package com.example.sample.domain

import com.example.sample.data.model.DetectedPose
import com.example.sample.data.model.PoseFrame
import com.example.sample.data.model.PosePoint
import com.example.sample.data.model.torsoCenter
import kotlin.math.abs
import kotlin.math.min

class LaneAssignmentEngine {
    private var previousLaneCenters: Map<Int, PosePoint> = emptyMap()

    fun assign(frame: PoseFrame): PoseFrame {
        if (frame.poses.isEmpty()) {
            previousLaneCenters = emptyMap()
            return frame
        }

        val bestPoseByLane = mutableMapOf<Int, Pair<DetectedPose, Float>>()
        frame.poses.forEach { pose ->
            val center = pose.torsoCenter() ?: return@forEach
            val laneIndex = detectLane(center)
            val weightedScore = center.confidence + lanePriorityBoost(center.x, laneIndex)
            val current = bestPoseByLane[laneIndex]
            if (current == null || weightedScore > current.second) {
                bestPoseByLane[laneIndex] = pose.copy(laneIndex = laneIndex) to weightedScore
            }
        }

        val assignedPoses = bestPoseByLane.toSortedMap().values.map { it.first }
        previousLaneCenters = assignedPoses.mapNotNull { pose ->
            val lane = pose.laneIndex ?: return@mapNotNull null
            val center = pose.torsoCenter() ?: return@mapNotNull null
            lane to center
        }.toMap()

        return frame.copy(poses = assignedPoses)
    }

    fun reset() {
        previousLaneCenters = emptyMap()
    }

    private fun detectLane(center: PosePoint): Int {
        val previousLane = previousLaneCenters.entries
            .minByOrNull { (_, previous) -> abs(previous.x - center.x) + abs(previous.y - center.y) }
            ?.key

        if (previousLane != null) {
            val laneStart = previousLane.toFloat() / LANE_COUNT
            val laneEnd = (previousLane + 1).toFloat() / LANE_COUNT
            if (center.x in (laneStart - HYSTERESIS_MARGIN)..(laneEnd + HYSTERESIS_MARGIN)) {
                return previousLane
            }
        }

        return min((center.x * LANE_COUNT).toInt(), LAST_LANE_INDEX).coerceAtLeast(0)
    }

    private fun lanePriorityBoost(normalizedX: Float, laneIndex: Int): Float {
        val laneCenter = (laneIndex + 0.5f) / LANE_COUNT
        return 0.2f - abs(normalizedX - laneCenter)
    }

    private companion object {
        const val LANE_COUNT = 4
        const val LAST_LANE_INDEX = LANE_COUNT - 1
        const val HYSTERESIS_MARGIN = 0.05f
    }
}
