package com.example.perfectcamerademo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
    val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
    val selectedBoxes = mutableListOf<BoundingBox>()
    while (sortedBoxes.isNotEmpty()) {
        val first = sortedBoxes.first()
        selectedBoxes.add(first)
        sortedBoxes.remove(first)
        val iterator = sortedBoxes.iterator()
        while (iterator.hasNext()) {
            val nextBox = iterator.next()
            val iou = calculateIoU(first, nextBox)
            if (iou >= 0.5f) {
                iterator.remove()
            }
        }
    }
    return selectedBoxes
}

private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
    val x1 = maxOf(box1.x1, box2.x1)
    val y1 = maxOf(box1.y1, box2.y1)
    val x2 = minOf(box1.x2, box2.x2)
    val y2 = minOf(box1.y2, box2.y2)
    val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
    val box1Area = box1.w * box1.h
    val box2Area = box2.w * box2.h
    return intersectionArea / (box1Area + box2Area - intersectionArea)
}

fun boxesToBoundingBoxes(boxes: MutableList<Box>): MutableList<BoundingBox> {
    val r = mutableListOf<BoundingBox>()
    boxes.forEach {
        if (it.label == 0 && it.prop > 0.9f) {
            r.add(boxToBoundingBox(it))
        }

    }
    return r
}

fun boxToBoundingBox(box: Box): BoundingBox {
    return BoundingBox(
        box.x,
        box.y,
        box.x + box.width,
        box.y + box.height,
        box.x + box.width / 2f,
        box.y + box.height / 2f,
        box.width,
        box.height,
        box.prop,
        box.label,
        ""
    )
}

fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<BoundingBox>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    for (box in boxes) {
        val rect = RectF(
            box.x1,
            box.y1,
            box.x2,
            box.y2
        )
        canvas.drawRect(rect, paint)
        canvas.drawText(box.clsName, rect.left, rect.bottom, textPaint)
    }
    return mutableBitmap
}