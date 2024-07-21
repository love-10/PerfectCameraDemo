package org.example.SingleKalman

import android.graphics.Rect
import android.util.Log
import com.example.perfectcamerademo.Kalman.toIntArray
import com.google.gson.Gson
import org.opencv.core.Mat

typealias GRect = Rect
typealias ORect = org.opencv.core.Rect

// 将左上右下表示的框转换为中心点坐标和宽高表示的框
fun GRect.toORect(): ORect {
    return ORect(left, top, right - left, bottom - top)
}

// 将中心点坐标和宽高表示的框转换为左上右下表示的框
fun ORect.toGRect(): GRect {
    return GRect(x, y, x + width, y + height)
}

fun Array<FloatArray>.array16ToORect(): ORect {
    return toIntArray().array16ToORect()
}

fun Array<IntArray>.array16ToORect(): ORect {
    return ORect(
        get(0)[0],
        get(1)[0],
        get(2)[0],
        get(3)[0]
    )
}

fun log(msg: Any?) {
    Log.d("xxxxx", Gson().toJson(msg))
}

fun Any?.toJson(): String {
    return Gson().toJson(this)
}

fun Mat.printMat() {
    val rows = rows()
    val cols = cols()
    for (i in 0 until rows) {
        val sb = StringBuilder()
        for (j in 0 until cols) {
            sb.append(get(i, j)[0])
            if (j != cols - 1) {
                sb.append(" ")
            }
        }
        log(sb.toString())
    }
}

