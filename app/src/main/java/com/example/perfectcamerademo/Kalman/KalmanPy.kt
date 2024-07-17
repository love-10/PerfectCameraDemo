package com.example.perfectcamerademo.Kalman
//import org.example.SingleKalman.GRect
//import org.opencv.core.*
//import org.opencv.core.Core.multiply
//import java.io.File
//
//fun xyxyToXywh(box: Rect): DoubleArray {
//    val x = (box.x + box.width / 2).toDouble()
//    val y = (box.y + box.height / 2).toDouble()
//    val w = box.width.toDouble()
//    val h = box.height.toDouble()
//    return doubleArrayOf(x, y, w, h)
//}
//
//fun xywhToXyxy(box: DoubleArray): Rect {
//    val x = (box[0] - box[2] / 2).toInt()
//    val y = (box[1] - box[3] / 2).toInt()
//    val w = box[2].toInt()
//    val h = box[3].toInt()
//    return Rect(x, y, w, h)
//}
//
//fun calculateIoU(box1: Rect, box2: Rect): Double {
//    val intersection = Rect()
//    val union = box1.area() + box2.area() - intersection.area()
//    return intersection.area() / union
//}
//
//// 状态初始化
//val initialTargetBox = Rect(729, 238, 764 - 729, 339 - 238)
//val initialBoxState = xyxyToXywh(initialTargetBox)
//val initialState = Mat(6, 1, CvType.CV_64F).apply {
//    put(
//        0,
//        0,
//        initialBoxState[0],
//        initialBoxState[1],
//        initialBoxState[2],
//        initialBoxState[3],
//        0.0,
//        0.0
//    )
//}
//
//val IOU_THRESHOLD = 0.3
//
//// 状态转移矩阵
//val A = Mat(6, 6, CvType.CV_64F).apply {
//    put(0, 0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0)
//    put(1, 0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
//    put(2, 0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0)
//    put(3, 0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0)
//    put(4, 0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
//    put(5, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
//}
//
//// 状态观测矩阵
//val H = Mat.eye(6, 6, CvType.CV_64F)
//
//// 过程噪声协方差矩阵 Q
//val Q = Mat.eye(6, 6, CvType.CV_64F).apply {
//    multiply(this, Scalar(0.1), this)
//}
//
//// 观测噪声协方差矩阵 R
//val R = Mat.eye(6, 6, CvType.CV_64F).apply {
//    multiply(this, Scalar(1.0), this)
//}
//
//// 状态估计协方差矩阵 P 初始化
//val P = Mat.eye(6, 6, CvType.CV_64F)
//
//fun run(boxes: MutableList<GRect>) {
//
//    var frameCounter = 1
//    var XPosterior = initialState.clone()
//    var PPosterior = P.clone()
//    var Z = initialState.clone()
//
//    boxes.forEach {
//        if (File(labelFilePath).exists()) {
//            val lines = File(labelFilePath).readLines()
//            val boxes = lines.map { line ->
//                val parts = line.split(" ")
//                val x = parts[1].toDouble()
//                val y = parts[2].toDouble()
//                val w = parts[3].toDouble()
//                val h = parts[4].toDouble()
//                xywhToXyxy(doubleArrayOf(x, y, w, h))
//            }
//
//            val ious = boxes.map { box -> calculateIoU(lastBoxPosterior, box) }
//            val maxIoU = ious.maxOrNull() ?: 0.0
//            val maxIoUIndex = ious.indexOf(maxIoU)
//
//            if (maxIoU > IOU_THRESHOLD) {
//                val maxIoUBox = boxes[maxIoUIndex]
//                plotOneBox(maxIoUBox, frame, Scalar(0.0, 255.0, 0.0), target = false)
//
//                val xywh = xyxyToXywh(maxIoUBox)
//                val dx = xywh[0] - XPosterior[0, 0]
//                val dy = xywh[1] - XPosterior[1, 0]
//
//                Z.put(0, 0, xywh[0], xywh[1], xywh[2], xywh[3], dx, dy)
//
//                val XPrior = A * XPosterior
//                val boxPrior = xywhToXyxy(
//                    doubleArrayOf(
//                        XPrior[0, 0],
//                        XPrior[1, 0],
//                        XPrior[2, 0],
//                        XPrior[3, 0]
//                    )
//                )
//
//                val PPrior = A * PPosterior * A.t() + Q
//
//                val k1 = PPrior * H.t()
//                val k2 = H * PPrior * H.t() + R
//                val K = k1 * Core.invert(k2, Mat(), Core.DECOMP_LU)
//
//                val XPosterior1 = Z - H * XPrior
//                XPosterior = XPrior + K * XPosterior1
//
//                val PPosterior1 = Mat.eye(6, 6, CvType.CV_64F) - K * H
//                PPosterior = PPosterior1 * PPrior
//            } else {
//                XPosterior = A * XPosterior
//            }
//
//            val boxPosterior = xywhToXyxy(
//                doubleArrayOf(
//                    XPosterior[0, 0],
//                    XPosterior[1, 0],
//                    XPosterior[2, 0],
//                    XPosterior[3, 0]
//                )
//            )
//            plotOneBox(boxPosterior, frame, Scalar(255.0, 255.0, 255.0), target = false)
//        }
//    }
//}