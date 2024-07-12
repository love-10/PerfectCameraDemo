package com.example.perfectcamerademo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

fun boxesToBoundingBoxes(boxes: MutableList<Box>): MutableList<BoundingBox> {
    val r = mutableListOf<BoundingBox>()
    boxes.forEach {
        r.add(boxToBoundingBox(it))
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