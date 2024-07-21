package com.example.perfectcamerademo.Kalman

import org.example.SingleKalman.log
import org.example.SingleKalman.printMat
import org.opencv.core.Core.gemm
import org.opencv.core.Core.multiply
import org.opencv.core.CvType.CV_64F
import org.opencv.core.Mat
import org.opencv.core.Scalar

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

    fun run() {

        for (z in measurements) {
            // 预测步骤
            val xPred = Mat()
            gemm(F, x, 1.0, Mat(), 0.0, xPred)

            val PPred = Mat()
            gemm(F, P, 1.0, Mat(), 0.0, PPred)
            gemm(PPred, F.t(), 1.0, Q, 1.0, PPred)

            // 更新步骤
            val zMat = Mat(2, 1, CV_64F).apply {
                put(0, 0, z[0])
                put(1, 0, z[1])
            }
            val y = Mat()
            gemm(H, xPred, -1.0, zMat, 1.0, y)

            val S = Mat()
            gemm(H, PPred, 1.0, Mat(), 0.0, S)
            gemm(S, H.t(), 1.0, R, 1.0, S)

            val K = Mat()
            S.inv()
            gemm(PPred, H.t(), 1.0, Mat(), 0.0, K)
            gemm(K, S, 1.0, Mat(), 0.0, K)

            gemm(K, y, 1.0, xPred, 1.0, x)
            gemm(K, H, 1.0, Mat.eye(4, 4, CV_64F), -1.0, K)
            gemm(K, PPred, 1.0, Mat(), 0.0, P)

            log("跟新状态:")
            x.printMat()
        }
    }


}