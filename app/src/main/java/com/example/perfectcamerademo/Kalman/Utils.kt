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

fun makeXMatrix(value: Int): Array<IntArray> {
    val matrix = Array(6) {
        when (it) {
            0 -> intArrayOf(value, 0, 0, 0, 0, 0)
            1 -> intArrayOf(0, value, 0, 0, 0, 0)
            2 -> intArrayOf(0, 0, value, 0, 0, 0)
            3 -> intArrayOf(0, 0, 0, value, 0, 0)
            4 -> intArrayOf(0, 0, 0, 0, value, 0)
            5 -> intArrayOf(0, 0, 0, 0, 0, value)
            else -> intArrayOf(0, 0, 0, 0, 0, 0)
        }
    }
    return matrix
}

fun makeXMatrix(value: Float): Array<FloatArray> {
    val matrix = Array(6) {
        when (it) {
            0 -> floatArrayOf(value, 0f, 0f, 0f, 0f, 0f)
            1 -> floatArrayOf(0f, value, 0f, 0f, 0f, 0f)
            2 -> floatArrayOf(0f, 0f, value, 0f, 0f, 0f)
            3 -> floatArrayOf(0f, 0f, 0f, value, 0f, 0f)
            4 -> floatArrayOf(0f, 0f, 0f, 0f, value, 0f)
            5 -> floatArrayOf(0f, 0f, 0f, 0f, 0f, value)
            else -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
        }
    }
    return matrix
}

private fun transpose(array: Array<FloatArray>): Array<FloatArray> {
    val row = array.size
    val column = if (row != 0) array[0].size else 0
    val transpose = Array(column) { FloatArray(row) }
    for (i in 0..<row) {
        for (j in 0..<column) {
            transpose[j][i] = array[i][j]
        }
    }
    return transpose
}

private fun transpose(array: Array<IntArray>): Array<IntArray> {
    val row = array.size
    val column = if (row != 0) array[0].size else 0
    val transpose = Array(column) { IntArray(row) }
    for (i in 0..<row) {
        for (j in 0..<column) {
            transpose[j][i] = array[i][j]
        }
    }
    return transpose
}

fun Array<FloatArray>.T(): Array<FloatArray> {
    return transpose(this)
}

fun Array<IntArray>.T(): Array<IntArray> {
    return transpose(this)
}