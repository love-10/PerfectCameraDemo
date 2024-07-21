package com.example.perfectcamerademo.Kalman

import android.graphics.PointF
import org.example.SingleKalman.log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object AvgUtils {
    // 欧几里得距离

    private val points = mutableListOf<PointF>()
    private val threshold = 5.0 // 设定一个距离阈值

    fun run() {
        // 插入点并检测偏移点
        addPoint(PointF(1.0f, 2.0f))
        addPoint(PointF(2.0f, 3.0f))
        addPoint(PointF(3.0f, 4.0f))
        addPoint(PointF(100.0f, 2.0f)) // 这个点应该被认为是偏移点
        addPoint(PointF(5.0f, 6.0f))

        // 打印剩余的有效点
        log(points)
    }

    private fun addPoint(point: PointF) {
        if (points.isNotEmpty()) {
            val avgPoint = calculateAveragePoint()
            val distance = calculateEuclideanDistance(avgPoint, point)
            if (distance > threshold) {
                log("Outlier Point (${point.x}, ${point.y}) is an outlier and will be removed.")
                return
            }
        }
        points.add(point)
    }

    private fun calculateAveragePoint(): PointF {
        var sumX = 0.0f
        var sumY = 0.0f
        for (point in points) {
            sumX += point.x
            sumY += point.y
        }
        return PointF(sumX / points.size, sumY / points.size)
    }

    private fun calculateEuclideanDistance(
        p1: PointF,
        p2: PointF
    ): Float {
        return sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
    }

    private fun removeOutliers(coordinates: List<PointF>): List<PointF> {
        // 计算均值
        val meanX = coordinates.map { it.x }.average()
        val meanY = coordinates.map { it.y }.average()

        // 计算标准差
        val stdDevX = sqrt(coordinates.map { (it.x - meanX).pow(2) }.average())
        val stdDevY = sqrt(coordinates.map { (it.y - meanY).pow(2) }.average())

        // 设定一个阈值，通常为2或3个标准差范围内的数据为正常数据
        val threshold = 1

        return coordinates.filter {
            val isWithinX = abs(it.x - meanX) <= threshold * stdDevX
            val isWithinY = abs(it.y - meanY) <= threshold * stdDevY
            isWithinX && isWithinY
        }
    }
}