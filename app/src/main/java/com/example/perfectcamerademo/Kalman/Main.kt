package com.example.perfectcamerademo.Kalman

import android.graphics.Point
import org.example.SingleKalman.GRect
import org.example.SingleKalman.array16ToORect
import org.example.SingleKalman.toGRect
import org.example.SingleKalman.toORect

// 状态初始化
val initial_target_box = GRect(729, 238, 764, 339)  // 目标初始bouding box

val initial_box_state = initial_target_box.toORect()


val initial_state = arrayOf(
    intArrayOf(
        initial_box_state.x,
        initial_box_state.y,
        initial_box_state.width,
        initial_box_state.height,
        0,
        0
    )
).T().toFloatArray()
// [中心x,中心y,宽w,高h,dx,dy]


val IOU_Threshold = 0.3f  // 匹配时的阈值

// 状态转移矩阵，上一时刻的状态转移到当前时刻
val A = makeXMatrix(1).apply {
    intArrayOf(0)[4] = 1
    intArrayOf(1)[5] = 1
}.toFloatArray()

// 状态观测矩阵
val H = makeXMatrix(1.0f)

// 过程噪声协方差矩阵Q，p(w)~N(0,Q)，噪声来自真实世界中的不确定性,
//在跟踪任务当中，过程噪声来自目标移动的不确定性（突然加速、减速、转弯等）
val Q = makeXMatrix(0.1f)

// 观测噪声协方差矩阵R，p(v)~N(0,R)
// 观测噪声来自于检测框丢失、重叠等
val R = makeXMatrix(1f)

// 状态估计协方差矩阵P初始化
val P = makeXMatrix(1f)

var X_posterior = initial_state.copyOf()
var P_posterior = P.copyOf()
var Z = initial_state.copyOf()

fun run(boxes: MutableList<GRect>) {

    var max_iou = IOU_Threshold
    var max_iou_matched = false
    var target_box: GRect? = null
    boxes.forEach { box ->
        val iou = calculateIoU(box, X_posterior.array16ToORect().toGRect())
        if (iou > max_iou) {
            target_box = box
            max_iou = iou
            max_iou_matched = true
        }
    }

    if (max_iou_matched) {
        val box = target_box!!.toORect()
        val box_center = Point(target_box!!.centerX(), target_box!!.centerY())

        Z = arrayOf(
            floatArrayOf(box.x.toFloat()),
            floatArrayOf(box.y.toFloat()),
            floatArrayOf(box.width.toFloat()),
            floatArrayOf(box.height.toFloat()),
            X_posterior[0].preOffset(box.x),
            X_posterior[1].preOffset(box.y)
        )
    }

    if (max_iou_matched) {
        // -----进行先验估计-----------------
        val X_prior = A.dot(X_posterior)
        val box_prior = X_prior.array16ToORect().toGRect()
        // -----计算状态估计协方差矩阵P--------
        val P_prior_1 = A.dot(P_posterior)
        val P_prior = P_prior_1.dot(A.T()).add(Q)
        // ------计算卡尔曼增益---------------------
        val k1 = P_prior.dot(H.T())
        val k2 = (H.dot(P_prior)).dot(H.T()).add(R)
        val K = k1.dot(k2.inv())

        // --------------后验估计------------
        val X_posterior_1 = Z.sub(H.dot(X_prior))
        val X_posterior = X_prior.add(K.dot(X_posterior_1))
        val box_posterior = X_posterior.array16ToORect().toGRect()

        val P_posterior_1 = makeXMatrix(1f).sub(K.dot(H))
        P_posterior = P_posterior_1.dot(P_prior)
    } else {
        // 如果IOU匹配失败，此时失去观测值，那么直接使用上一次的最优估计作为先验估计
        // 此时直接迭代，不使用卡尔曼滤波
        X_posterior = A.dot(X_posterior)
        val box_posterior = X_posterior.array16ToORect().toGRect()
        val box_center = Point(box_posterior.centerX(), box_posterior.centerY())
    }
}