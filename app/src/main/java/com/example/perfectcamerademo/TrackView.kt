package com.example.perfectcamerademo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TrackView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    val rects = mutableListOf<RectF>()
    fun update(boxes: MutableList<BoundingBox>) {
        rects.clear()
        boxes.forEach {
            val scale = width / 1080
            val x1 = it.x1 * scale
            val y1 = it.y1 * scale
            val x2 = it.x2 * scale
            val y2 = it.y2 * scale
            rects.add(RectF(x1, y1, x2, y2))
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rects.forEach {
            canvas.drawRect(it, paint)
        }
    }
}