package com.example.perfectcamerademo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
    fun update(boxes: List<Box>) {
        rects.clear()
        props.clear()
        boxes.forEach {
            val scale = width / 1080
            val x1 = it.x * scale
            val y1 = it.y * scale
            val x2 = (it.x + it.width) * scale
            val y2 = (it.y + it.height) * scale
            rects.add(RectF(x1, y1, x2, y2))
            props.add("${it.prop * 100}%")
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