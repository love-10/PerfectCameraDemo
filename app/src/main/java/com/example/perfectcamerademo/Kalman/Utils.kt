package org.example.SingleKalman

import android.graphics.Rect

typealias GRect = Rect
typealias ORect = org.opencv.core.Rect

// 将左上右下表示的框转换为中心点坐标和宽高表示的框
fun grect2orect(rect: GRect): ORect {
    return ORect()
}

// 将中心点坐标和宽高表示的框转换为左上右下表示的框
fun orect2grect(rect: ORect): GRect {
    return GRect()
}

