package com.example.perfectcamerademo.Kalman

import org.example.SingleKalman.GRect
import org.example.SingleKalman.T
import org.example.SingleKalman.grect2orect
import org.example.SingleKalman.makeXMatrix

// 状态初始化
val initial_target_box = GRect(729, 238, 764, 339)  // 目标初始bouding box

val initial_box_state = grect2orect(initial_target_box)


val initial_state = arrayOf(
    intArrayOf(
        initial_box_state.x,
        initial_box_state.y,
        initial_box_state.width,
        initial_box_state.height,
        0,
        0
    )
).T()
// [中心x,中心y,宽w,高h,dx,dy]


val IOU_Threshold = 0.3f  // 匹配时的阈值

// 状态转移矩阵，上一时刻的状态转移到当前时刻
val A = makeXMatrix(1)


//A = np.array([[1, 0, 0, 0, 1, 0],
//[0, 1, 0, 0, 0, 1],
//[0, 0, 1, 0, 0, 0],
//[0, 0, 0, 1, 0, 0],
//[0, 0, 0, 0, 1, 0],
//[0, 0, 0, 0, 0, 1]])

// 状态观测矩阵
val H = makeXMatrix(1)

// 过程噪声协方差矩阵Q，p(w)~N(0,Q)，噪声来自真实世界中的不确定性,
//在跟踪任务当中，过程噪声来自目标移动的不确定性（突然加速、减速、转弯等）
val Q = makeXMatrix(0.1f)

// 观测噪声协方差矩阵R，p(v)~N(0,R)
// 观测噪声来自于检测框丢失、重叠等
val R = makeXMatrix(1)

// 控制输入矩阵B
val B = null
// 状态估计协方差矩阵P初始化
val P = makeXMatrix(1)