package com.example.perfectcamerademo.Kalman

import android.graphics.Point
import android.graphics.PointF
import org.example.SingleKalman.log
import org.example.SingleKalman.printMat
import org.opencv.core.Core.add
import org.opencv.core.Core.gemm
import org.opencv.core.Core.multiply
import org.opencv.core.Core.subtract
import org.opencv.core.CvType.CV_64F
import org.opencv.core.Mat
import org.opencv.core.Scalar
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object KalmanPro {
    // 初始状态 [x, y, vx, vy]
    private val x = Mat(4, 1, CV_64F).apply {
        put(0, 0, 0.0)
        put(1, 0, 0.0)
        put(2, 0, 0.0)
        put(3, 0, 0.0)
    }


    // 初始状态协方差矩阵
    private var P = Mat.eye(4, 4, CV_64F)

    // 状态转移矩阵（假设单位时间步长）
    private val F = Mat(4, 4, CV_64F).apply {
        put(0, 0, 1.0); put(0, 1, 0.0); put(0, 2, 1.0); put(0, 3, 0.0)
        put(1, 0, 0.0); put(1, 1, 1.0); put(1, 2, 0.0); put(1, 3, 1.0)
        put(2, 0, 0.0); put(2, 1, 0.0); put(2, 2, 1.0); put(2, 3, 0.0)
        put(3, 0, 0.0); put(3, 1, 0.0); put(3, 2, 0.0); put(3, 3, 1.0)
    }

    // 观测矩阵
    private val H = Mat(2, 4, CV_64F).apply {
        put(0, 0, 1.0); put(0, 1, 0.0); put(0, 2, 0.0); put(0, 3, 0.0)
        put(1, 0, 0.0); put(1, 1, 1.0); put(1, 2, 0.0); put(1, 3, 0.0)
    }

    // 过程噪声协方差矩阵
    private val Q = Mat.eye(4, 4, CV_64F).apply {
        multiply(this, Scalar(0.01), this)
    }

    // 观测噪声协方差矩阵
    private val R = Mat.eye(2, 2, CV_64F).apply { multiply(this, Scalar(0.1), this) }

    // 观测值 (x, y)
    private val measurements = listOf(
        listOf(1.0, 2.0),
        listOf(2.0, 3.0),
        listOf(3.0, 4.0),
        listOf(4.0, 5.0),
        listOf(5.0, 6.0)
    )

    private val ms = listOf(
        PointF(1.0f, 2.0f),
        PointF(2.0f, 3.0f),
        PointF(3.0f, 4.0f),
        PointF(4.0f, 5.0f),
        PointF(5.0f, 6.0f),
        PointF(7.0f, 6.0f),
    )

    fun run() {
//        log(removeOutliers(ms))
        // 插入点并检测偏移点
        addPoint(1.0, 2.0)
        addPoint(2.0, 3.0)
        addPoint(3.0, 4.0)
        addPoint(100.0, 200.0) // 这个点应该被认为是偏移点
        addPoint(4.0, 5.0)

        // 打印剩余的有效点
        log(points)
        for (z in measurements) {
            // 预测步骤
            gemm(F, x, 1.0, Mat(), 0.0, x)

            val PPred = Mat()
            gemm(F, P, 1.0, Mat(), 0.0, PPred)
            gemm(PPred, F.t(), 1.0, Q, 0.0, P)

            // 更新步骤
            val zMat = Mat(2, 1, CV_64F).apply {
                put(0, 0, z[0])
                put(1, 0, z[1])
            }
            val y = Mat()
            gemm(H, x, 1.0, Mat(), 0.0, y)
            subtract(zMat, y, y)

            val S = Mat()
            gemm(P, H.t(), 1.0, Mat(), 0.0, S)
            gemm(H, S, 1.0, Mat(), 0.0, S)
            add(S, R, S)

            val K = Mat()

            gemm(P, H.t(), 1.0, Mat(), 0.0, K)
            gemm(K, S.inv(), 1.0, Mat(), 0.0, K)

            val xTemp = Mat()
            gemm(K, y, 1.0, Mat(), 0.0, xTemp)
            add(xTemp, x, x)

            val pTemp = Mat()
            gemm(K, H, 1.0, Mat(), 0.0, pTemp)
            gemm(pTemp, P, 1.0, Mat(), 0.0, pTemp)

            subtract(P, pTemp, P)
//            log("跟新状态:")
//            x.printMat()
        }
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

    private val points = mutableListOf<Pair<Double, Double>>()
    private val threshold = 5.0 // 设定一个距离阈值

    private fun addPoint(x: Double, y: Double) {
        val newPoint = Pair(x, y)
        if (points.isNotEmpty()) {
            val avgPoint = calculateAveragePoint()
            val distance = calculateEuclideanDistance(avgPoint, newPoint)
            if (distance > threshold) {
                log("Outlier Point ($x, $y) is an outlier and will be removed.")
                return
            }
        }
        points.add(newPoint)
    }

    private fun calculateAveragePoint(): Pair<Double, Double> {
        var sumX = 0.0
        var sumY = 0.0
        for (point in points) {
            sumX += point.first
            sumY += point.second
        }
        return Pair(sumX / points.size, sumY / points.size)
    }

    private fun calculateEuclideanDistance(
        p1: Pair<Double, Double>,
        p2: Pair<Double, Double>
    ): Double {
        return sqrt((p1.first - p2.first) * (p1.first - p2.first) + (p1.second - p2.second) * (p1.second - p2.second))
    }

}