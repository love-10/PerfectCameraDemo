package com.example.perfectcamerademo.Kalman

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.RealMatrix
import org.example.SingleKalman.GRect
import kotlin.math.roundToInt

fun Array<FloatArray>.toIntArray(): Array<IntArray> {
    return this.map { row ->
        row.map { it.roundToInt() }.toIntArray()
    }.toTypedArray()
}

fun Array<FloatArray>.toDoubleArray(): Array<DoubleArray> {
    return this.map { row ->
        row.map { it.toDouble() }.toDoubleArray()
    }.toTypedArray()
}

fun Array<DoubleArray>.toFloatArray(): Array<FloatArray> {
    return this.map { row ->
        row.map { it.toFloat() }.toFloatArray()
    }.toTypedArray()
}

fun Array<IntArray>.toFloatArray(): Array<FloatArray> {
    return this.map { row ->
        row.map { it.toFloat() }.toFloatArray()
    }.toTypedArray()
}

fun IntArray.toFloatArray(): FloatArray {
    return this.map {
        it.toFloat()
    }.toFloatArray()
}

fun FloatArray.preOffset(value: Int): FloatArray {
    return this.map {
        value - it
    }.toFloatArray()
}

fun FloatArray.toIntArray(): IntArray {
    return this.map {
        it.roundToInt()
    }.toIntArray()
}

fun Array<IntArray>.dot(bArray: Array<IntArray>): Array<IntArray> {
    return dotProduct(toFloatArray(), bArray.toFloatArray()).toIntArray()
}

fun Array<FloatArray>.dot(bArray: Array<FloatArray>): Array<FloatArray> {
    return dotProduct(this, bArray)
}


private fun dotProduct(a: Array<FloatArray>, b: Array<FloatArray>): Array<FloatArray> {
    val rowsA = a.size
    val colsA = a[0].size
    val rowsB = b.size
    val colsB = b[0].size

    // 检查矩阵维度是否匹配
    if (colsA != rowsB) {
        throw IllegalArgumentException("Matrix dimensions do not match for dot product.")
    }

    // 初始化结果矩阵
    val result = Array(rowsA) { FloatArray(colsB) }

    // 计算点积
    for (i in 0 until rowsA) {
        for (j in 0 until colsB) {
            for (k in 0 until colsA) {
                result[i][j] += a[i][k] * b[k][j]
            }
        }
    }
    return result
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

fun Array<FloatArray>.T(): Array<FloatArray> {
    return transpose(this)
}

fun Array<IntArray>.T(): Array<IntArray> {
    return transpose(toFloatArray()).toIntArray()
}

fun makeXMatrix(value: Int): Array<IntArray> {
    return makeXMatrix(value.toFloat()).toIntArray()
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

fun calculateIoU(rect1: GRect, rect2: GRect): Float {
    val xLeft = maxOf(rect1.left, rect2.left)
    val yTop = maxOf(rect1.top, rect2.top)
    val xRight = minOf(rect1.right, rect2.right)
    val yBottom = minOf(rect1.bottom, rect2.bottom)

    val intersectionArea = if (xRight < xLeft || yBottom < yTop) {
        0.0
    } else {
        (xRight - xLeft) * (yBottom - yTop).toDouble()
    }

    val rect1Area = (rect1.right - rect1.left) * (rect1.bottom - rect1.top).toDouble()
    val rect2Area = (rect2.right - rect2.left) * (rect2.bottom - rect2.top).toDouble()

    val unionArea = rect1Area + rect2Area - intersectionArea

    return (intersectionArea / unionArea).toFloat()
}

fun Array<FloatArray>.add(floatArray: Array<FloatArray>): Array<FloatArray> {
    val rows = this.size
    val cols = this[0].size

    // 确保两个数组的维度一致
    if (rows != floatArray.size || cols != floatArray[0].size) {
        throw IllegalArgumentException("The dimensions of the arrays do not match.")
    }

    val result = Array(rows) { FloatArray(cols) }

    for (i in 0 until rows) {
        for (j in 0 until cols) {
            result[i][j] = this[i][j] + floatArray[i][j]
        }
    }

    return result
}

fun Array<FloatArray>.sub(floatArray: Array<FloatArray>): Array<FloatArray> {
    val rows = this.size
    val cols = this[0].size

    // 确保两个数组的维度一致
    if (rows != floatArray.size || cols != floatArray[0].size) {
        throw IllegalArgumentException("The dimensions of the arrays do not match.")
    }

    val result = Array(rows) { FloatArray(cols) }

    for (i in 0 until rows) {
        for (j in 0 until cols) {
            result[i][j] = this[i][j] - floatArray[i][j]
        }
    }

    return result
}

fun Array<FloatArray>.inv(): Array<FloatArray> {
    val matrix = toDoubleArray()
    val realMatrix: RealMatrix = Array2DRowRealMatrix(matrix)
    val luDecomposition = LUDecomposition(realMatrix)
    val inverseMatrix = luDecomposition.solver.inverse

    // 将 RealMatrix 转换为 Array<DoubleArray>
    val result = Array(matrix.size) { DoubleArray(matrix[0].size) }
    for (i in result.indices) {
        for (j in result[i].indices) {
            result[i][j] = inverseMatrix.getEntry(i, j)
        }
    }
    return result.toFloatArray()
}