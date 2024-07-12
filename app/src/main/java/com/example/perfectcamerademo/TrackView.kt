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

    val textPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        textSize = 60f
    }
    val rects = mutableListOf<RectF>()
    val props = mutableListOf<String>()
    fun update(boxes: MutableList<BoundingBox>) {
        rects.clear()
        props.clear()
        boxes.forEach {
            val scale = width / 1080
            val x1 = it.x1 * scale
            val y1 = it.y1 * scale
            val x2 = it.x2 * scale
            val y2 = it.y2 * scale
            rects.add(RectF(x1, y1, x2, y2))
            props.add("${it.cnf * 100}%")
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rects.forEachIndexed { index, rectF ->
            canvas.drawRect(rectF, paint)
            canvas.drawText(props[index], rectF.left, rectF.top + 60, textPaint)
        }
    }
}